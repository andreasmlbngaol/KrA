package id.andreasmlbngaol.kra.academic.domain.model

import id.andreasmlbngaol.kra.core.enums.SemesterType

/**
 * A single academic term: one calendar year plus a semester type (odd/even),
 * e.g. odd semester 2025/2026.
 */
data class AcademicTerm(
    val id: Int,
    val year: Int,
    val semesterType: SemesterType
)
