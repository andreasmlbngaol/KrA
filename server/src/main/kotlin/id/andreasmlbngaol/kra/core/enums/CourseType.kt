package id.andreasmlbngaol.kra.core.enums

import kotlinx.serialization.Serializable

/** Category of a course: university/faculty mandatory (MKWU/MKWK) vs regular mandatory/elective. */
@Serializable
enum class CourseType {
    MKWK,
    MKWU,
    REGULAR_MANDATORY,
    REGULAR_ELECTIVE
}

/** The course's "Jenis" (kind) label shown in the FE (similar to the SATU MAHASISWA portal). */
fun CourseType.jenisLabel(): String = when (this) {
    CourseType.MKWU -> "MKWU"
    CourseType.MKWK -> "MKWK"
    CourseType.REGULAR_MANDATORY, CourseType.REGULAR_ELECTIVE -> "Reguler"
}
