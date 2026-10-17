package id.andreasmlbngaol.kra.academic.domain.usecase

import id.andreasmlbngaol.kra.academic.domain.model.Khs
import id.andreasmlbngaol.kra.academic.domain.model.RepeatableCourse
import id.andreasmlbngaol.kra.academic.domain.repository.CourseRepository
import id.andreasmlbngaol.kra.academic.domain.repository.KhsRepository
import id.andreasmlbngaol.kra.academic.presentation.dto.KhsCourseDto
import id.andreasmlbngaol.kra.academic.presentation.dto.KhsEntryDto
import id.andreasmlbngaol.kra.academic.presentation.dto.KhsSemesterDto
import id.andreasmlbngaol.kra.academic.presentation.dto.KhsSummaryResponse
import id.andreasmlbngaol.kra.core.enums.CourseType
import id.andreasmlbngaol.kra.core.enums.Grade
import id.andreasmlbngaol.kra.core.enums.jenisLabel
import org.springframework.stereotype.Service
import kotlin.math.round

/** Outcome of manually adding a KHS entry. */
enum class AddKhsOutcome { SUCCESS, COURSE_NOT_FOUND, DUPLICATE }

/**
 * Use case for every KHS (Kartu Hasil Studi) operation: fetching grade history,
 * computing IP/IPK, SKS quota, repeatable courses, and manually adding/removing entries.
 *
 * All IP/IPK calculations consistently use the BEST grade per course and ignore the
 * `T` (Tunda/incomplete) grade.
 */
