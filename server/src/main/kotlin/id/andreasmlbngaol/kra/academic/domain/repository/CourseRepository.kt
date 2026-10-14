package id.andreasmlbngaol.kra.academic.domain.repository

import id.andreasmlbngaol.kra.academic.domain.model.AcademicTerm
import id.andreasmlbngaol.kra.academic.domain.model.ClassOffering
import id.andreasmlbngaol.kra.academic.domain.model.Course
import java.util.UUID

/**
 * Read-only access to the course catalog: courses, class offerings, SKS totals,
 * and the active academic term.
 */
interface CourseRepository {
    /** Returns the courses offered in [semester]. */
    fun getAvailableCourses(semester: Int): List<Course>

    /** Returns the course with [code], or `null` if none matches. */
    fun getCourseByCode(code: String): Course?

    /** Returns the class offerings (KOM) for [courseCode]. */
    fun getClassOfferings(courseCode: String): List<ClassOffering>

    /** Returns the combined SKS of the courses identified by [courseCodes]. */
    fun getTotalSksByCodes(courseCodes: List<String>): Int

    /** Returns the courses matching [codes]; unmatched codes are omitted. */
    fun getCoursesByCodes(codes: List<String>): List<Course>

    /** Returns the class offerings whose IDs are in [ids]. */
    fun getClassOfferingsByIds(ids: List<UUID>): List<ClassOffering>

    /** Returns the currently active academic term, or `null` if none is active. */
    fun getActiveTerm(): AcademicTerm?
}
