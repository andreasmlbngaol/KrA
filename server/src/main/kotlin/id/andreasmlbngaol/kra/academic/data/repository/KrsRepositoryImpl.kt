package id.andreasmlbngaol.kra.academic.data.repository

import id.andreasmlbngaol.kra.academic.data.entity.KrsEntity
import id.andreasmlbngaol.kra.academic.data.repository.jpa.KrsJpaRepository
import id.andreasmlbngaol.kra.academic.domain.model.Krs
import id.andreasmlbngaol.kra.academic.domain.repository.KrsRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Repository
@Transactional
class KrsRepositoryImpl(
    private val krs: KrsJpaRepository
) : KrsRepository {

    override fun getKrsByNim(nim: String, semester: Int): List<Krs> =
        krs.findByNimAndSemester(nim, semester).map { Krs(it.nim, it.classOfferingId, it.semester) }

    override fun saveDraftKrs(nim: String, classOfferingIds: List<UUID>, semester: Int) {
        krs.findByNimAndSemester(nim, semester).forEach { krs.delete(it) }
        krs.saveAll(classOfferingIds.map { KrsEntity(nim, it, semester) })
    }

    override fun deleteDraftItem(nim: String, classOfferingId: UUID, semester: Int): Boolean =
        krs.deleteByNimAndClassOfferingIdAndSemester(nim, classOfferingId, semester) > 0
}
