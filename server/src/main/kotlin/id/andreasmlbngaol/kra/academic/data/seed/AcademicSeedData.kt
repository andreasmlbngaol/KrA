package id.andreasmlbngaol.kra.academic.data.seed

import id.andreasmlbngaol.kra.core.enums.CourseType
import id.andreasmlbngaol.kra.core.enums.Day
import id.andreasmlbngaol.kra.core.enums.LockType

/**
 * Static seed data for the Ilmu Komputer curriculum: lecturers, course catalog,
 * CSP constraints, prerequisites, curriculum packages, and class offerings.
 *
 * Ported verbatim from the old Ktor project's `seed/` package. Times are kept as-is,
 * including the original mixed `08:00` / `08.00` spellings.
 */

data class DosenSeed(val nama: String, val nidn: String)

val dosenSeedList = listOf(
    DosenSeed("Amer Sharif S.Si, M.Kom", "0121106902"),
    DosenSeed("Anandhini Medianty Nababan S. Kom., M. T", "0013049304"),
    DosenSeed("Desilia Silvida, S.Kom, M.Kom", "0005128906"),
    DosenSeed("Dr. Dewi Sartika Br Ginting, S.Kom, M.Kom", "0104059001"),
    DosenSeed("Dian Rachmawati, S.Si, M.Kom", "0023078303"),
    DosenSeed("Dr. Amalia, ST, MT", "0121127801"),
    DosenSeed("Dr. Eng. Ade Candra, ST, M.Kom", "0004097901"),
    DosenSeed("Dr. Fauzan Nurahmadi, S.Kom, M.Cs", "0029128506"),
    DosenSeed("Dr. Ir. Elviawaty Muisa Zamzami, ST, MT", "0016077001"),
    DosenSeed("Dr. M. Andri Budiman, ST,M.Comp.Sc,MEM", "0008107507"),
    DosenSeed("Dr. Maya Silvi Lydia B.Sc., M.Sc.", "0027017403"),
    DosenSeed("Dr. Niskarto Zendrato S.Kom., M.Kom", "0119098902"),
    DosenSeed("Dr. Pauzi Ibrahim Nainggolan S.Komp., M.Sc.", "0014098805"),
    DosenSeed("Prof. Drs. Poltak Sihombing M.Kom., Ph.D", "0017036205"),
    DosenSeed("Dr. Sri Melvani Hardi, S.Kom, M.Kom", "0101058801"),
    DosenSeed("Dr. Handrizal, S.Si, M.Comp,Sc", "0113067703"),
    DosenSeed("Dr. Jos Timanta Tarigan, S.Kom., M.Sc", "0126018502"),
    DosenSeed("Dr. T. Henny Febriana Harumy S.Kom, M.Kom", "0119028802"),
    DosenSeed("Dr. Fuzy Yustika Manik, S.Kom., M.Kom", "0115108703"),
    DosenSeed("Hayatunnufus, S.Kom, M.Cs", "0019079202"),
    DosenSeed("Herriyance, ST, M.Kom", "0024108007"),
    DosenSeed("Insidini Fawwaz M.Kom", "0000000001"),
    DosenSeed("Mauliana S.Kom., M.Sc", "0000000002"),
    DosenSeed("Nurrahmadayeni M.Kom", "0000000003"),
    DosenSeed("Prof. Dr. Syahril Efendi, S.Si, M.IT", "0010116706"),
    DosenSeed("Tito Afwi Senda S.T., M.Kom", "0000000004"),
    DosenSeed("Dosen Pengisi", "0000000000"),
)

data class CourseData(
    val code: String,
    val name: String,
    val sks: Int,
    val normalSemester: Int,
    val type: CourseType,
)

