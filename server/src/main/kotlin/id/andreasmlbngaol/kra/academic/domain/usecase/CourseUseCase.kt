package id.andreasmlbngaol.kra.academic.domain.usecase

import id.andreasmlbngaol.kra.academic.domain.model.ClassOffering
import id.andreasmlbngaol.kra.academic.domain.model.Course
import id.andreasmlbngaol.kra.academic.domain.model.CurriculumPackage
import id.andreasmlbngaol.kra.academic.domain.repository.CourseRepository
import id.andreasmlbngaol.kra.academic.domain.repository.CurriculumRepository
import id.andreasmlbngaol.kra.academic.domain.repository.KhsRepository
import id.andreasmlbngaol.kra.core.enums.CourseType
import org.springframework.stereotype.Service

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
    /** Lists every course offered in the given semester. */
    fun getAvailableCourses(semester: Int): List<Course> =
        courseRepository.getAvailableCourses(semester)

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
     * Mandatory curriculum-package courses for [semester] that the student has NOT
     * passed yet.
     */
    fun getRemainingMandatoryCourses(nim: String, semester: Int): List<Course> {
        val packageCodes = curriculumRepository.getCurriculumPackage(semester).map { it.courseCode }
        if (packageCodes.isEmpty()) return emptyList()

        val passedCodes = khsRepository.getPassedCourseCodes(nim).toSet()
        val remainingCodes = packageCodes.filter { it !in passedCodes }
        return courseRepository.getCoursesByCodes(remainingCodes)
    }

    /**
     * The mandatory elective group for the current [semester] (the "choose 1 of N"
     * group): offered REGULAR_ELECTIVE courses whose normalSemester equals [semester].
     */
    fun getSemesterElectives(semester: Int): List<Course> =
        courseRepository.getAvailableCourses(semester)
            .filter { it.type == CourseType.REGULAR_ELECTIVE && it.normalSemester == semester }

    /** Fetches courses for an arbitrary set of course codes. */
    fun getCoursesByCodes(codes: List<String>): List<Course> =
        courseRepository.getCoursesByCodes(codes)
}