@Service
class KhsUseCase(
    private val courseRepository: CourseRepository,
    private val khsRepository: KhsRepository
) {
    /** Fetches every KHS entry belonging to a student. */
    fun getKhsByNim(nim: String): List<Khs> = khsRepository.getKhsByNim(nim)

    /** Sums the total number of SKS the student has already PASSED. */
    fun getTotalPassedSks(nim: String): Int = khsRepository.getTotalPassedSks(nim)

    /**
     * Computes the Indeks Prestasi (IP) for a single semester.
     *
     * IP = Σ(SKS × grade point) ÷ Σ(SKS), over the BEST grade of each course in that
     * semester. The `T` grade is ignored entirely.
     */
    fun getIpSemester(nim: String, semester: Int): Double {
        val khs = khsRepository.getKhsBySemester(nim, semester)
        if (khs.isEmpty()) return 0.0

        val bestGrades = khs
            .groupBy { it.courseCode }
            .mapValues { (_, entries) -> entries.maxBy { gradePoint(it.grade) } }

        val countedGrades = bestGrades.values.filter { it.grade != Grade.T }

        val totalValue = countedGrades.sumOf { entry ->
            val sks = courseRepository.getCourseByCode(entry.courseCode)?.sks?.toDouble() ?: 0.0
            sks * gradePoint(entry.grade)
        }
        val totalSks = countedGrades.sumOf { entry ->
            courseRepository.getCourseByCode(entry.courseCode)?.sks?.toDouble() ?: 0.0
        }

        return if (totalSks == 0.0) 0.0 else totalValue / totalSks
    }

    /** Fetches the list of passed course CODES (used for prerequisite checks). */
    fun getPassedCourseCodes(nim: String): List<String> = khsRepository.getPassedCourseCodes(nim)

    /**
     * Maps the previous semester's IP to the maximum SKS quota for the next semester.
     *
     * Quota tiers: IP ≥ 3.00 → 24, ≥ 2.50 → 22, ≥ 2.00 → 20, ≥ 1.50 → 18, otherwise → 16.
     */
    fun getMaxSksByIp(ip: Double): Int = when {
        ip >= 3.00 -> 24
        ip >= 2.50 -> 22
        ip >= 2.00 -> 20
        ip >= 1.50 -> 18
        else -> 16
    }

    /**
     * The maximum SKS quota for the current semester based on the previous semester's IP.
     * Semesters 1–2 (or a missing previous KHS) always get the full 24-SKS quota.
     */
    fun getMaxSks(nim: String, previousSemester: Int): Int {
        if (previousSemester < 3) return 24
        val previousKhs = khsRepository.getKhsBySemester(nim, previousSemester)
        if (previousKhs.isEmpty()) return 24
        return getMaxSksByIp(getIpSemester(nim, previousSemester))
    }

    /**
     * Determines which courses CAN be repeated and which are RECOMMENDED for grade
     * improvement, using the best grade per course and the courses offered this semester.
     */
    fun getRepeatableCourses(nim: String, currentSemester: Int): List<RepeatableCourse> {
        val allKhs = khsRepository.getKhsByNim(nim)
        if (allKhs.isEmpty()) return emptyList()

        val bestGradePerCourse = allKhs
            .groupBy { it.courseCode }
            .mapValues { (_, entries) -> entries.maxBy { gradePoint(it.grade) }.grade }

        val repeatable = bestGradePerCourse.filter { (_, grade) -> grade in REPEATABLE_GRADES }
        if (repeatable.isEmpty()) return emptyList()

        val availableCodes = courseRepository.getOfferedCourses().map { it.code }.toSet()
        val courses = courseRepository.getCoursesByCodes(repeatable.keys.toList()).associateBy { it.code }

        return repeatable.mapNotNull { (code, grade) ->
            if (code !in availableCodes) return@mapNotNull null
            // Ordinal grows as grades worsen, so `< C+` skips grades better than C+.
            if (grade.ordinal < Grade.C_PLUS.ordinal) return@mapNotNull null
            val course = courses[code] ?: return@mapNotNull null
            val mbkmEligible = course.type == CourseType.REGULAR_MANDATORY ||
                course.type == CourseType.REGULAR_ELECTIVE
            val recognitionSemester = if (course.normalSemester % 2 == 1) 7 else 6
            RepeatableCourse(
                code = code,
                name = course.name,
                sks = course.sks,
                bestGrade = grade,
                mustRepeat = grade == Grade.E || grade == Grade.T,
                recommendedForRepeat = true,
                mbkmEligible = mbkmEligible,
                recognitionSemester = recognitionSemester
            )
        }
    }

    /**
     * Builds the full KHS summary for the frontend: per-semester course lists, total SKS,
     * IP, and the cumulative IPK. Uses the BEST grade per course; `T` is ignored.
     */
    fun getKhsSummary(nim: String, name: String, currentSemester: Int): KhsSummaryResponse {
        val allKhs = khsRepository.getKhsByNim(nim)
        val courses = courseRepository
            .getCoursesByCodes(allKhs.map { it.courseCode }.distinct())
            .associateBy { it.code }

        val semesters = allKhs
            .groupBy { it.semesterTaken }
            .toSortedMap()
            .map { (semester, entries) ->
                val bestPerCourse = entries
                    .groupBy { it.courseCode }
                    .map { (_, group) -> group.maxBy { gradePoint(it.grade) } }
                    .sortedBy { it.courseCode }

                val courseDtos = bestPerCourse.map { khs ->
                    val course = courses[khs.courseCode]
                    KhsCourseDto(
                        courseCode = khs.courseCode,
                        courseName = course?.name ?: khs.courseCode,
                        classCode = khs.classCode,
                        sks = course?.sks ?: 0,
                        grade = khs.grade,
                        gradeLabel = gradeLabel(khs.grade),
                        gradePoint = gradePoint(khs.grade),
                        classType = "Reguler",
                        category = course?.type?.jenisLabel() ?: "Reguler"
                    )
                }

                val reference = entries.first()
                KhsSemesterDto(
                    semester = semester,
                    academicYear = reference.academicYear,
                    semesterType = reference.semesterType,
                    totalSks = courseDtos.sumOf { it.sks },
                    ip = round2(getIpSemester(nim, semester)),
                    courses = courseDtos
                )
            }

        val bestOverall = allKhs
            .groupBy { it.courseCode }
            .map { (_, group) -> group.maxBy { gradePoint(it.grade) } }
            .filter { it.grade != Grade.T }
        val ipkSks = bestOverall.sumOf { courses[it.courseCode]?.sks ?: 0 }
        val ipkPoints = bestOverall.sumOf {
            (courses[it.courseCode]?.sks?.toDouble() ?: 0.0) * gradePoint(it.grade)
        }
        val cumulativeIp = if (ipkSks == 0) 0.0 else round2(ipkPoints / ipkSks)

        return KhsSummaryResponse(
            nim = nim,
            name = name,
            currentSemester = currentSemester,
            totalPassedSks = getTotalPassedSks(nim),
            cumulativeIp = cumulativeIp,
            semesters = semesters
        )
    }

    /** A FLAT list of all KHS entries for the Kelola KRS page, sorted by semester then code. */
    fun getKhsEntries(nim: String): List<KhsEntryDto> {
        val khs = khsRepository.getKhsByNim(nim)
        val courses = courseRepository
            .getCoursesByCodes(khs.map { it.courseCode }.distinct())
            .associateBy { it.code }

        return khs
            .sortedWith(compareBy({ it.semesterTaken }, { it.courseCode }))
            .map { entry ->
                val course = courses[entry.courseCode]
                KhsEntryDto(
                    courseCode = entry.courseCode,
                    courseName = course?.name ?: entry.courseCode,
                    classCode = entry.classCode,
                    sks = course?.sks ?: 0,
                    grade = entry.grade,
                    gradeLabel = gradeLabel(entry.grade),
                    semesterTaken = entry.semesterTaken,
                    academicYear = entry.academicYear,
                    semesterType = entry.semesterType,
                    classType = "Reguler",
                    category = course?.type?.jenisLabel() ?: "Reguler"
                )
            }
    }

    /** Adds a single KHS entry manually. */
    fun addKhsEntry(khs: Khs): AddKhsOutcome {
        if (courseRepository.getCourseByCode(khs.courseCode) == null) return AddKhsOutcome.COURSE_NOT_FOUND
        val inserted = khsRepository.addKhs(khs)
        return if (inserted) AddKhsOutcome.SUCCESS else AddKhsOutcome.DUPLICATE
    }

    /** Deletes a single KHS entry by its key. */
    fun deleteKhsEntry(nim: String, courseCode: String, semesterTaken: Int): Boolean =
        khsRepository.deleteKhs(nim, courseCode, semesterTaken)

    companion object {
        /** Grades that may still be improved by repeating the course. */
        private val REPEATABLE_GRADES =
            setOf(Grade.B_PLUS, Grade.B, Grade.C_PLUS, Grade.C, Grade.D, Grade.E, Grade.T)

        /** Numeric grade point for a [Grade] (A=4.0 … E/T=0.0). */
        fun gradePoint(grade: Grade): Double = when (grade) {
            Grade.A -> 4.0
            Grade.B_PLUS -> 3.5
            Grade.B -> 3.0
            Grade.C_PLUS -> 2.5
            Grade.C -> 2.0
            Grade.D -> 1.0
            Grade.E -> 0.0
            Grade.T -> 0.0
        }

        /** Display label for a grade (e.g. `B_PLUS` → "B+"). */
        fun gradeLabel(grade: Grade): String = when (grade) {
            Grade.B_PLUS -> "B+"
            Grade.C_PLUS -> "C+"
            else -> grade.name
        }

        /** Rounds [value] to 2 decimal places. */
        private fun round2(value: Double): Double = round(value * 100.0) / 100.0
    }
}
