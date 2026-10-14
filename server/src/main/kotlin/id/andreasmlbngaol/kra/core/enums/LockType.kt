package id.andreasmlbngaol.kra.core.enums

import kotlinx.serialization.Serializable

/** How strongly a curriculum-package course is locked to its semester. */
@Serializable
enum class LockType {
    HARD,
    SOFT
}
