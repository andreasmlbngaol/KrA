package id.andreasmlbngaol.kra.academic.domain.repository

import id.andreasmlbngaol.kra.academic.domain.model.Krs
import java.util.UUID

/** Storage for KRS drafts: reading, saving, and removing draft entries. */
interface KrsRepository {
    /** Returns the KRS entries saved by [nim] for [semester]. */
    fun getKrsByNim(nim: String, semester: Int): List<Krs>

    /** Persists the draft for [nim] in [semester] from the given [classOfferingIds]. */
    fun saveDraftKrs(nim: String, classOfferingIds: List<UUID>, semester: Int)

    /** Removes one class offering from the draft; returns `true` if an entry was removed. */
    fun deleteDraftItem(nim: String, classOfferingId: UUID, semester: Int): Boolean
}
