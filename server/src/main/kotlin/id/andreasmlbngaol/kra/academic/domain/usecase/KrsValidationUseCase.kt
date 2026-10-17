package id.andreasmlbngaol.kra.academic.domain.usecase

import id.andreasmlbngaol.kra.academic.domain.model.DuplicateCourse
import id.andreasmlbngaol.kra.academic.domain.model.ElectiveOption
import id.andreasmlbngaol.kra.academic.domain.model.ElectiveRequirement
import id.andreasmlbngaol.kra.academic.domain.model.KrsValidationResult
import id.andreasmlbngaol.kra.academic.domain.model.PrerequisiteViolation
import id.andreasmlbngaol.kra.academic.domain.model.ScheduleConflict
import id.andreasmlbngaol.kra.academic.domain.model.rules.AcademicRules
import id.andreasmlbngaol.kra.academic.domain.repository.CourseRepository
import id.andreasmlbngaol.kra.core.enums.CourseType
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Validates a prospective KRS draft against all enrollment rules before it is saved.
 *
 * A single pass over the selected class offerings produces a [KrsValidationResult]
 * covering: total vs. allowed SKS (including MKWU-reserved SKS), schedule conflicts,
 * prerequisite violations, duplicate courses, and the mandatory-elective requirement.
 */
@Service
class KrsValidationUseCase(
    private val courseRepository: CourseRepository,
    private val khsUseCase: KhsUseCase,
    private val courseUseCase: CourseUseCase
) {
    /**
     * Runs every enrollment check for a candidate set of class offerings.
     *
     * The SKS budget is derived from the previous semester's IP (via [KhsUseCase.getMaxSks])
     * minus the SKS reserved for MKWU this semester.
     */
    fun validateDraft(nim: String, semester: Int, classOfferingIds: List<UUID>): KrsValidationResult {
        val classes = courseRepository.getClassOfferingsByIds(classOfferingIds)
        val courseCodes = classes.map { it.courseCode }.distinct()
        val courses = courseRepository.getCoursesByCodes(courseCodes)
        val courseNameByCode = courses.associate { it.code to it.name }

        val totalSks = courses.sumOf { it.sks }
        // Same resolver as the solver: one source of truth for the cap and the MKWU slot.
        val context = courseUseCase.resolveSemesterContext(nim, semester)
        val maxSks = khsUseCase.getMaxSks(nim, context.lastGraded)
        val mkwuReserved = context.mkwuReservedSks
        val remaining = maxSks - mkwuReserved - totalSks

        // A course code may appear at most once: reject both the same class UUID twice
        // and the same course taken via a different class. Map each input UUID back to
        // its offering (keeping input duplicates), group by course code, and keep the
        // codes that appear more than once.
        val classById = classes.associateBy { it.id }
        val duplicateCourses = classOfferingIds
            .mapNotNull { classById[it] }
            .groupBy { it.courseCode }
            .filter { it.value.size > 1 }
            .map { (code, offerings) ->
                DuplicateCourse(
                    courseCode = code,
                    courseName = courseNameByCode[code] ?: "(nama tidak ditemukan)",
                    classCodes = offerings.map { it.classCode }
                )
            }

        val passed = khsUseCase.getPassedCourseCodes(nim).toSet()
        val prerequisiteViolations = courses.mapNotNull { course ->
            val missing = course.prerequisites.filter { it !in passed }
            if (missing.isEmpty()) null else PrerequisiteViolation(course.code, missing)
        }

        val conflicts = mutableListOf<ScheduleConflict>()
        for (i in classes.indices) {
            for (j in i + 1 until classes.size) {
                val a = classes[i]
                val b = classes[j]
                val aStart = AcademicRules.timeToMinutes(a.startTime)
                val aEnd = AcademicRules.timeToMinutes(a.endTime)
                val bStart = AcademicRules.timeToMinutes(b.startTime)
                val bEnd = AcademicRules.timeToMinutes(b.endTime)
                val overlap = a.day != null && a.day == b.day &&
                    aStart != null && aEnd != null && bStart != null && bEnd != null &&
                    aStart < bEnd && bStart < aEnd
                if (overlap) {
                    conflicts.add(
                        ScheduleConflict(
                            classA = "${a.courseCode} - ${courseNameByCode[a.courseCode] ?: "(nama tidak ditemukan)"} (Kelas ${a.classCode})",
                            classB = "${b.courseCode} - ${courseNameByCode[b.courseCode] ?: "(nama tidak ditemukan)"} (Kelas ${b.classCode})",
                            day = a.day
                        )
                    )
                }
            }
        }

        // Mandatory "choose 1 of N" electives for the current semester: offered
        // REGULAR_ELECTIVE courses whose normalSemester equals the current semester.
        val semesterElectives = context.electiveSemester
            ?.let { electiveSemester ->
                courseRepository.getOfferedCourses().filter {
                    it.type == CourseType.REGULAR_ELECTIVE && it.normalSemester == electiveSemester
                }
            }
            .orEmpty()
        val electiveCodes = semesterElectives.map { it.code }.toSet()
        val electiveRequirement = ElectiveRequirement(
            requiredChoose = if (semesterElectives.isEmpty()) 0 else 1,
            selectedCount = courses.count { it.code in electiveCodes },
            options = semesterElectives.map { ElectiveOption(it.code, it.name, it.sks) }
        )

        return KrsValidationResult(
            totalSks = totalSks,
            maxSks = maxSks,
            mkwuReservedSks = mkwuReserved,
            remainingSks = remaining,
            scheduleConflicts = conflicts,
            prerequisiteViolations = prerequisiteViolations,
            duplicateCourses = duplicateCourses,
            electiveRequirement = electiveRequirement
        )
    }

    /**
     * SKS reserved for MKWU (compulsory university courses) in a given semester.
     * Delegates to [AcademicRules] so the validator and the CSP solver share one source.
     */
    fun getMkwuReservedSks(semester: Int): Int = AcademicRules.mkwuReservedSks(semester)
}
