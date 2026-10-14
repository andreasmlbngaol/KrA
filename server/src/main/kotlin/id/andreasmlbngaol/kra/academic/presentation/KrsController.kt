package id.andreasmlbngaol.kra.academic.presentation

import id.andreasmlbngaol.kra.academic.domain.usecase.KrsUseCase
import id.andreasmlbngaol.kra.academic.domain.usecase.StudentUseCase
import id.andreasmlbngaol.kra.academic.presentation.dto.KrsDraftItemDto
import id.andreasmlbngaol.kra.academic.presentation.dto.KrsDraftResponse
import id.andreasmlbngaol.kra.core.enums.jenisLabel
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * KRS endpoints under `/api/krs` for the logged-in student: read the current-semester
 * draft and remove a single course from it.
 */
@RestController
@RequestMapping("/api/krs")
class KrsController(
    private val krsUseCase: KrsUseCase,
    private val studentUseCase: StudentUseCase
) {
    @GetMapping("/draft")
    fun draft(auth: Authentication): ResponseEntity<Any> {
        val student = studentUseCase.getStudentByNim(auth.name)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Data mahasiswa tidak ditemukan")

        val items = krsUseCase.getKrsDraftDetails(auth.name, student.semester)
        return ResponseEntity.ok(
            KrsDraftResponse(
                semester = student.semester,
                exists = items.isNotEmpty(),
                totalSks = items.sumOf { it.sks },
                items = items.map {
                    KrsDraftItemDto(
                        classOfferingId = it.classOfferingId.toString(),
                        courseCode = it.courseCode,
                        courseName = it.courseName,
                        classCode = it.classCode,
                        room = it.room,
                        day = it.day,
                        startTime = it.startTime,
                        endTime = it.endTime,
                        sks = it.sks,
                        semester = it.normalSemester,
                        category = it.courseType?.jenisLabel() ?: "Reguler",
                        lecturers = it.lecturers
                    )
                }
            )
        )
    }

    @DeleteMapping("/draft/{classOfferingId}")
    fun deleteDraftItem(
        @PathVariable classOfferingId: String,
        auth: Authentication
    ): ResponseEntity<String> {
        val id = runCatching { UUID.fromString(classOfferingId) }.getOrNull()
            ?: return ResponseEntity.badRequest().body("ID kelas tidak valid")

        val student = studentUseCase.getStudentByNim(auth.name)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Data mahasiswa tidak ditemukan")

        val deleted = krsUseCase.deleteDraftItem(auth.name, id, student.semester)
        return if (deleted) ResponseEntity.ok("Mata kuliah dihapus dari KRS")
        else ResponseEntity.status(HttpStatus.NOT_FOUND).body("Mata kuliah tidak ada di draft")
    }
}
