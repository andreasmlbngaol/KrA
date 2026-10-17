package id.andreasmlbngaol.kra.academic.domain.usecase

import id.andreasmlbngaol.kra.academic.domain.model.ClassOffering
import id.andreasmlbngaol.kra.academic.domain.model.Course
import id.andreasmlbngaol.kra.academic.domain.model.CurriculumPackage
import id.andreasmlbngaol.kra.academic.domain.model.Khs
import id.andreasmlbngaol.kra.academic.domain.repository.CourseRepository
import id.andreasmlbngaol.kra.academic.domain.repository.CurriculumRepository
import id.andreasmlbngaol.kra.academic.domain.model.rules.AcademicRules
import id.andreasmlbngaol.kra.academic.domain.repository.KhsRepository
import id.andreasmlbngaol.kra.core.enums.CourseType
import id.andreasmlbngaol.kra.core.enums.Grade
import org.springframework.stereotype.Service
import java.util.ArrayDeque

/**
 * Read-side use cases for the course catalog: offered courses, class offerings,
 * curriculum packages, the active academic term, and SKS totals.
 *
 * Thin orchestration over the repositories; holds no state of its own.
 */
@Service
class CourseUseCase(
    private val courseRepository: CourseRepository,
    private val curriculumRepository: CurriculumRepository,
    private val khsRepository: KhsRepository
) {
    /** Lists every course that has a class offering in the active academic term. */
    fun getOfferedCourses(): List<Course> = courseRepository.getOfferedCourses()

    /** Looks up a single course by its course code. */
    fun getCourseByCode(code: String): Course? = courseRepository.getCourseByCode(code)

    /** Lists the class offerings (KOM) for a given course. */
    fun getClassOfferings(courseCode: String): List<ClassOffering> =
        courseRepository.getClassOfferings(courseCode)

    /** Lists the curriculum package (mandatory course set) for a semester. */
    fun getCurriculumPackage(semester: Int): List<CurriculumPackage> =
        curriculumRepository.getCurriculumPackage(semester)

    /** Sums the total SKS for a set of course codes. */
    fun getTotalSksByCodes(courseCodes: List<String>): Int =
        courseRepository.getTotalSksByCodes(courseCodes)

    /**
     * Every mandatory course the student still has to take, up to and including [semester].
     *
     * Cumulative on purpose: a student in semester 8 who never passed a semester 5 course
     * still owes it. Curriculum-package membership is deliberately ignored, because the
     * package for a given semester only lists that semester's courses (and some semesters
     * have no package at all), which used to hide every backlog.
     */
    fun getRemainingMandatoryCourses(nim: String, semester: Int): List<Course> {
        if (semester < 1) return emptyList()
        val passedCodes = khsRepository.getPassedCourseCodes(nim).toSet()
        return courseRepository.getAllCourses()
            .filter { course ->
                course.type == CourseType.REGULAR_MANDATORY &&
                    course.normalSemester in 1..semester &&
                    course.code !in passedCodes
            }
            .sortedWith(compareBy({ it.normalSemester }, { it.code }))
    }

    /**
     * The mandatory elective group for the current [semester] (the "choose 1 of N"
     * group): offered REGULAR_ELECTIVE courses whose normalSemester equals [semester].
     */
    fun getSemesterElectives(semester: Int): List<Course> =
        courseRepository.getOfferedCourses()
            .filter { it.type == CourseType.REGULAR_ELECTIVE && it.normalSemester == semester }

    /** Fetches courses for an arbitrary set of course codes. */
    fun getCoursesByCodes(codes: List<String>): List<Course> =
        courseRepository.getCoursesByCodes(codes)

    /**
     * Upper-semester REGULAR_MANDATORY courses the student may take early. Availability
     * comes from the active-term offerings (not parity/curriculum package); the course
     * must still have all prerequisites passed and no Min SKS requirement. Electives and
     * capstone courses are excluded by design.
     */
    fun getEligibleUpperCourses(nim: String, semester: Int): List<Course> {
        val passed = khsRepository.getPassedCourseCodes(nim).toSet()
        return courseRepository.getOfferedCourses().filter { course ->
            course.type == CourseType.REGULAR_MANDATORY &&
                course.minSks == 0 &&
                course.normalSemester > semester &&
                course.prerequisites.all { it in passed }
        }
    }

    /**
     * The elective group the student still owes: the highest semester up to [semester]
     * that actually offers electives and from which the student has not passed any course.
     * Returns `null` when no elective is outstanding.
     */
    fun getPendingElectiveSemester(nim: String, semester: Int): Int? {
        val passed = khsRepository.getPassedCourseCodes(nim).toSet()
        val offered = courseRepository.getOfferedCourses()
            .filter { it.type == CourseType.REGULAR_ELECTIVE }
        return (semester downTo 1).firstOrNull { candidate ->
            val group = offered.filter { it.normalSemester == candidate }
            group.isNotEmpty() && group.none { it.code in passed }
        }
    }

    /**
     * Semester-scoped facts the solver and the validator must agree on.
     *
     * @property lastGraded Highest semester that actually has a KHS row. The SKS cap comes
     *   from this semester's IP, NOT from `administrative - 1`: a student who took a leave
     *   has no grades in the skipped semester, and using it would fall back to the maximum
     *   quota instead of the real performance.
     * @property lowestIncomplete Lowest semester that still has an unpassed mandatory course.
     * @property mkwuReservedSks Largest MKWU obligation between [lowestIncomplete] and
     *   [administrative]. Taking the maximum avoids reserving 0 SKS when a student repeats an
     *   earlier semester (for example, semester 4) while administratively sitting in semester 6.
     * @property electiveSemester Semester whose "choose 1 of N" group is still outstanding.
     * @property dSks Total SKS whose best grade is D.
     * @property dRepeatMinSks SKS of D that must be repeated so at most 8 SKS of D remain.
     */
    data class SemesterContext(
        val administrative: Int,
        val lastGraded: Int,
        val lowestIncomplete: Int?,
        val mkwuReservedSks: Int,
        val electiveSemester: Int?,
        val dSks: Int = 0,
        val dRepeatMinSks: Int = 0
    )

    /** Resolves the semester-scoped facts for [nim] once, so no two callers can disagree. */
    fun resolveSemesterContext(nim: String, administrative: Int): SemesterContext {
        val khsRows = khsRepository.getKhsByNim(nim)
        val lastGraded = khsRows.maxOfOrNull { it.semesterTaken } ?: 0
        val remaining = getRemainingMandatoryCourses(nim, administrative)
        val lowestIncomplete = remaining.minOfOrNull { it.normalSemester }
        val from = lowestIncomplete ?: administrative
        val mkwuReserved = (from..administrative).maxOfOrNull { AcademicRules.mkwuReservedSks(it) } ?: 0
        val dSks = dSksOf(khsRows)
        return SemesterContext(
            administrative = administrative,
            lastGraded = lastGraded,
            lowestIncomplete = lowestIncomplete,
            mkwuReservedSks = mkwuReserved,
            electiveSemester = getPendingElectiveSemester(nim, administrative),
            dSks = dSks,
            dRepeatMinSks = AcademicRules.dRepeatMinSks(dSks)
        )
    }

    /** Total SKS whose best grade is D. */
    fun getDSks(nim: String): Int = dSksOf(khsRepository.getKhsByNim(nim))

    /**
     * Every regular course whose best grade is D, in curriculum order. MKWU/MKWK are
     * excluded because they are filled manually in the portal, not in the KRS draft.
     */
    fun getDGradedCourses(nim: String): List<Course> {
        val codes = dGradedCodes(khsRepository.getKhsByNim(nim))
        if (codes.isEmpty()) return emptyList()
        return courseRepository.getCoursesByCodes(codes.toList())
            .filter { it.type == CourseType.REGULAR_MANDATORY || it.type == CourseType.REGULAR_ELECTIVE }
            .sortedWith(compareBy({ it.normalSemester }, { it.code }))
    }

    private fun dSksOf(khsRows: List<Khs>): Int {
        val codes = dGradedCodes(khsRows)
        if (codes.isEmpty()) return 0
        return courseRepository.getCoursesByCodes(codes.toList()).sumOf { it.sks }
    }

    private fun dGradedCodes(khsRows: List<Khs>): Set<String> =
        khsRows
            .groupBy { it.courseCode }
            .mapValues { (_, entries) -> entries.maxBy { KhsUseCase.gradePoint(it.grade) }.grade }
            .filterValues { it == Grade.D }
            .keys

    /**
     * `importance(c) = 1 + number of still-unpassed mandatory courses that transitively
     * require c`.
     *
     * This is the solver's priority signal: a course that blocks four later courses is
     * worth more than one that blocks none, so a failed prerequisite is repeated before a
     * course that gates nothing (for example Etika Profesi, whose only dependent is an
     * elective). Only mandatory dependents count, otherwise every course that gates an
     * elective would look urgent.
     */
    fun getImportanceScores(nim: String): Map<String, Int> =
        getImportance(nim).mapValues { it.value.score }

    /**
     * Per-course importance plus the concrete list of courses it unlocks, so the agent can
     * explain *why* a repeat matters ("this opens 4 other mandatory courses") instead of
     * only quoting a number.
     */
    fun getImportance(nim: String): Map<String, ImportanceInfo> {
        val catalog = courseRepository.getAllCourses()
        val byCode = catalog.associateBy { it.code }
        val passed = khsRepository.getPassedCourseCodes(nim).toSet()
        val bestGrade = khsRepository.getKhsByNim(nim)
            .groupBy { it.courseCode }
            .mapValues { (_, entries) -> entries.maxBy { KhsUseCase.gradePoint(it.grade) }.grade }
        // No semester cap: a semester-5 course that unblocks a semester-7 course is still
        // important, even though the semester-7 course cannot be taken yet.
        val unpassedMandatory = catalog
            .filter { it.type == CourseType.REGULAR_MANDATORY && it.code !in passed }
            .map { it.code }
            .toSet()
        // D already counts as passed, but the student may still repeat it. Include every
        // repeat candidate (C+ and below) so the agent can explain how many later courses
        // depend on it instead of showing importance 1.
        val repeatCandidates = catalog
            .filter { it.type == CourseType.REGULAR_MANDATORY }
            .filter { course -> (bestGrade[course.code]?.ordinal ?: -1) >= Grade.C_PLUS.ordinal }
            .map { it.code }
            .toSet()

        val dependents = HashMap<String, MutableList<String>>()
        catalog.forEach { course ->
            course.prerequisites.forEach { prerequisite ->
                dependents.getOrPut(prerequisite) { mutableListOf() }.add(course.code)
            }
        }

        return (unpassedMandatory + repeatCandidates).associateWith { code ->
            val seen = HashSet<String>()
            val stack = ArrayDeque<String>()
            stack.addLast(code)
            while (stack.isNotEmpty()) {
                val current = stack.removeLast()
                dependents[current].orEmpty().forEach { dependent ->
                    if (seen.add(dependent)) stack.addLast(dependent)
                }
            }
            val unlocked = seen
                .filter { it in unpassedMandatory }
                .mapNotNull { byCode[it] }
                .sortedWith(compareBy({ it.normalSemester }, { it.code }))
            ImportanceInfo(score = 1 + unlocked.size, unlockedCourses = unlocked)
        }
    }

    /** Importance score plus the still-unpassed mandatory courses it unlocks. */
    data class ImportanceInfo(
        val score: Int,
        val unlockedCourses: List<Course>
    )
}
