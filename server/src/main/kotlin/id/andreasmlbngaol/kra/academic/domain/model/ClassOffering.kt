package id.andreasmlbngaol.kra.academic.domain.model

import id.andreasmlbngaol.kra.core.enums.Day
import java.util.UUID

/**
 * A class offering for ONE specific academic year.
 *
 * A class identity (KOM A/B/C) stays the same each year, but the lecturer, room,
 * schedule, and quota may differ per term. Hence "Pemrograman Dasar A 2024" and
 * "Pemrograman Dasar A 2025" are two different ClassOfferings.
 */
data class ClassOffering(
    val id: UUID,
    val courseCode: String,
    val classCode: String,
    val term: AcademicTerm,
    val room: String?,
    val day: Day?,
    val startTime: String?,
    val endTime: String?,
    val quota: Int,
    val lecturers: List<Lecturer> = emptyList()
)
