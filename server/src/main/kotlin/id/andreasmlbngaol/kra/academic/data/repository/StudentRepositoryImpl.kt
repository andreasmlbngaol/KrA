package id.andreasmlbngaol.kra.academic.data.repository

import id.andreasmlbngaol.kra.academic.data.repository.jpa.StudentJpaRepository
import id.andreasmlbngaol.kra.academic.domain.model.Student
import id.andreasmlbngaol.kra.academic.domain.repository.StudentRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional(readOnly = true)
class StudentRepositoryImpl(
    private val students: StudentJpaRepository
) : StudentRepository {
    override fun getStudentByNim(nim: String): Student? =
        students.findById(nim).map { Student(it.nim, it.name, it.semester) }.orElse(null)
}
