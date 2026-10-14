package id.andreasmlbngaol.kra.academic.presentation

import id.andreasmlbngaol.kra.academic.domain.model.Khs
import id.andreasmlbngaol.kra.academic.domain.usecase.AddKhsOutcome
import id.andreasmlbngaol.kra.academic.domain.usecase.KhsUseCase
import id.andreasmlbngaol.kra.academic.domain.usecase.StudentUseCase
import id.andreasmlbngaol.kra.academic.presentation.dto.AddKhsRequest
import id.andreasmlbngaol.kra.academic.presentation.dto.KhsEntryDto
import id.andreasmlbngaol.kra.academic.presentation.dto.KhsSummaryResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * KHS endpoints under `/api/khs` for the logged-in student (NIM from the access token):
 * summary, flat entry list, and adding/deleting a single KHS entry.
 */
@RestController
@RequestMapping("/api/khs")
class KhsController(
    private val khsUseCase: KhsUseCase,
    private val studentUseCase: StudentUseCase
) {
    @GetMapping
    fun summary(auth: Authentication): ResponseEntity<Any> {
        val nim = auth.name
        val student = studentUseCase.getStudentByNim(nim)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Data mahasiswa tidak ditemukan")

        val summary: KhsSummaryResponse = khsUseCase.getKhsSummary(
            nim = nim,
            name = student.name,
            currentSemester = student.semester
        )
        return ResponseEntity.ok(summary)
    }

    @GetMapping("/entries")
    fun entries(auth: Authentication): ResponseEntity<List<KhsEntryDto>> =
        ResponseEntity.ok(khsUseCase.getKhsEntries(auth.name))

    @PostMapping("/entries")
    fun addEntry(@RequestBody request: AddKhsRequest, auth: Authentication): ResponseEntity<Any> {
        if (request.courseCode.isBlank()) {
            return ResponseEntity.badRequest().body("Kode mata kuliah wajib diisi")
        }
        if (request.semesterTaken !in 1..14) {
            return ResponseEntity.badRequest().body("Semester pengambilan tidak valid (1-14)")
        }

        val khs = Khs(
            nim = auth.name,
            courseCode = request.courseCode.trim(),
            classCode = request.classCode.trim(),
            grade = request.grade,
            semesterTaken = request.semesterTaken,
            academicYear = request.academicYear,
            semesterType = request.semesterType
        )

        return when (khsUseCase.addKhsEntry(khs)) {
            AddKhsOutcome.SUCCESS ->
                ResponseEntity.status(HttpStatus.CREATED).body("Mata kuliah ditambahkan ke KHS")
            AddKhsOutcome.COURSE_NOT_FOUND ->
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Mata kuliah '${request.courseCode}' tidak ditemukan")
            AddKhsOutcome.DUPLICATE ->
                ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Mata kuliah itu sudah tercatat di semester tersebut")
        }
    }

    @DeleteMapping("/entries/{courseCode}/{semester}")
    fun deleteEntry(
        @PathVariable courseCode: String,
        @PathVariable semester: Int,
        auth: Authentication
    ): ResponseEntity<String> {
        if (courseCode.isBlank() || semester !in 0..255) {
            return ResponseEntity.badRequest().body("Parameter tidak valid")
        }
        val deleted = khsUseCase.deleteKhsEntry(auth.name, courseCode, semester)
        return if (deleted) ResponseEntity.ok("Mata kuliah dihapus dari KHS")
        else ResponseEntity.status(HttpStatus.NOT_FOUND).body("Data tidak ditemukan")
    }
}
