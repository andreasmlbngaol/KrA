package id.andreasmlbngaol.kra.academic.presentation.dto

import id.andreasmlbngaol.kra.core.enums.Grade
import id.andreasmlbngaol.kra.core.enums.SemesterType

/** One course-grade row in the KHS (best grade per course). */
data class KhsCourseDto(
    val courseCode: String,
    val courseName: String,
    val classCode: String,
    val sks: Int,
    val grade: Grade,
    val gradeLabel: String,
    val gradePoint: Double,
    val classType: String = "Reguler",
    val category: String = "Reguler"
)

/** All grades for one completed semester. */
data class KhsSemesterDto(
    val semester: Int,
    val academicYear: Int,
    val semesterType: SemesterType,
    val totalSks: Int,
    val ip: Double,
    val courses: List<KhsCourseDto>
)

/** Full student KHS summary for display in the FE. */
data class KhsSummaryResponse(
    val nim: String,
    val name: String,
    val currentSemester: Int,
    val totalPassedSks: Int,
    val cumulativeIp: Double,
    val semesters: List<KhsSemesterDto>
)

/** One KHS entry in flat form (for the "Kelola KRS" page). */
data class KhsEntryDto(
    val courseCode: String,
    val courseName: String,
    val classCode: String,
    val sks: Int,
    val grade: Grade,
    val gradeLabel: String,
    val semesterTaken: Int,
    val academicYear: Int,
    val semesterType: SemesterType,
    val classType: String = "Reguler",
    val category: String = "Reguler"
)