val courseSeedList = listOf(
    // Semester 1
    CourseData("USU1106", "Pendidikan Pancasila", 2, 1, CourseType.MKWK),
    CourseData("USU1108", "Bahasa Indonesia", 2, 1, CourseType.MKWK),
    CourseData("ILK1105", "Dasar Pemrograman", 3, 1, CourseType.REGULAR_MANDATORY),
    CourseData("ILK1106", "Praktikum Dasar Pemrograman", 1, 1, CourseType.REGULAR_MANDATORY),
    CourseData("ILK1108", "Organisasi Arsitektur Komputer", 3, 1, CourseType.REGULAR_MANDATORY),
    CourseData("ILK1109", "Pengantar Ilmu Komputer", 2, 1, CourseType.REGULAR_MANDATORY),
    CourseData("ILK1110", "Logika Matematika", 3, 1, CourseType.REGULAR_MANDATORY),
    CourseData("MTK1101", "Matematika Dasar", 3, 1, CourseType.REGULAR_MANDATORY),

    // Semester 2
    CourseData("USU1101", "Pendidikan Agama Islam", 2, 2, CourseType.MKWK),
    CourseData("USU1102", "Pendidikan Agama Katolik", 2, 2, CourseType.MKWK),
    CourseData("USU1103", "Pendidikan Agama Kristen", 2, 2, CourseType.MKWK),
    CourseData("USU1104", "Pendidikan Agama Hindu", 2, 2, CourseType.MKWK),
    CourseData("USU1105", "Pendidikan Agama Buddha", 2, 2, CourseType.MKWK),
    CourseData("USU1109", "Pendidikan Kewarganegaraan", 2, 2, CourseType.MKWK),
    CourseData("ILK1201", "Matematika Diskrit", 3, 2, CourseType.REGULAR_MANDATORY),
    CourseData("ILK1202", "Aljabar Linear", 3, 2, CourseType.REGULAR_MANDATORY),
    CourseData("ILK1203", "Algoritma dan Pemrograman", 3, 2, CourseType.REGULAR_MANDATORY),
    CourseData("ILK1204", "Praktikum Algoritma dan Pemrograman", 1, 2, CourseType.REGULAR_MANDATORY),
    CourseData("ILK1205", "Probabilitas dan Statistika", 3, 2, CourseType.REGULAR_MANDATORY),
    CourseData("ILK1206", "Praktikum Probabilitas dan Statistika", 1, 2, CourseType.REGULAR_MANDATORY),
    CourseData("ILK1207", "Sistem Operasi", 2, 2, CourseType.REGULAR_MANDATORY),

    // Semester 3
    CourseData("MKWU31", "MKWU Sem 3A", 2, 3, CourseType.MKWK),
    CourseData("MKWU32", "MKWU Sem 3B", 2, 3, CourseType.MKWK),
    CourseData("ILK2104", "Basis Data", 3, 3, CourseType.REGULAR_MANDATORY),
    CourseData("ILK2105", "Praktikum Basis Data", 1, 3, CourseType.REGULAR_MANDATORY),
    CourseData("ILK2106", "Pemrograman Web", 3, 3, CourseType.REGULAR_MANDATORY),
    CourseData("ILK2107", "Praktikum Pemrograman Web", 1, 3, CourseType.REGULAR_MANDATORY),
    CourseData("ILK2108", "Wirausaha Digital", 2, 3, CourseType.REGULAR_MANDATORY),
    CourseData("ILK2109", "Struktur Data", 3, 3, CourseType.REGULAR_MANDATORY),
    CourseData("ILK2110", "Praktikum Struktur Data", 1, 3, CourseType.REGULAR_MANDATORY),

    // Semester 4
    CourseData("MKWU41", "MKWU Sem 4A", 2, 4, CourseType.MKWK),
    CourseData("ILK2203", "Pemrograman Berorientasi Objek", 3, 4, CourseType.REGULAR_MANDATORY),
    CourseData("ILK2204", "Praktikum Pemrograman Berorientasi Objek", 1, 4, CourseType.REGULAR_MANDATORY),
    CourseData("ILK2207", "Desain dan Analisis Algoritma", 3, 4, CourseType.REGULAR_MANDATORY),
    CourseData("ILK2208", "Jaringan Komputer", 3, 4, CourseType.REGULAR_MANDATORY),
    CourseData("ILK2209", "Praktikum Jaringan Komputer", 1, 4, CourseType.REGULAR_MANDATORY),
    CourseData("ILK2210", "Keamanan Data dan Informasi", 3, 4, CourseType.REGULAR_MANDATORY),
    CourseData("ILK2211", "Analisis dan Desain Perangkat Lunak", 3, 4, CourseType.REGULAR_MANDATORY),

    // Semester 5
    CourseData("MKWU51", "MKWU Sem 5A", 2, 5, CourseType.MKWK),
    CourseData("ILK3108", "Analisis Jejaring Sosial", 3, 5, CourseType.REGULAR_ELECTIVE),
    CourseData("ILK3109", "Kriptografi", 3, 5, CourseType.REGULAR_ELECTIVE),
    CourseData("ILK3110", "Penjaminan Kualitas Perangkat Lunak", 3, 5, CourseType.REGULAR_ELECTIVE),
    CourseData("ILK3101", "Metodologi Penelitian", 2, 5, CourseType.REGULAR_MANDATORY),
    CourseData("ILK3102", "Etika Profesi", 2, 5, CourseType.REGULAR_MANDATORY),
    CourseData("ILK3103", "Grafika Komputer", 2, 5, CourseType.REGULAR_MANDATORY),
    CourseData("ILK3104", "Praktikum Grafika Komputer", 1, 5, CourseType.REGULAR_MANDATORY),
    CourseData("ILK3105", "Pemrograman Mobile", 3, 5, CourseType.REGULAR_MANDATORY),
    CourseData("ILK3106", "Praktikum Pemrograman Mobile", 1, 5, CourseType.REGULAR_MANDATORY),
    CourseData("ILK3107", "Kecerdasan Buatan", 3, 5, CourseType.REGULAR_MANDATORY),

    // Semester 6
    CourseData("ILK3209", "Natural Language Processing", 3, 6, CourseType.REGULAR_ELECTIVE),
    CourseData("ILK3210", "Forensik Digital", 3, 6, CourseType.REGULAR_ELECTIVE),
    CourseData("ILK3211", "Game Development", 3, 6, CourseType.REGULAR_ELECTIVE),
    CourseData("ILK3201", "Penambangan Data", 3, 6, CourseType.REGULAR_MANDATORY),
    CourseData("ILK3202", "Praktikum Penambangan Data", 1, 6, CourseType.REGULAR_MANDATORY),
    CourseData("ILK3203", "Pembelajaran Mesin", 3, 6, CourseType.REGULAR_MANDATORY),
    CourseData("ILK3204", "Praktikum Pembelajaran Mesin", 1, 6, CourseType.REGULAR_MANDATORY),
    CourseData("ILK3205", "Big Data", 3, 6, CourseType.REGULAR_MANDATORY),
    CourseData("ILK3206", "Internet of Things", 3, 6, CourseType.REGULAR_MANDATORY),
    CourseData("ILK3207", "Pengolahan Citra Digital", 2, 6, CourseType.REGULAR_MANDATORY),
    CourseData("ILK3208", "Praktikum Pengolahan Citra Digital", 1, 6, CourseType.REGULAR_MANDATORY),

    // Semester 7
    CourseData("ILK4110", "Ethical Hacking", 3, 7, CourseType.REGULAR_ELECTIVE),
    CourseData("ILK4108", "Sistem Media Interaktif", 3, 7, CourseType.REGULAR_ELECTIVE),
    CourseData("ILK4109", "Deep Learning", 3, 7, CourseType.REGULAR_ELECTIVE),
    CourseData("ILK4101", "Business Intelligence", 2, 7, CourseType.REGULAR_MANDATORY),
    CourseData("ILK4103", "Komputasi Paralel dan Terdistribusi", 3, 7, CourseType.REGULAR_MANDATORY),
    CourseData("ILK4104", "Cloud Computing", 3, 7, CourseType.REGULAR_MANDATORY),
    CourseData("ILK4105", "Computer Vision", 3, 7, CourseType.REGULAR_MANDATORY),
    CourseData("ILK4106", "Human Computer Interaction", 3, 7, CourseType.REGULAR_MANDATORY),
    CourseData("ILK4107", "Proyek Perangkat Lunak", 3, 7, CourseType.REGULAR_MANDATORY),
    CourseData("ILK4202", "Kerja Praktek/Magang", 3, 7, CourseType.REGULAR_MANDATORY),

    // Semester 8 (normalSemester = earliest semester it can be taken)
    CourseData("ILK4299", "Tugas Akhir", 6, 7, CourseType.REGULAR_MANDATORY),
)

data class Constraint(val courseCode: String, val minSks: Int)

