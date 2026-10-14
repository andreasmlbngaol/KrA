package id.andreasmlbngaol.kra.academic.domain.model

/** A student identified by NIM. */
data class Student(
    val nim: String,
    val name: String,
    val semester: Int
)
