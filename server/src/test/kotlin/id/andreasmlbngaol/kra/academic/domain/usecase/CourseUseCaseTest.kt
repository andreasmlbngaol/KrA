package id.andreasmlbngaol.kra.academic.domain.usecase

import id.andreasmlbngaol.kra.academic.domain.model.AcademicTerm
import id.andreasmlbngaol.kra.academic.domain.model.ClassOffering
import id.andreasmlbngaol.kra.academic.domain.model.Course
import id.andreasmlbngaol.kra.academic.domain.model.CurriculumPackage
import id.andreasmlbngaol.kra.academic.domain.model.Khs
import id.andreasmlbngaol.kra.academic.domain.repository.CourseRepository
import id.andreasmlbngaol.kra.academic.domain.repository.CurriculumRepository
import id.andreasmlbngaol.kra.academic.domain.repository.KhsRepository
import id.andreasmlbngaol.kra.core.enums.CourseType
import id.andreasmlbngaol.kra.core.enums.Grade
import id.andreasmlbngaol.kra.core.enums.SemesterType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * Pure tests for the semester-scoped logic: cumulative mandatory lists, the derived
 * semester context, and the importance score. No Spring, no database.
 */
class CourseUseCaseTest {

    private fun course(
        code: String,
        semester: Int,
        type: CourseType = CourseType.REGULAR_MANDATORY,
        sks: Int = 3,
        prerequisites: List<String> = emptyList()
    ) = Course(
        code = code,
        name = "Mata Kuliah $code",
        sks = sks,
        normalSemester = semester,
        type = type,
        prerequisites = prerequisites
    )

    private fun khs(code: String, grade: Grade, semesterTaken: Int) = Khs(
        nim = "230001",
        courseCode = code,
        classCode = "A",
        grade = grade,
        semesterTaken = semesterTaken,
        academicYear = 2024,
        semesterType = SemesterType.GANJIL
    )

    private fun useCase(
        catalog: List<Course>,
        khsRows: List<Khs>
    ): CourseUseCase = CourseUseCase(
        courseRepository = object : CourseRepository {
            override fun getOfferedCourses(): List<Course> = catalog
            override fun getAllCourses(): List<Course> = catalog
            override fun getCourseByCode(code: String): Course? = catalog.firstOrNull { it.code == code }
            override fun getClassOfferings(courseCode: String): List<ClassOffering> = emptyList()
            override fun getClassOfferingsByCourseCodes(courseCodes: List<String>): List<ClassOffering> = emptyList()
            override fun getTotalSksByCodes(courseCodes: List<String>): Int =
                catalog.filter { it.code in courseCodes }.sumOf { it.sks }
            override fun getCoursesByCodes(codes: List<String>): List<Course> =
                catalog.filter { it.code in codes }
            override fun getClassOfferingsByIds(ids: List<UUID>): List<ClassOffering> = emptyList()
            override fun getActiveTerm(): AcademicTerm? = null
        },
        curriculumRepository = object : CurriculumRepository {
            override fun getCurriculumPackage(semester: Int): List<CurriculumPackage> = emptyList()
        },
        khsRepository = object : KhsRepository {
            override fun getKhsByNim(nim: String): List<Khs> = khsRows
            override fun getTotalPassedSks(nim: String): Int = 0
            override fun getKhsBySemester(nim: String, semester: Int): List<Khs> =
                khsRows.filter { it.semesterTaken == semester }
            override fun getPassedCourseCodes(nim: String): List<String> =
                khsRows.filter { it.grade != Grade.E && it.grade != Grade.T }.map { it.courseCode }
            override fun addKhs(khs: Khs): Boolean = true
            override fun deleteKhs(nim: String, courseCode: String, semesterTaken: Int): Boolean = true
        }
    )

