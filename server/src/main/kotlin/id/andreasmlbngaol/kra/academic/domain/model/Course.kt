package id.andreasmlbngaol.kra.academic.domain.model

import id.andreasmlbngaol.kra.core.enums.CourseType

/**
 * A course in the curriculum catalog.
 *
 * @property normalSemester The semester this course is normally taken in.
 * @property minSks Minimum passed SKS required to take it (capstone courses; 0 otherwise).
 * @property parityFree Whether it is offered every semester regardless of odd/even parity.
 * @property prerequisites Course codes that must be passed first.
 */
data class Course(
    val code: String,
    val name: String,
    val sks: Int,
    val normalSemester: Int,
    val type: CourseType,
    val minSks: Int = 0,
    val parityFree: Boolean = false,
    val prerequisites: List<String> = emptyList()
)