val constraintSeedList = listOf(
    Constraint("ILK4299", 124), // Tugas Akhir
    Constraint("ILK4202", 120),
)

/** Course prerequisite pairs: course -> required prerequisite. */
val prerequisiteSeedList = listOf(
    Pair("ILK1202", "MTK1101"),
    Pair("ILK1203", "ILK1105"),
    Pair("ILK1204", "ILK1105"),
    Pair("ILK1205", "MTK1101"),
    Pair("ILK1206", "MTK1101"),
    Pair("ILK1207", "ILK1105"),
    Pair("ILK1207", "ILK1110"),
    Pair("ILK2109", "ILK1105"),
    Pair("ILK2109", "ILK1203"),
    Pair("ILK2110", "ILK1105"),
    Pair("ILK2110", "ILK1203"),
    Pair("ILK2104", "ILK1105"),
    Pair("ILK2104", "ILK1203"),
    Pair("ILK2105", "ILK1105"),
    Pair("ILK2105", "ILK1203"),
    Pair("ILK2106", "ILK1105"),
    Pair("ILK2106", "ILK1203"),
    Pair("ILK2107", "ILK1105"),
    Pair("ILK2107", "ILK1203"),
    Pair("ILK2208", "ILK1105"),
    Pair("ILK2208", "ILK1207"),
    Pair("ILK2209", "ILK1105"),
    Pair("ILK2209", "ILK1207"),
    Pair("ILK2203", "ILK1105"),
    Pair("ILK2203", "ILK1203"),
    Pair("ILK2204", "ILK1105"),
    Pair("ILK2204", "ILK1203"),
    Pair("ILK2207", "MTK1101"),
    Pair("ILK2207", "ILK1105"),
    Pair("ILK2207", "ILK1203"),
    Pair("ILK2211", "ILK1105"),
    Pair("ILK2211", "ILK1203"),
    Pair("ILK2211", "ILK2104"),
    Pair("ILK2210", "ILK1105"),
    Pair("ILK2210", "ILK1203"),
    Pair("ILK2210", "ILK1207"),
    Pair("ILK3107", "ILK1105"),
    Pair("ILK3107", "ILK1203"),
    Pair("ILK3107", "ILK1205"),
    Pair("ILK3107", "ILK1110"),
    Pair("ILK3107", "ILK1201"),
    Pair("ILK3107", "ILK1202"),
    Pair("ILK3103", "MTK1101"),
    Pair("ILK3103", "ILK1105"),
    Pair("ILK3103", "ILK1203"),
    Pair("ILK3104", "MTK1101"),
    Pair("ILK3104", "ILK1105"),
    Pair("ILK3104", "ILK1203"),
    Pair("ILK3105", "ILK1105"),
    Pair("ILK3105", "ILK1203"),
    Pair("ILK3105", "ILK2203"),
    Pair("ILK3105", "ILK2104"),
    Pair("ILK3106", "ILK1105"),
    Pair("ILK3106", "ILK1203"),
    Pair("ILK3106", "ILK2203"),
    Pair("ILK3106", "ILK2104"),
    Pair("ILK3108", "ILK1105"),
    Pair("ILK3108", "ILK1205"),
    Pair("ILK3108", "ILK1203"),
    Pair("ILK3108", "ILK2203"),
    Pair("ILK3108", "ILK2104"),
    Pair("ILK3110", "ILK1105"),
    Pair("ILK3110", "ILK1203"),
    Pair("ILK3110", "ILK2211"),
    Pair("ILK3109", "ILK1105"),
    Pair("ILK3109", "ILK1203"),
    Pair("ILK3109", "ILK2210"),
    Pair("ILK3201", "MTK1101"),
    Pair("ILK3201", "ILK1105"),
    Pair("ILK3201", "ILK1203"),
    Pair("ILK3201", "ILK2109"),
    Pair("ILK3201", "ILK2104"),
    Pair("ILK3201", "ILK1205"),
    Pair("ILK3202", "MTK1101"),
    Pair("ILK3202", "ILK1105"),
    Pair("ILK3202", "ILK1203"),
    Pair("ILK3202", "ILK2109"),
    Pair("ILK3202", "ILK2104"),
    Pair("ILK3202", "ILK1205"),
    Pair("ILK3203", "MTK1101"),
    Pair("ILK3203", "ILK1105"),
    Pair("ILK3203", "ILK1203"),
    Pair("ILK3203", "ILK1205"),
    Pair("ILK3203", "ILK3107"),
    Pair("ILK3204", "MTK1101"),
    Pair("ILK3204", "ILK1105"),
    Pair("ILK3204", "ILK1203"),
    Pair("ILK3204", "ILK1205"),
    Pair("ILK3204", "ILK3107"),
    Pair("ILK3205", "MTK1101"),
    Pair("ILK3205", "ILK1105"),
    Pair("ILK3205", "ILK1203"),
    Pair("ILK3205", "ILK1205"),
    Pair("ILK3205", "ILK2104"),
    Pair("ILK3206", "ILK1105"),
    Pair("ILK3206", "ILK2208"),
    Pair("ILK3206", "ILK1207"),
    Pair("ILK3206", "ILK1110"),
    Pair("ILK3206", "ILK1201"),
    Pair("ILK3206", "ILK2203"),
    Pair("ILK3207", "MTK1101"),
    Pair("ILK3207", "ILK1105"),
    Pair("ILK3207", "ILK1203"),
    Pair("ILK3207", "ILK1202"),
    Pair("ILK3207", "ILK3103"),
    Pair("ILK3208", "MTK1101"),
    Pair("ILK3208", "ILK1105"),
    Pair("ILK3208", "ILK1203"),
    Pair("ILK3208", "ILK1202"),
    Pair("ILK3208", "ILK3103"),
    Pair("ILK3209", "ILK1105"),
    Pair("ILK3209", "ILK1203"),
    Pair("ILK3209", "ILK1202"),
    Pair("ILK3209", "ILK3107"),
    Pair("ILK3210", "ILK1105"),
    Pair("ILK3210", "ILK1203"),
    Pair("ILK3210", "ILK2208"),
    Pair("ILK3210", "ILK2104"),
    Pair("ILK3211", "ILK1105"),
    Pair("ILK3211", "ILK1203"),
    Pair("ILK3211", "ILK3107"),
    Pair("ILK3211", "ILK3103"),
    Pair("ILK4101", "MTK1101"),
    Pair("ILK4101", "ILK1105"),
    Pair("ILK4101", "ILK1203"),
    Pair("ILK4101", "ILK2104"),
    Pair("ILK4103", "ILK1105"),
    Pair("ILK4103", "ILK1108"),
    Pair("ILK4103", "ILK1203"),
    Pair("ILK4103", "ILK2208"),
    Pair("ILK4104", "ILK1105"),
    Pair("ILK4104", "ILK1108"),
    Pair("ILK4104", "ILK1203"),
    Pair("ILK4104", "ILK2208"),
    Pair("ILK4105", "ILK1105"),
    Pair("ILK4105", "ILK1203"),
    Pair("ILK4105", "ILK1202"),
    Pair("ILK4105", "ILK3103"),
    Pair("ILK4105", "ILK3207"),
    Pair("ILK4105", "ILK3107"),
    Pair("ILK4106", "ILK1105"),
    Pair("ILK4106", "ILK2211"),
    Pair("ILK4106", "ILK2104"),
    Pair("ILK4106", "ILK1205"),
    Pair("ILK4107", "ILK1105"),
    Pair("ILK4107", "ILK1203"),
    Pair("ILK4107", "ILK2211"),
    Pair("ILK4107", "ILK2104"),
    Pair("ILK4107", "ILK2106"),
    Pair("ILK4107", "ILK3105"),
    Pair("ILK4107", "ILK2203"),
    Pair("ILK4108", "ILK1105"),
    Pair("ILK4108", "ILK1203"),
    Pair("ILK4108", "ILK2203"),
    Pair("ILK4108", "ILK2106"),
    Pair("ILK4108", "ILK2211"),
    Pair("ILK4109", "ILK3107"),
    Pair("ILK4109", "ILK3203"),
    Pair("ILK4110", "ILK1207"),
    Pair("ILK4110", "ILK1105"),
    Pair("ILK4110", "ILK3102"),
    Pair("ILK4110", "ILK2210"),
)

