package id.andreasmlbngaol.kra.academic.data.repository.jpa

import id.andreasmlbngaol.kra.academic.data.entity.CoursePrerequisiteEntity
import id.andreasmlbngaol.kra.academic.data.entity.CoursePrerequisiteId
import org.springframework.data.jpa.repository.JpaRepository

interface CoursePrerequisiteJpaRepository : JpaRepository<CoursePrerequisiteEntity, CoursePrerequisiteId> {
    fun findByCourseCodeIn(courseCodes: Collection<String>): List<CoursePrerequisiteEntity>
}
