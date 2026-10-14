package id.andreasmlbngaol.kra.academic.domain.model

import id.andreasmlbngaol.kra.core.enums.CourseType
import id.andreasmlbngaol.kra.core.enums.Day
import java.util.UUID

/**
 * One item in a KRS draft: a selected class offering enriched with course and
 * schedule details for display and validation.
 */
data class KrsDraftItem(
    val classOfferingId: UUID,
    val courseCode: String,
    val courseName: String,
    val classCode: String,
    val room: String?,
    val day: Day?,
    val startTime: String?,
    val endTime: String?,
    val sks: Int,
    val semester: Int,
    val normalSemester: Int = 0,
    val courseType: CourseType? = null,
    val lecturers: List<String> = emptyList()
)
