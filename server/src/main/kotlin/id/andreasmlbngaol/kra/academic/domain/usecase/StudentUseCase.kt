package id.andreasmlbngaol.kra.academic.domain.usecase

import id.andreasmlbngaol.kra.academic.domain.model.Student
import id.andreasmlbngaol.kra.academic.domain.repository.StudentRepository
import org.springframework.stereotype.Service

/** Read-side use case for student records. */
@Service
class StudentUseCase(
    private val studentRepository: StudentRepository
) {
    /** Looks up a student by NIM. */
    fun getStudentByNim(nim: String): Student? = studentRepository.getStudentByNim(nim)
}