data class CurriculumPackageSeed(val semester: Int, val courseCode: String, val lockType: LockType)

val curriculumPackageSeedList = listOf(
    Triple(1, "USU1106", LockType.HARD),
    Triple(1, "USU1108", LockType.HARD),
    Triple(1, "ILK1105", LockType.HARD),
    Triple(1, "ILK1106", LockType.HARD),
    Triple(1, "ILK1108", LockType.HARD),
    Triple(1, "ILK1109", LockType.HARD),
    Triple(1, "ILK1110", LockType.HARD),
    Triple(1, "MTK1101", LockType.HARD),
    Triple(2, "USU1109", LockType.HARD),
    Triple(2, "ILK1201", LockType.HARD),
    Triple(2, "ILK1202", LockType.HARD),
    Triple(2, "ILK1203", LockType.HARD),
    Triple(2, "ILK1204", LockType.HARD),
    Triple(2, "ILK1205", LockType.HARD),
    Triple(2, "ILK1206", LockType.HARD),
    Triple(2, "ILK1207", LockType.HARD),
    Triple(3, "ILK2104", LockType.SOFT),
    Triple(3, "ILK2105", LockType.SOFT),
    Triple(3, "ILK2106", LockType.SOFT),
    Triple(3, "ILK2107", LockType.SOFT),
    Triple(3, "ILK2108", LockType.SOFT),
    Triple(3, "ILK2109", LockType.SOFT),
    Triple(3, "ILK2110", LockType.SOFT),
    Triple(4, "ILK2203", LockType.SOFT),
    Triple(4, "ILK2204", LockType.SOFT),
    Triple(4, "ILK2207", LockType.SOFT),
    Triple(4, "ILK2208", LockType.SOFT),
    Triple(4, "ILK2209", LockType.SOFT),
    Triple(4, "ILK2210", LockType.SOFT),
    Triple(4, "ILK2211", LockType.SOFT),
    Triple(5, "ILK3101", LockType.SOFT),
    Triple(5, "ILK3102", LockType.SOFT),
    Triple(5, "ILK3103", LockType.SOFT),
    Triple(5, "ILK3104", LockType.SOFT),
    Triple(5, "ILK3105", LockType.SOFT),
    Triple(5, "ILK3106", LockType.SOFT),
    Triple(5, "ILK3107", LockType.SOFT),
    Triple(6, "ILK3201", LockType.SOFT),
    Triple(6, "ILK3202", LockType.SOFT),
    Triple(6, "ILK3203", LockType.SOFT),
    Triple(6, "ILK3204", LockType.SOFT),
    Triple(6, "ILK3205", LockType.SOFT),
    Triple(6, "ILK3206", LockType.SOFT),
    Triple(6, "ILK3207", LockType.SOFT),
    Triple(6, "ILK3208", LockType.SOFT),
    Triple(7, "ILK4101", LockType.SOFT),
    Triple(7, "ILK4103", LockType.SOFT),
    Triple(7, "ILK4104", LockType.SOFT),
    Triple(7, "ILK4105", LockType.SOFT),
    Triple(7, "ILK4106", LockType.SOFT),
    Triple(7, "ILK4107", LockType.SOFT),
)

data class ClassData(
    val courseCode: String,
    val classCode: String,
    val day: Day?,
    val startTime: String?,
    val endTime: String?,
    val quota: Int,
)

/** Regular classes (A, B, C); schedules are null when the course has no fixed schedule. */
private fun regularClasses(
    code: String,
    hasSchedule: Boolean,
    dayA: Day? = null, startA: String? = null, endA: String? = null,
    dayB: Day? = null, startB: String? = null, endB: String? = null,
    dayC: Day? = null, startC: String? = null, endC: String? = null,
    quota: Int = 40
) = if (hasSchedule) listOf(
    ClassData(code, "A", dayA, startA, endA, quota),
    ClassData(code, "B", dayB, startB, endB, quota),
    ClassData(code, "C", dayC, startC, endC, quota),
) else listOf(
    ClassData(code, "A", null, null, null, quota),
    ClassData(code, "B", null, null, null, quota),
    ClassData(code, "C", null, null, null, quota),
)

private fun electiveClass(
    code: String,
    classCode: String = "A",
    day: Day? = null,
    startTime: String? = null,
    endTime: String? = null,
    quota: Int = 40
) = ClassData(code, classCode, day, startTime, endTime, quota)

