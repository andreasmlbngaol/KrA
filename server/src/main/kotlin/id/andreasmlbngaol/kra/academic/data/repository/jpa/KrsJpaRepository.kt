package id.andreasmlbngaol.kra.academic.data.repository.jpa

import id.andreasmlbngaol.kra.academic.data.entity.KrsEntity
import id.andreasmlbngaol.kra.academic.data.entity.KrsId
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface KrsJpaRepository : JpaRepository<KrsEntity, KrsId> {
    fun findByNimAndSemester(nim: String, semester: Int): List<KrsEntity>

    fun deleteByNimAndClassOfferingIdAndSemester(nim: String, classOfferingId: UUID, semester: Int): Long
}
