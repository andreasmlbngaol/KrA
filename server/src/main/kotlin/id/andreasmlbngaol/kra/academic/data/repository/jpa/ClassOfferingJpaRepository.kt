package id.andreasmlbngaol.kra.academic.data.repository.jpa

import id.andreasmlbngaol.kra.academic.data.entity.ClassOfferingEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface ClassOfferingJpaRepository : JpaRepository<ClassOfferingEntity, UUID> {
    @Query(
        """
        select distinct o from ClassOfferingEntity o
        left join fetch o.lecturerNidns
        where o.course.code = :courseCode and o.term.isActive = true
        """
    )
    fun findActiveByCourseCode(@Param("courseCode") courseCode: String): List<ClassOfferingEntity>

    @Query(
        """
        select distinct o from ClassOfferingEntity o
        left join fetch o.lecturerNidns
        where o.course.code in :courseCodes and o.term.isActive = true
        """
    )
    fun findActiveByCourseCodes(@Param("courseCodes") courseCodes: Collection<String>): List<ClassOfferingEntity>

    @Query(
        """
        select distinct o from ClassOfferingEntity o
        left join fetch o.lecturerNidns
        where o.id in :ids
        """
    )
    fun findByIdInWithLecturers(@Param("ids") ids: Collection<UUID>): List<ClassOfferingEntity>
}