    @Test
    fun `mandatory list is cumulative, not limited to the requested semester's package`() {
        val catalog = listOf(
            course("ILK3101", 5),
            course("ILK3201", 6),
            course("ILK4105", 7)
        )
        val khs = listOf(
            khs("ILK3101", Grade.E, 5),
            khs("ILK3201", Grade.A, 6),
            khs("ILK4105", Grade.E, 7)
        )

        val remaining = useCase(catalog, khs).getRemainingMandatoryCourses("230001", 8)

        // Semester 8 has no curriculum package at all; the backlog must still appear.
        assertEquals(listOf("ILK3101", "ILK4105"), remaining.map { it.code })
    }

    @Test
    fun `context reserves MKWU for the semester being repeated`() {
        val catalog = listOf(course("ILK3101", 5), course("ILK3201", 6))
        val khs = listOf(khs("ILK3201", Grade.A, 6))

        val context = useCase(catalog, khs).resolveSemesterContext("230001", 6)

        // Administratively semester 6 (no MKWU), but the load is semester 5 (2 SKS MKWU).
        assertEquals(5, context.lowestIncomplete)
        assertEquals(2, context.mkwuReservedSks)
        assertEquals(6, context.lastGraded)
    }

    @Test
    fun `importance ignores dependents that are only electives`() {
        val catalog = listOf(
            course("ILK2203", 4),
            course("ILK3105", 5, prerequisites = listOf("ILK2203")),
            course("ILK3102", 5),
            course("ILK4110", 7, type = CourseType.REGULAR_ELECTIVE, prerequisites = listOf("ILK3102"))
        )

        val importance = useCase(catalog, emptyList()).getImportanceScores("230001")

        assertTrue(importance.getValue("ILK2203") > 1, "ILK2203 gates a mandatory course")
        assertEquals(1, importance.getValue("ILK3102"), "ILK3102 only gates an elective")
    }

    @Test
    fun `D load above 8 SKS requires repeating the excess`() {
        val catalog = listOf(
            course("ILK2203", 4, sks = 3),
            course("ILK2208", 4, sks = 3),
            course("ILK2211", 4, sks = 3)
        )
        val khs = listOf(
            khs("ILK2203", Grade.D, 4),
            khs("ILK2208", Grade.D, 4),
            khs("ILK2211", Grade.D, 4)
        )

        val context = useCase(catalog, khs).resolveSemesterContext("230001", 5)

        assertEquals(9, context.dSks)
        assertEquals(1, context.dRepeatMinSks)
    }

    @Test
    fun `D load within the 8 SKS limit keeps repeating optional`() {
        val catalog = listOf(
            course("ILK2203", 4, sks = 3),
            course("ILK2208", 4, sks = 3)
        )
        val khs = listOf(
            khs("ILK2203", Grade.D, 4),
            khs("ILK2208", Grade.D, 4)
        )

        val context = useCase(catalog, khs).resolveSemesterContext("230001", 5)

        assertEquals(6, context.dSks)
        assertEquals(0, context.dRepeatMinSks)
    }

    @Test
    fun `importance lists the courses that a repeat candidate unlocks`() {
        val catalog = listOf(
            course("ILK2203", 4),
            course("ILK3105", 5, prerequisites = listOf("ILK2203")),
            course("ILK3107", 5, prerequisites = listOf("ILK2203")),
            course("ILK3102", 5)
        )
        val khs = listOf(khs("ILK2203", Grade.D, 4), khs("ILK3102", Grade.A, 5))

        val info = useCase(catalog, khs).getImportance("230001").getValue("ILK2203")

        // D counts as passed, but the course is still a repeat candidate and gates two
        // mandatory courses, so its importance must not collapse to 1.
        assertEquals(3, info.score)
        assertEquals(listOf("ILK3105", "ILK3107"), info.unlockedCourses.map { it.code })
    }

    @Test
    fun `last graded semester comes from the KHS, not from the administrative semester`() {
        val catalog = listOf(course("ILK3101", 5))
        val khs = listOf(khs("ILK2104", Grade.A, 3), khs("ILK2203", Grade.A, 4))

        val context = useCase(catalog, khs).resolveSemesterContext("230001", 7)

        assertEquals(4, context.lastGraded)
    }
}
