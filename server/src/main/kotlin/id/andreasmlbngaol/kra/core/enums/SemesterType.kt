package id.andreasmlbngaol.kra.core.enums

import kotlinx.serialization.Serializable

/** Semester parity: GANJIL (odd) or GENAP (even). Serialized by enum name. */
@Serializable
enum class SemesterType {
    GANJIL,
    GENAP
}
