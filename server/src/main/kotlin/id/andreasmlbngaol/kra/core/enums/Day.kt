package id.andreasmlbngaol.kra.core.enums

import kotlinx.serialization.Serializable

/** Teaching days of the week (Monday-Friday). Serialized by enum name. */
@Serializable
enum class Day {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY
}
