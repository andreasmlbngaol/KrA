package id.andreasmlbngaol.kra.academic.presentation.dto

import id.andreasmlbngaol.kra.core.enums.Day

/** One course inside the current-semester KRS draft. */
data class KrsDraftItemDto(
    val classOfferingId: String,
    val courseCode: String,
    val courseName: String,
    val classCode: String,
    val room: String? = null,
    val day: Day? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val sks: Int,
    val semester: Int = 0,
    val classType: String = "Reguler",
    val category: String = "Reguler",
    val lecturers: List<String> = emptyList()
)

/** Current-semester KRS draft for display on the "Kelola KRS" page. */
data class KrsDraftResponse(
    val semester: Int,
    val exists: Boolean,
    val totalSks: Int,
    val items: List<KrsDraftItemDto>
)
