package id.andreasmlbngaol.kra.core.enums

import kotlinx.serialization.Serializable

/**
 * Letter grades from A down to E, plus T (incomplete/pending).
 *
 * Serialized by enum name (e.g. "A", "B_PLUS"), which is also the stored/wire value.
 */
@Serializable
enum class Grade {
    A,
    B_PLUS,
    B,
    C_PLUS,
    C,
    D,
    E,
    T
}
