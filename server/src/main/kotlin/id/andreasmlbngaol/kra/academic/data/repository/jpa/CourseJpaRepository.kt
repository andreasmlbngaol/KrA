package id.andreasmlbngaol.kra.academic.data.repository.jpa

import id.andreasmlbngaol.kra.academic.data.entity.CourseEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CourseJpaRepository : JpaRepository<CourseEntity, String> {
    /** Courses that belong to [semester]'s curriculum package. */
    @Query(
        """
        select c from CourseEntity c, CurriculumPackageEntity cp
        where cp.courseCode = c.code and cp.semester = :semester
        """
    )
    fun findAvailableInSemester(@Param("semester") semester: Int): List<CourseEntity>
}
