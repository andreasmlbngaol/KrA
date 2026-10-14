package id.andreasmlbngaol.kra.academic.domain.model

/**
 * Result of attempting to save a KRS draft.
 *
 * Saving a draft is always validated first in the domain layer
 * ([id.andreasmlbngaol.kra.academic.domain.usecase.KrsUseCase.saveDraftKrs]). The draft is
 * only persisted when nothing blocks it (schedule conflicts, prerequisite violations,
 * or total SKS exceeding the quota). Otherwise the save is cancelled and the reasons
 * are carried via [Rejected.validation].
 */
sealed interface SaveDraftKrsResult {
    /** The draft passed validation and was saved. */
    data class Success(val validation: KrsValidationResult) : SaveDraftKrsResult

    /** The draft was rejected by validation; nothing was saved. */
    data class Rejected(val validation: KrsValidationResult) : SaveDraftKrsResult
}
