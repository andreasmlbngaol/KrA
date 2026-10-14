package id.andreasmlbngaol.kra.academic.domain.model

import id.andreasmlbngaol.kra.core.enums.LockType

/**
 * One curriculum-package entry: a course that belongs to a semester's package,
 * with how strongly it is locked to that semester.
 */
data class CurriculumPackage(
    val semester: Int,
    val courseCode: String,
    val lockType: LockType
)
