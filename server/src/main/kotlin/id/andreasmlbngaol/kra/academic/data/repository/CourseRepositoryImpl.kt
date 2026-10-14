package id.andreasmlbngaol.kra.academic.data.repository

import id.andreasmlbngaol.kra.academic.data.entity.AcademicTermEntity
import id.andreasmlbngaol.kra.academic.data.entity.ClassOfferingEntity
import id.andreasmlbngaol.kra.academic.data.entity.CourseEntity
import id.andreasmlbngaol.kra.academic.data.entity.LecturerEntity
import id.andreasmlbngaol.kra.academic.data.repository.jpa.AcademicTermJpaRepository
import id.andreasmlbngaol.kra.academic.data.repository.jpa.ClassOfferingJpaRepository
import id.andreasmlbngaol.kra.academic.data.repository.jpa.CourseConstraintJpaRepository
import id.andreasmlbngaol.kra.academic.data.repository.jpa.CourseJpaRepository
import id.andreasmlbngaol.kra.academic.data.repository.jpa.CoursePrerequisiteJpaRepository
import id.andreasmlbngaol.kra.academic.data.repository.jpa.LecturerJpaRepository
import id.andreasmlbngaol.kra.academic.domain.model.AcademicTerm
import id.andreasmlbngaol.kra.academic.domain.model.ClassOffering
import id.andreasmlbngaol.kra.academic.domain.model.Course
import id.andreasmlbngaol.kra.academic.domain.model.Lecturer
import id.andreasmlbngaol.kra.academic.domain.repository.CourseRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Repository
@Transactional(readOnly = true)
class CourseRepositoryImpl(
    private val courses: CourseJpaRepository,
    private val constraints: CourseConstraintJpaRepository,
    private val prerequisites: CoursePrerequisiteJpaRepository,
    private val offerings: ClassOfferingJpaRepository,
    private val terms: AcademicTermJpaRepository,
    private val lecturers: LecturerJpaRepository
) : CourseRepository {

    override fun getAvailableCourses(semester: Int): List<Course> {
        val entities = courses.findAvailableInSemester(semester)
        return entities.map { toCourse(it) }
    }

    override fun getCourseByCode(code: String): Course? =
        courses.findById(code).map { toCourse(it) }.orElse(null)

    override fun getClassOfferings(courseCode: String): List<ClassOffering> =
        offerings.findActiveByCourseCode(courseCode).map { toOffering(it) }

    override fun getTotalSksByCodes(courseCodes: List<String>): Int {
        if (courseCodes.isEmpty()) return 0
        return courses.findAllById(courseCodes).sumOf { it.sks }
    }

    override fun getCoursesByCodes(codes: List<String>): List<Course> {
        if (codes.isEmpty()) return emptyList()
        return courses.findAllById(codes).map { toCourse(it) }
    }

    override fun getClassOfferingsByIds(ids: List<UUID>): List<ClassOffering> {
        if (ids.isEmpty()) return emptyList()
        return offerings.findByIdInWithLecturers(ids).map { toOffering(it) }
    }

    override fun getActiveTerm(): AcademicTerm? =
        terms.findByIsActiveTrue()?.let {
            AcademicTerm(id = 0, year = it.year, semesterType = it.semesterType)
        }

    // --- Mapping ---

    private fun toCourse(e: CourseEntity): Course {
        val minSks = constraints.findById(e.code).map { it.minSks }.orElse(0)
        val prereqs = prerequisites.findByCourseCodeIn(listOf(e.code)).map { it.prerequisiteCode }
        return Course(
            code = e.code,
            name = e.name,
            sks = e.sks,
            normalSemester = e.normalSemester,
            type = e.type,
            minSks = minSks,
            parityFree = false, // removed from schema; offerings data is the source of truth
            prerequisites = prereqs
        )
    }

    private fun toOffering(e: ClassOfferingEntity): ClassOffering = ClassOffering(
        id = e.id,
        courseCode = e.course.code,
        classCode = e.classCode,
        term = AcademicTerm(id = 0, year = e.term.year, semesterType = e.term.semesterType),
        room = e.room,
        day = e.day,
        startTime = e.startTime,
        endTime = e.endTime,
        quota = e.quota,
        lecturers = e.lecturerNidns.mapNotNull { nidn ->
            lecturers.findById(nidn).map { Lecturer(it.nidn, it.name) }.orElse(null)
        }
    )
}
