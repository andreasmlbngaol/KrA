package id.andreasmlbngaol.kra.academic.data.repository.jpa

import id.andreasmlbngaol.kra.academic.data.entity.CourseEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface CourseJpaRepository : JpaRepository<CourseEntity, String> {
    /**
     * Courses that have at least one class offering in the active academic term.
     *
     * Availability is driven by the real offering data: if a course has a class this
     * term, it is available — regardless of the curriculum package it belongs to or
     * the odd/even parity of [CourseEntity.normalSemester].
     */
    @Query(
        """
        select c from CourseEntity c
        where exists (
            select 1 from ClassOfferingEntity o
            where o.course = c and o.term.isActive = true
        )
        """
    )
    fun findOfferedInActiveTerm(): List<CourseEntity>
}