val classSeedList: List<ClassData> = buildList {
    // Semester 1 - MKWU uses a special classCode
    addAll(listOf(
        ClassData("USU1106", "B", Day.MONDAY, "08:00", "09.40", 40),
        ClassData("USU1108", "B", Day.MONDAY, "10.30", "12.10", 40),
    ))
    addAll(regularClasses("ILK1105", true,
        Day.FRIDAY, "08:00", "10:30",
        Day.FRIDAY, "13.50", "16.20",
        Day.FRIDAY, "10.30", "13.00"))
    addAll(regularClasses("ILK1106", false))
    addAll(regularClasses("ILK1108", true,
        Day.THURSDAY, "13.50", "16.20",
        Day.THURSDAY, "08.00", "10.30",
        Day.THURSDAY, "10.30", "13.00"))
    addAll(regularClasses("ILK1109", true,
        Day.FRIDAY, "13.50", "15.30",
        Day.FRIDAY, "10:30", "12.10",
        Day.FRIDAY, "08.00", "09.40"))
    addAll(regularClasses("ILK1110", true,
        Day.WEDNESDAY, "10.30", "13.00",
        Day.WEDNESDAY, "13:50", "16.20",
        Day.WEDNESDAY, "08.00", "10.30"))
    addAll(regularClasses("MTK1101", true,
        Day.WEDNESDAY, "13:50", "16.20",
        Day.WEDNESDAY, "08.00", "10.30",
        Day.WEDNESDAY, "10.30", "13:00"))

    // Semester 2
    addAll(listOf(
        ClassData("USU1101", "A", Day.MONDAY, "08:00", "09.40", 40),
        ClassData("USU1102", "A", Day.MONDAY, "08:00", "09.40", 40),
        ClassData("USU1103", "A", Day.MONDAY, "08:00", "09.40", 40),
        ClassData("USU1104", "A", Day.MONDAY, "08:00", "09.40", 40),
        ClassData("USU1105", "A", Day.MONDAY, "08:00", "09.40", 40),
        ClassData("USU1109", "B", Day.MONDAY, "10:30", "12:10", 40),
    ))
    addAll(regularClasses("ILK1201", true,
        Day.WEDNESDAY, "08:00", "10:30",
        Day.THURSDAY, "10:30", "13:00",
        Day.WEDNESDAY, "11:20", "13:50"))
    addAll(regularClasses("ILK1202", true,
        Day.THURSDAY, "13:50", "16:20",
        Day.WEDNESDAY, "08:00", "10:30",
        Day.THURSDAY, "10:30", "13:00"))
    addAll(regularClasses("ILK1203", true,
        Day.THURSDAY, "10:30", "13:00",
        Day.FRIDAY, "13:50", "16:20",
        Day.WEDNESDAY, "13:50", "16:20"))
    addAll(regularClasses("ILK1204", false))
    addAll(regularClasses("ILK1205", true,
        Day.WEDNESDAY, "10:30", "13:00",
        Day.WEDNESDAY, "13:00", "15:30",
        Day.FRIDAY, "13:50", "16:20"))
    addAll(regularClasses("ILK1206", false))
    addAll(regularClasses("ILK1207", true,
        Day.THURSDAY, "08:00", "09:40",
        Day.FRIDAY, "10:30", "12:10",
        Day.FRIDAY, "08:50", "10:30"))

    // Semester 3
    addAll(regularClasses("ILK2104", true,
        Day.WEDNESDAY, "08:00", "10:30",
        Day.WEDNESDAY, "10:30", "13.00",
        Day.WEDNESDAY, "13:50", "16:20"))
    addAll(regularClasses("ILK2105", false))
    addAll(regularClasses("ILK2106", true,
        Day.THURSDAY, "08:00", "10:30",
        Day.THURSDAY, "10:30", "13.00",
        Day.THURSDAY, "13:50", "16.20"))
    addAll(regularClasses("ILK2107", false))
    addAll(regularClasses("ILK2108", true,
        Day.FRIDAY, "13.50", "15.30",
        Day.FRIDAY, "08.00", "09.40",
        Day.FRIDAY, "10.30", "12.10"))
    addAll(regularClasses("ILK2109", true,
        Day.THURSDAY, "13.50", "16.20",
        Day.THURSDAY, "08.00", "10.30",
        Day.THURSDAY, "10.30", "13.00"))
    addAll(regularClasses("ILK2110", false))

    // Semester 4
    addAll(regularClasses("ILK2203", true,
        Day.WEDNESDAY, "13:50", "16:20",
        Day.FRIDAY, "13:50", "16:20",
        Day.THURSDAY, "08:00", "10:30"))
    addAll(regularClasses("ILK2204", false))
    addAll(regularClasses("ILK2207", true,
        Day.THURSDAY, "13:50", "16:20",
        Day.FRIDAY, "08:00", "10:30",
        Day.FRIDAY, "13:50", "16:20"))
    addAll(regularClasses("ILK2208", true,
        Day.FRIDAY, "13:50", "16:20",
        Day.THURSDAY, "08:00", "10:30",
        Day.FRIDAY, "08:00", "10:30"))
    addAll(regularClasses("ILK2209", false))
    addAll(regularClasses("ILK2210", true,
        Day.FRIDAY, "08:00", "10:30",
        Day.THURSDAY, "13:50", "16:20",
        Day.WEDNESDAY, "13:50", "16:20"))
    addAll(regularClasses("ILK2211", true,
        Day.THURSDAY, "08:00", "10:30",
        Day.WEDNESDAY, "10:30", "13:00",
        Day.WEDNESDAY, "08:00", "10:30"))

    // Semester 5
    add(electiveClass("ILK3108", "A", Day.MONDAY, "08:00", "10.30"))
    add(electiveClass("ILK3109", "A", Day.MONDAY, "13.50", "16.20"))
    add(electiveClass("ILK3110", "A", Day.MONDAY, "13.50", "16.20"))
    addAll(regularClasses("ILK3101", true,
        Day.THURSDAY, "10.30", "12.10",
        Day.THURSDAY, "13.50", "15.30",
        Day.THURSDAY, "08.00", "09.40"))
    addAll(regularClasses("ILK3102", true,
        Day.FRIDAY, "10.30", "12.10",
        Day.FRIDAY, "13.50", "15.30",
        Day.FRIDAY, "08.00", "09.40"))
    addAll(regularClasses("ILK3103", true,
        Day.WEDNESDAY, "13.50", "15.30",
        Day.WEDNESDAY, "08.00", "09.40",
        Day.WEDNESDAY, "10.30", "12.10"))
    addAll(regularClasses("ILK3104", false))
    addAll(regularClasses("ILK3105", true,
        Day.FRIDAY, "08.00", "10.30",
        Day.FRIDAY, "10.30", "13.00",
        Day.FRIDAY, "13.50", "16.20"))
    addAll(regularClasses("ILK3106", false))
    addAll(regularClasses("ILK3107", true,
        Day.WEDNESDAY, "10.30", "13.00",
        Day.WEDNESDAY, "13.50", "16.20",
        Day.WEDNESDAY, "08.00", "10.30"))

    // Semester 6
    add(electiveClass("ILK3209", "A", Day.THURSDAY, "10:30", "13:00"))
    add(electiveClass("ILK3210", "A", Day.THURSDAY, "10:30", "13:00"))
    add(electiveClass("ILK3211", "A", Day.THURSDAY, "10:30", "13:00"))
    addAll(regularClasses("ILK3201", true,
        Day.THURSDAY, "08:00", "10:30",
        Day.WEDNESDAY, "08:00", "10:30",
        Day.WEDNESDAY, "13:50", "16:20"))
    addAll(regularClasses("ILK3202", false))
    addAll(regularClasses("ILK3203", true,
        Day.MONDAY, "10:30", "13:00",
        Day.MONDAY, "13:00", "15:30",
        Day.MONDAY, "08:00", "10:30"))
    addAll(regularClasses("ILK3204", false))
    addAll(regularClasses("ILK3205", true,
        Day.WEDNESDAY, "13:50", "16:20",
        Day.FRIDAY, "08:00", "10:30",
        Day.THURSDAY, "13:50", "16:20"))
    addAll(regularClasses("ILK3206", true,
        Day.FRIDAY, "08:00", "10:30",
        Day.THURSDAY, "13:50", "16:20",
        Day.WEDNESDAY, "10:30", "13:00"))
    addAll(regularClasses("ILK3207", true,
        Day.MONDAY, "08:50", "10:30",
        Day.THURSDAY, "08:00", "09:40",
        Day.MONDAY, "10:30", "12:10"))
    addAll(regularClasses("ILK3208", false))
    add(electiveClass("ILK4202", "A", null, null, null, 100))

    // Semester 7
    add(electiveClass("ILK4110", "A", Day.FRIDAY, "10.30", "13.00"))
    add(electiveClass("ILK4108", "A", Day.FRIDAY, "13.50", "16.20"))
    add(electiveClass("ILK4109", "A", Day.FRIDAY, "08:00", "10.30"))
    addAll(regularClasses("ILK4101", true,
        Day.THURSDAY, "13.50", "15.30",
        Day.THURSDAY, "08.00", "09.40",
        Day.THURSDAY, "10.30", "12.10"))
    addAll(regularClasses("ILK4103", true,
        Day.WEDNESDAY, "08.00", "10.30",
        Day.WEDNESDAY, "10.30", "13.00",
        Day.WEDNESDAY, "13.50", "16.20"))
    addAll(regularClasses("ILK4104", true,
        Day.THURSDAY, "08.00", "10.30",
        Day.THURSDAY, "10.30", "13.00",
        Day.THURSDAY, "13.50", "16.20"))
    addAll(regularClasses("ILK4105", true,
        Day.MONDAY, "10:30", "13.00",
        Day.MONDAY, "13.50", "16.20",
        Day.MONDAY, "08.00", "10.30"))
    addAll(regularClasses("ILK4106", true,
        Day.MONDAY, "13.50", "16.20",
        Day.MONDAY, "08.00", "10.30",
        Day.MONDAY, "10.30", "13.00"))
    addAll(regularClasses("ILK4107", true,
        Day.MONDAY, "08.00", "10.30",
        Day.MONDAY, "10.30", "13.00",
        Day.MONDAY, "13.50", "16.20"))

    // Semester 8
    add(electiveClass("ILK4299", "A", null, null, null, 100))
}

