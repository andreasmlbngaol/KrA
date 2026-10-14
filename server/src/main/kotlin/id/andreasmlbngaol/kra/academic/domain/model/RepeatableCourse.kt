package id.andreasmlbngaol.kra.academic.domain.model

import id.andreasmlbngaol.kra.core.enums.Grade

/**
 * A course eligible to be repeated, with the best grade so far and repeat flags.
 *
 * @property mustRepeat The best grade is E/T, so the course MUST be repeated.
 * @property recommendedForRepeat The best grade is C+ or below; proactively suggested in the normal flow.
 * @property mbkmEligible Whether repeating this course counts toward MBKM recognition.
 * @property recognitionSemester The semester in which the repeat would be recognized.
 */
data class RepeatableCourse(
    val code: String,
    val name: String,
    val sks: Int,
    val bestGrade: Grade,
    val mustRepeat: Boolean,
    val recommendedForRepeat: Boolean,
    val mbkmEligible: Boolean,
    val recognitionSemester: Int
)
