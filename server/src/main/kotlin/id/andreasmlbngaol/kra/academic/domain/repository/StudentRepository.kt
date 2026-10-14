package id.andreasmlbngaol.kra.academic.domain.repository

import id.andreasmlbngaol.kra.academic.domain.model.Student

/** Read-only access to student records. */
interface StudentRepository {
    /** Returns the student with [nim], or `null` if not found. */
    fun getStudentByNim(nim: String): Student?
}