/**
 * Lecturers per class: (courseCode to classCode) -> list of lecturer NIDNs.
 * Source: the even-semester 2025/2026 schedule. Practical courses share their theory
 * course's lecturers; MKWK/MTK use "Dosen Pengisi"; ILK4202 uses every lecturer
 * except Niskarto and Dosen Pengisi.
 */
val classLecturers: Map<Pair<String, String>, List<String>> = mapOf(
    ("ILK1108" to "A") to listOf("0121106902", "0113067703"),
    ("ILK1108" to "B") to listOf("0121106902", "0113067703"),
    ("ILK1108" to "C") to listOf("0121106902", "0113067703"),
    ("ILK1109" to "A") to listOf("0000000002", "0024108007"),
    ("ILK1109" to "B") to listOf("0000000002", "0024108007"),
    ("ILK1109" to "C") to listOf("0000000002", "0024108007"),
    ("ILK1110" to "A") to listOf("0023078303", "0005128906"),
    ("ILK1110" to "B") to listOf("0023078303", "0005128906"),
    ("ILK1110" to "C") to listOf("0023078303", "0005128906"),
    ("MTK1101" to "A") to listOf("0008107507", "0010116706"),
    ("MTK1101" to "B") to listOf("0008107507", "0010116706"),
    ("MTK1101" to "C") to listOf("0008107507", "0010116706"),
    ("ILK1106" to "A") to listOf("0115108703", "0023078303"),
    ("ILK1106" to "B") to listOf("0115108703", "0023078303"),
    ("ILK1106" to "C") to listOf("0115108703", "0023078303"),
    ("USU1106" to "A") to listOf("0000000000"),
    ("USU1106" to "B") to listOf("0000000000"),
    ("USU1106" to "C") to listOf("0000000000"),
    ("USU1108" to "A") to listOf("0000000000"),
    ("USU1108" to "B") to listOf("0000000000"),
    ("USU1108" to "C") to listOf("0000000000"),

    ("USU1101" to "A") to listOf("0000000000"),
    ("USU1101" to "B") to listOf("0000000000"),
    ("USU1101" to "C") to listOf("0000000000"),
    ("USU1102" to "A") to listOf("0000000000"),
    ("USU1102" to "B") to listOf("0000000000"),
    ("USU1102" to "C") to listOf("0000000000"),
    ("USU1103" to "A") to listOf("0000000000"),
    ("USU1103" to "B") to listOf("0000000000"),
    ("USU1103" to "C") to listOf("0000000000"),
    ("USU1104" to "A") to listOf("0000000000"),
    ("USU1104" to "B") to listOf("0000000000"),
    ("USU1104" to "C") to listOf("0000000000"),
    ("USU1105" to "A") to listOf("0000000000"),
    ("USU1105" to "B") to listOf("0000000000"),
    ("USU1105" to "C") to listOf("0000000000"),
    ("USU1109" to "A") to listOf("0000000000"),
    ("USU1109" to "B") to listOf("0000000000"),
    ("USU1109" to "C") to listOf("0000000000"),

    ("ILK1201" to "A") to listOf("0115108703", "0010116706"),
    ("ILK1201" to "B") to listOf("0115108703", "0005128906"),
    ("ILK1201" to "C") to listOf("0115108703", "0000000004"),
    ("ILK1202" to "A") to listOf("0005128906", "0000000001"),
    ("ILK1202" to "B") to listOf("0005128906", "0000000001"),
    ("ILK1202" to "C") to listOf("0005128906", "0000000001"),
    ("ILK1203" to "A") to listOf("0023078303", "0000000003"),
    ("ILK1203" to "B") to listOf("0023078303", "0000000003"),
    ("ILK1203" to "C") to listOf("0023078303", "0000000003"),
    ("ILK1204" to "A") to listOf("0023078303", "0000000003"),
    ("ILK1204" to "B") to listOf("0023078303", "0000000003"),
    ("ILK1204" to "C") to listOf("0023078303", "0000000003"),
    ("ILK1205" to "A") to listOf("0119028802", "0104059001"),
    ("ILK1205" to "B") to listOf("0119028802", "0115108703"),
    ("ILK1205" to "C") to listOf("0010116706", "0115108703"),
    ("ILK1206" to "A") to listOf("0119028802", "0104059001"),
    ("ILK1206" to "B") to listOf("0119028802", "0115108703"),
    ("ILK1206" to "C") to listOf("0010116706", "0115108703"),
    ("ILK1207" to "A") to listOf("0029128506", "0017036205"),
    ("ILK1207" to "B") to listOf("0029128506", "0024108007"),
    ("ILK1207" to "C") to listOf("0029128506", "0024108007"),

    ("MKWU31" to "A") to listOf("0000000000"),
    ("MKWU31" to "B") to listOf("0000000000"),
    ("MKWU31" to "C") to listOf("0000000000"),
    ("MKWU32" to "A") to listOf("0000000000"),
    ("MKWU32" to "B") to listOf("0000000000"),
    ("MKWU32" to "C") to listOf("0000000000"),
    ("ILK2104" to "A") to listOf("0000000001", "0104059001"),
    ("ILK2104" to "B") to listOf("0000000001", "0104059001"),
    ("ILK2104" to "C") to listOf("0000000001", "0104059001"),
    ("ILK2105" to "A") to listOf("0000000001", "0104059001"),
    ("ILK2105" to "B") to listOf("0000000001", "0104059001"),
    ("ILK2105" to "C") to listOf("0000000001", "0104059001"),
    ("ILK2106" to "A") to listOf("0104059001", "0000000003"),
    ("ILK2106" to "B") to listOf("0104059001", "0000000003"),
    ("ILK2106" to "C") to listOf("0000000004", "0000000003"),
    ("ILK2107" to "A") to listOf("0104059001", "0000000003"),
    ("ILK2107" to "B") to listOf("0104059001", "0000000003"),
    ("ILK2107" to "C") to listOf("0000000004", "0000000003"),
    ("ILK2108" to "A") to listOf("0119028802", "0029128506"),
    ("ILK2108" to "B") to listOf("0119028802", "0029128506"),
    ("ILK2108" to "C") to listOf("0119028802", "0029128506"),
    ("ILK2109" to "A") to listOf("0000000001", "0013049304"),
    ("ILK2109" to "B") to listOf("0000000001", "0013049304"),
    ("ILK2109" to "C") to listOf("0000000001", "0013049304"),
    ("ILK2110" to "A") to listOf("0000000001", "0013049304"),
    ("ILK2110" to "B") to listOf("0000000001", "0013049304"),
    ("ILK2110" to "C") to listOf("0000000001", "0013049304"),

    ("MKWU41" to "A") to listOf("0000000000"),
    ("MKWU41" to "B") to listOf("0000000000"),
    ("MKWU41" to "C") to listOf("0000000000"),
    ("ILK2203" to "A") to listOf("0000000003", "0126018502"),
    ("ILK2203" to "B") to listOf("0019079202", "0013049304"),
    ("ILK2203" to "C") to listOf("0019079202", "0013049304"),
    ("ILK2204" to "A") to listOf("0000000003", "0126018502"),
    ("ILK2204" to "B") to listOf("0019079202", "0013049304"),
    ("ILK2204" to "C") to listOf("0019079202", "0013049304"),
    ("ILK2207" to "A") to listOf("0008107507", "0000000002"),
    ("ILK2207" to "B") to listOf("0008107507", "0000000002"),
    ("ILK2207" to "C") to listOf("0008107507", "0000000002"),
    ("ILK2208" to "A") to listOf("0121106902", "0029128506"),
    ("ILK2208" to "B") to listOf("0113067703", "0029128506"),
    ("ILK2208" to "C") to listOf("0121106902", "0113067703"),
    ("ILK2209" to "A") to listOf("0121106902", "0029128506"),
    ("ILK2209" to "B") to listOf("0113067703", "0029128506"),
    ("ILK2209" to "C") to listOf("0121106902", "0113067703"),
    ("ILK2210" to "A") to listOf("0113067703", "0121106902"),
    ("ILK2210" to "B") to listOf("0113067703", "0121106902"),
    ("ILK2210" to "C") to listOf("0113067703", "0121106902"),
    ("ILK2211" to "A") to listOf("0016077001", "0101058801"),
    ("ILK2211" to "B") to listOf("0016077001", "0013049304"),
    ("ILK2211" to "C") to listOf("0016077001", "0013049304"),

    ("MKWU51" to "A") to listOf("0000000000"),
    ("MKWU51" to "B") to listOf("0000000000"),
    ("MKWU51" to "C") to listOf("0000000000"),
    ("ILK3101" to "A") to listOf("0027017403", "0004097901"),
    ("ILK3101" to "B") to listOf("0027017403", "0004097901"),
    ("ILK3101" to "C") to listOf("0027017403", "0004097901"),
    ("ILK3102" to "A") to listOf("0004097901", "0016077001"),
    ("ILK3102" to "B") to listOf("0004097901", "0016077001"),
    ("ILK3102" to "C") to listOf("0004097901", "0016077001"),
    ("ILK3103" to "A") to listOf("0126018502", "0024108007"),
    ("ILK3103" to "B") to listOf("0126018502", "0024108007"),
    ("ILK3103" to "C") to listOf("0126018502", "0024108007"),
    ("ILK3104" to "A") to listOf("0126018502", "0024108007"),
    ("ILK3104" to "B") to listOf("0126018502", "0024108007"),
    ("ILK3104" to "C") to listOf("0126018502", "0024108007"),
    ("ILK3105" to "A") to listOf("0019079202", "0000000003"),
    ("ILK3105" to "B") to listOf("0019079202", "0000000003"),
    ("ILK3105" to "C") to listOf("0019079202", "0000000003"),
    ("ILK3106" to "A") to listOf("0019079202", "0000000003"),
    ("ILK3106" to "B") to listOf("0019079202", "0000000003"),
    ("ILK3106" to "C") to listOf("0019079202", "0000000003"),
    ("ILK3107" to "A") to listOf("0121127801", "0014098805"),
    ("ILK3107" to "B") to listOf("0121127801", "0014098805"),
    ("ILK3107" to "C") to listOf("0121127801", "0017036205"),
    ("ILK3108" to "A") to listOf("0027017403", "0000000002"),
    ("ILK3109" to "A") to listOf("0008107507", "0023078303"),
    ("ILK3110" to "A") to listOf("0013049304", "0029128506"),

    ("ILK3201" to "A") to listOf("0104059001", "0000000004"),
    ("ILK3201" to "B") to listOf("0104059001", "0000000004"),
    ("ILK3201" to "C") to listOf("0119028802", "0104059001"),
    ("ILK3202" to "A") to listOf("0104059001", "0000000004"),
    ("ILK3202" to "B") to listOf("0104059001", "0000000004"),
    ("ILK3202" to "C") to listOf("0119028802", "0104059001"),
    ("ILK3203" to "A") to listOf("0121127801", "0004097901"),
    ("ILK3203" to "B") to listOf("0121127801", "0000000001"),
    ("ILK3203" to "C") to listOf("0004097901", "0000000004"),
    ("ILK3204" to "A") to listOf("0121127801", "0004097901"),
    ("ILK3204" to "B") to listOf("0121127801", "0000000001"),
    ("ILK3204" to "C") to listOf("0004097901", "0000000004"),
    ("ILK3205" to "A") to listOf("0101058801", "0027017403"),
    ("ILK3205" to "B") to listOf("0101058801", "0027017403"),
    ("ILK3205" to "C") to listOf("0101058801", "0027017403"),
    ("ILK3206" to "A") to listOf("0019079202", "0017036205"),
    ("ILK3206" to "B") to listOf("0019079202", "0119098902"),
    ("ILK3206" to "C") to listOf("0019079202", "0119098902"),
    ("ILK3207" to "A") to listOf("0014098805", "0027017403"),
    ("ILK3207" to "B") to listOf("0014098805", "0024108007"),
    ("ILK3207" to "C") to listOf("0014098805", "0024108007"),
    ("ILK3208" to "A") to listOf("0014098805", "0027017403"),
    ("ILK3208" to "B") to listOf("0014098805", "0024108007"),
    ("ILK3208" to "C") to listOf("0014098805", "0024108007"),
    ("ILK3209" to "A") to listOf("0121127801", "0000000002"),
    ("ILK3210" to "A") to listOf("0014098805"),
    ("ILK3211" to "A") to listOf("0126018502"),

    ("ILK4101" to "A") to listOf("0101058801", "0000000002"),
    ("ILK4101" to "B") to listOf("0101058801", "0000000002"),
    ("ILK4101" to "C") to listOf("0101058801", "0000000002"),
    ("ILK4103" to "A") to listOf("0113067703", "0121106902"),
    ("ILK4103" to "B") to listOf("0113067703", "0121106902"),
    ("ILK4103" to "C") to listOf("0113067703", "0121106902"),
    ("ILK4104" to "A") to listOf("0019079202", "0017036205"),
    ("ILK4104" to "B") to listOf("0019079202", "0017036205"),
    ("ILK4104" to "C") to listOf("0019079202", "0017036205"),
    ("ILK4105" to "A") to listOf("0014098805", "0115108703"),
    ("ILK4105" to "B") to listOf("0014098805", "0115108703"),
    ("ILK4105" to "C") to listOf("0014098805", "0115108703"),
    ("ILK4106" to "A") to listOf("0101058801", "0005128906"),
    ("ILK4106" to "B") to listOf("0101058801", "0005128906"),
    ("ILK4106" to "C") to listOf("0101058801", "0005128906"),
    ("ILK4107" to "A") to listOf("0119028802", "0016077001"),
    ("ILK4107" to "B") to listOf("0119028802", "0016077001"),
    ("ILK4107" to "C") to listOf("0119028802", "0016077001"),
    ("ILK4108" to "A") to listOf("0126018502", "0013049304"),
    ("ILK4109" to "A") to listOf("0121127801", "0000000004"),
    ("ILK4110" to "A") to listOf("0000000004"),

    ("ILK4202" to "A") to listOf(
        "0121106902", "0013049304", "0005128906", "0104059001", "0023078303",
        "0121127801", "0004097901", "0029128506", "0016077001", "0008107507",
        "0027017403", "0014098805", "0017036205", "0101058801", "0113067703",
        "0126018502", "0119028802", "0115108703", "0019079202", "0024108007",
        "0000000001", "0000000002", "0000000003", "0010116706", "0000000004",
    ),
)
