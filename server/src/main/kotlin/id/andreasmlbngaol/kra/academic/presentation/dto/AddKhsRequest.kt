package id.andreasmlbngaol.kra.academic.presentation.dto

/** Request body for manually adding a KHS entry. */
data class AddKhsRequest(
    val courseCode: String,
    val classCode: String,
    val grade: id.andreasmlbngaol.kra.core.enums.Grade,
    val semesterTaken: Int,
    val academicYear: Int,
    val semesterType: id.andreasmlbngaol.kra.core.enums.SemesterType
)
