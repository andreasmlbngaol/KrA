package id.andreasmlbngaol.kra.academic.data.repository.jpa

import id.andreasmlbngaol.kra.academic.data.entity.AcademicTermEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AcademicTermJpaRepository : JpaRepository<AcademicTermEntity, UUID> {
    /** The single active term. Throws if more than one row is active, by design. */
    fun findByIsActiveTrue(): AcademicTermEntity?
}
