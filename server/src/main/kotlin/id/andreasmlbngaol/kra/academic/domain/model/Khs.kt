package id.andreasmlbngaol.kra.academic.domain.model

import id.andreasmlbngaol.kra.core.enums.Grade
import id.andreasmlbngaol.kra.core.enums.SemesterType

/**
 * A single KHS (transcript) entry: one course taken by a student in one term,
 * with the grade obtained.
 */
data class Khs(
    val nim: String,
    val courseCode: String,
    val classCode: String,
    val grade: Grade,
    val semesterTaken: Int,
    val academicYear: Int,
    val semesterType: SemesterType
)
