package id.andreasmlbngaol.kra.academic.domain.model

import java.util.UUID

/**
 * A saved KRS entry: one class offering taken by a student in a semester.
 */
data class Krs(
    val nim: String,
    val classOfferingId: UUID,
    val semester: Int
)
