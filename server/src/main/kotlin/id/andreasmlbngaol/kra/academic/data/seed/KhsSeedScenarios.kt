package id.andreasmlbngaol.kra.academic.data.seed

import id.andreasmlbngaol.kra.core.enums.Grade
import id.andreasmlbngaol.kra.core.enums.SemesterType

/**
 * Staged KHS scenarios for the demo users, ported verbatim from the old project's
 * `seed/khs/User{1..5}Khs.kt`.
 *
 * User1: grade template, NIM 221401001-221401009.
 * User2: real KHS (a friend's), NIM 221401011-221401018.
 * User3: grade template, NIM 231401001-231401006.
 * User4: real KHS template, NIM 221401021-221401028.
 * User5: Andreas, NIM 221401031-221401038.
 */

val user1Students = listOf(
    StagedStudent("221401001", "Ferry - Sem 1", 1),
    StagedStudent("221401002", "Ferry - Sem 2", 2),
    StagedStudent("221401003", "Ferry - Sem 3", 3),
    StagedStudent("221401004", "Ferry - Sem 4", 4),
    StagedStudent("221401005", "Ferry - Sem 5", 5),
    StagedStudent("221401006", "Ferry - Sem 6", 6),
    StagedStudent("221401007", "Ferry - Sem 7", 7),
    StagedStudent("221401008", "Ferry - Sem 8", 8),
    StagedStudent("221401009", "Ferry - Sem 9", 9),
)

private val user1GradesBySemester: Map<Pair<Int, SemesterType>, List<GradeEntry>> = mapOf(
    Pair(2022, SemesterType.GANJIL) to listOf(
        GradeEntry("USU1106", "B", Grade.A),
        GradeEntry("USU1108", "B", Grade.A),
        GradeEntry("ILK1105", "B", Grade.B),
        GradeEntry("ILK1106", "B", Grade.A),
        GradeEntry("ILK1108", "B", Grade.B),
        GradeEntry("ILK1109", "B", Grade.A),
        GradeEntry("ILK1110", "B", Grade.B_PLUS),
        GradeEntry("MTK1101", "B", Grade.A),
    ),
    Pair(2022, SemesterType.GENAP) to listOf(
        GradeEntry("USU1103", "A", Grade.A),
        GradeEntry("USU1109", "B", Grade.B_PLUS),
        GradeEntry("ILK1201", "B", Grade.B),
        GradeEntry("ILK1202", "B", Grade.A),
        GradeEntry("ILK1203", "B", Grade.A),
        GradeEntry("ILK1204", "B", Grade.A),
        GradeEntry("ILK1205", "B", Grade.A),
        GradeEntry("ILK1206", "B", Grade.A),
        GradeEntry("ILK1207", "B", Grade.B_PLUS),
    ),
    Pair(2023, SemesterType.GANJIL) to listOf(
        GradeEntry("MKWU31", "B", Grade.A),
        GradeEntry("MKWU32", "B", Grade.A),
        GradeEntry("ILK2104", "B", Grade.A),
        GradeEntry("ILK2105", "B", Grade.A),
        GradeEntry("ILK2106", "B", Grade.A),
        GradeEntry("ILK2107", "B", Grade.B_PLUS),
        GradeEntry("ILK2108", "B", Grade.A),
        GradeEntry("ILK2109", "B", Grade.B_PLUS),
        GradeEntry("ILK2110", "B", Grade.A),
        GradeEntry("ILK3102", "B", Grade.D),
        GradeEntry("ILK3107", "A", Grade.B),
    ),
    Pair(2023, SemesterType.GENAP) to listOf(
        GradeEntry("MKWU41", "B", Grade.A),
        GradeEntry("ILK2203", "B", Grade.A),
        GradeEntry("ILK2204", "B", Grade.C_PLUS),
        GradeEntry("ILK2207", "B", Grade.B_PLUS),
        GradeEntry("ILK2208", "B", Grade.C_PLUS),
        GradeEntry("ILK2209", "B", Grade.A),
        GradeEntry("ILK2210", "B", Grade.A),
        GradeEntry("ILK2211", "B", Grade.B_PLUS),
        GradeEntry("ILK3203", "B", Grade.A),
    ),
    Pair(2024, SemesterType.GANJIL) to listOf(
        GradeEntry("MKWU51", "B", Grade.A),
        GradeEntry("ILK3101", "B", Grade.A),
        GradeEntry("ILK3102", "B", Grade.D),
        GradeEntry("ILK3103", "B", Grade.A),
        GradeEntry("ILK3104", "B", Grade.A),
        GradeEntry("ILK3105", "B", Grade.A),
        GradeEntry("ILK3106", "B", Grade.A),
        GradeEntry("ILK3108", "A", Grade.B),
        GradeEntry("ILK4103", "B", Grade.A),
        GradeEntry("ILK4104", "B", Grade.B_PLUS),
    ),
    Pair(2024, SemesterType.GENAP) to listOf(
        GradeEntry("ILK3201", "B", Grade.A),
        GradeEntry("ILK3202", "B", Grade.B),
        GradeEntry("ILK3204", "B", Grade.B_PLUS),
        GradeEntry("ILK3205", "B", Grade.A),
        GradeEntry("ILK3206", "B", Grade.A),
        GradeEntry("ILK3207", "B", Grade.A),
        GradeEntry("ILK3208", "B", Grade.A),
        GradeEntry("ILK3211", "A", Grade.A),
    ),
    Pair(2025, SemesterType.GANJIL) to listOf(
        GradeEntry("ILK3102", "B", Grade.B),
        GradeEntry("ILK4101", "B", Grade.A),
        GradeEntry("ILK4105", "B", Grade.A),
        GradeEntry("ILK4106", "B", Grade.A),
        GradeEntry("ILK4107", "B", Grade.B_PLUS),
        GradeEntry("ILK4110", "A", Grade.A),
        GradeEntry("ILK4202", "A", Grade.A),
    ),
    Pair(2025, SemesterType.GENAP) to listOf(
        GradeEntry("ILK4299", "A", Grade.T),
    )
)

fun user1KhsData(): List<KhsData> = buildStagedKhs(user1Students, user1GradesBySemester)

val user2Students = listOf(
    StagedStudent("221401011", "Dean - Sem 1", 1),
    StagedStudent("221401012", "Dean - Sem 2", 2),
    StagedStudent("221401013", "Dean - Sem 3", 3),
    StagedStudent("221401014", "Dean - Sem 4", 4),
    StagedStudent("221401015", "Dean - Sem 5", 5),
    StagedStudent("221401016", "Dean - Sem 6", 6),
    StagedStudent("221401017", "Dean - Sem 7", 7),
    StagedStudent("221401018", "Dean - Sem 8", 8),
)

private val user2GradesBySemester: Map<Pair<Int, SemesterType>, List<GradeEntry>> = mapOf(
    Pair(2022, SemesterType.GANJIL) to listOf(
        GradeEntry("USU1106", "B", Grade.A),
        GradeEntry("ILK1109", "B", Grade.A),
        GradeEntry("USU1108", "B", Grade.A),
        GradeEntry("MTK1101", "B", Grade.B_PLUS),
        GradeEntry("ILK1110", "B", Grade.A),
        GradeEntry("ILK1105", "B", Grade.B),
        GradeEntry("ILK1106", "B", Grade.C_PLUS),
        GradeEntry("ILK1108", "B", Grade.C),
    ),
    Pair(2022, SemesterType.GENAP) to listOf(
        GradeEntry("ILK1203", "B", Grade.A),
        GradeEntry("ILK1202", "B", Grade.A),
        GradeEntry("ILK1201", "B", Grade.B_PLUS),
        GradeEntry("ILK1205", "B", Grade.A),
        GradeEntry("ILK1207", "B", Grade.A),
        GradeEntry("ILK1204", "B", Grade.A),
        GradeEntry("ILK1206", "B", Grade.A),
        GradeEntry("USU1103", "A", Grade.B_PLUS),
        GradeEntry("USU1109", "B", Grade.A),
    ),
    Pair(2023, SemesterType.GANJIL) to listOf(
        GradeEntry("ILK2106", "B", Grade.A),
        GradeEntry("ILK2108", "B", Grade.A),
        GradeEntry("ILK2109", "B", Grade.B_PLUS),
        GradeEntry("ILK2104", "B", Grade.A),
        GradeEntry("ILK3102", "B", Grade.B_PLUS),
        GradeEntry("ILK3107", "A", Grade.B),
        GradeEntry("ILK2110", "B", Grade.A),
        GradeEntry("ILK2105", "B", Grade.A),
        GradeEntry("ILK2107", "B", Grade.B_PLUS),
        GradeEntry("MKWU31", "B", Grade.A),
        GradeEntry("MKWU32", "B", Grade.A),
    ),
    Pair(2023, SemesterType.GENAP) to listOf(
        GradeEntry("ILK3203", "A", Grade.B_PLUS),
        GradeEntry("ILK2203", "B", Grade.A),
        GradeEntry("ILK2210", "B", Grade.A),
        GradeEntry("ILK2211", "B", Grade.B),
        GradeEntry("ILK2208", "B", Grade.C),
        GradeEntry("ILK2207", "B", Grade.B_PLUS),
        GradeEntry("ILK2209", "B", Grade.A),
        GradeEntry("ILK3204", "A", Grade.A),
        GradeEntry("ILK2204", "B", Grade.B),
        GradeEntry("MKWU41", "B", Grade.A),
    ),
    Pair(2024, SemesterType.GANJIL) to listOf(
        GradeEntry("ILK4103", "B", Grade.A),
        GradeEntry("ILK4104", "B", Grade.A),
        GradeEntry("ILK4101", "B", Grade.A),
        GradeEntry("ILK3101", "B", Grade.A),
        GradeEntry("ILK3105", "B", Grade.B_PLUS),
        GradeEntry("ILK3106", "B", Grade.A),
        GradeEntry("ILK3103", "B", Grade.A),
        GradeEntry("ILK3104", "B", Grade.A),
        GradeEntry("ILK3110", "A", Grade.A),
        GradeEntry("MKWU51", "B", Grade.A),
    ),
    Pair(2024, SemesterType.GENAP) to listOf(
        GradeEntry("ILK3207", "B", Grade.A),
        GradeEntry("ILK3208", "B", Grade.A),
        GradeEntry("ILK3201", "B", Grade.A),
        GradeEntry("ILK3202", "B", Grade.A),
        GradeEntry("ILK3205", "B", Grade.A),
        GradeEntry("ILK3206", "B", Grade.A),
        GradeEntry("ILK3209", "A", Grade.A),
    ),
    Pair(2025, SemesterType.GANJIL) to listOf(
        GradeEntry("ILK4107", "B", Grade.B_PLUS),
        GradeEntry("ILK4105", "B", Grade.A),
        GradeEntry("ILK4106", "B", Grade.A),
        GradeEntry("ILK4202", "A", Grade.A),
        GradeEntry("ILK4299", "A", Grade.T),
        GradeEntry("ILK4109", "A", Grade.A),
    ),
)

fun user2KhsData(): List<KhsData> = buildStagedKhs(user2Students, user2GradesBySemester)

val user3Students = listOf(
    StagedStudent("231401001", "Susan - Sem 1", 1),
    StagedStudent("231401002", "Susan - Sem 2", 2),
    StagedStudent("231401003", "Susan - Sem 3", 3),
    StagedStudent("231401004", "Susan - Sem 4", 4),
    StagedStudent("231401005", "Susan - Sem 5", 5),
    StagedStudent("231401006", "Susan - Sem 6", 6),
)

private val user3GradesBySemester: Map<Pair<Int, SemesterType>, List<GradeEntry>> = mapOf(
    Pair(2023, SemesterType.GANJIL) to listOf(
        GradeEntry("USU1106", "B", Grade.A),
        GradeEntry("USU1108", "B", Grade.A),
        GradeEntry("ILK1105", "B", Grade.A),
        GradeEntry("ILK1106", "B", Grade.A),
        GradeEntry("ILK1108", "B", Grade.A),
        GradeEntry("ILK1109", "B", Grade.A),
        GradeEntry("ILK1110", "B", Grade.A),
        GradeEntry("MTK1101", "B", Grade.A),
    ),
    Pair(2023, SemesterType.GENAP) to listOf(
        GradeEntry("USU1105", "A", Grade.A),
        GradeEntry("USU1109", "B", Grade.A),
        GradeEntry("ILK1201", "B", Grade.A),
        GradeEntry("ILK1202", "B", Grade.A),
        GradeEntry("ILK1203", "B", Grade.A),
        GradeEntry("ILK1204", "B", Grade.A),
        GradeEntry("ILK1205", "B", Grade.A),
        GradeEntry("ILK1206", "B", Grade.A),
        GradeEntry("ILK1207", "B", Grade.A),
    ),
    Pair(2024, SemesterType.GANJIL) to listOf(
        GradeEntry("ILK2104", "B", Grade.A),
        GradeEntry("ILK2105", "B", Grade.A),
        GradeEntry("ILK2106", "B", Grade.A),
        GradeEntry("ILK2107", "B", Grade.A),
        GradeEntry("ILK2108", "B", Grade.A),
        GradeEntry("ILK2109", "B", Grade.A),
        GradeEntry("ILK2110", "B", Grade.A),
        GradeEntry("MKWU31", "B", Grade.A),
        GradeEntry("MKWU32", "B", Grade.A),
    ),
    Pair(2024, SemesterType.GENAP) to listOf(
        GradeEntry("ILK2203", "B", Grade.A),
        GradeEntry("ILK2204", "B", Grade.A),
        GradeEntry("ILK2207", "B", Grade.A),
        GradeEntry("ILK2208", "B", Grade.A),
        GradeEntry("ILK2209", "B", Grade.A),
        GradeEntry("ILK2210", "B", Grade.A),
        GradeEntry("ILK2211", "B", Grade.B_PLUS),
        GradeEntry("MKWU41", "B", Grade.A),
    ),
    Pair(2025, SemesterType.GANJIL) to listOf(
        GradeEntry("ILK3101", "B", Grade.B),
        GradeEntry("ILK3102", "B", Grade.A),
        GradeEntry("ILK3103", "B", Grade.B),
        GradeEntry("ILK3104", "B", Grade.A),
        GradeEntry("ILK3105", "B", Grade.A),
        GradeEntry("ILK3106", "B", Grade.A),
        GradeEntry("ILK3107", "B", Grade.A),
        GradeEntry("ILK3109", "A", Grade.A),
        GradeEntry("MKWU51", "B", Grade.A),
    ),
)

fun user3KhsData(): List<KhsData> = buildStagedKhs(user3Students, user3GradesBySemester)

val user4Students = listOf(
    StagedStudent("221401021", "Elizabeth - Sem 1", 1),
    StagedStudent("221401022", "Elizabeth - Sem 2", 2),
    StagedStudent("221401023", "Elizabeth - Sem 3", 3),
    StagedStudent("221401024", "Elizabeth - Sem 4", 4),
    StagedStudent("221401025", "Elizabeth - Sem 5", 5),
    StagedStudent("221401026", "Elizabeth - Sem 6", 6),
    StagedStudent("221401027", "Elizabeth - Sem 7", 7),
    StagedStudent("221401028", "Elizabeth - Sem 8", 8),
)

private val user4GradesBySemester: Map<Pair<Int, SemesterType>, List<GradeEntry>> = mapOf(
    Pair(2022, SemesterType.GANJIL) to listOf(
        GradeEntry("USU1106", "B", Grade.B_PLUS),
        GradeEntry("ILK1109", "B", Grade.A),
        GradeEntry("USU1108", "B", Grade.A),
        GradeEntry("MTK1101", "B", Grade.B),
        GradeEntry("ILK1110", "B", Grade.B_PLUS),
        GradeEntry("ILK1105", "B", Grade.C),
        GradeEntry("ILK1106", "B", Grade.A),
        GradeEntry("ILK1108", "B", Grade.C_PLUS),
    ),
    Pair(2022, SemesterType.GENAP) to listOf(
        GradeEntry("ILK1203", "B", Grade.A),
        GradeEntry("ILK1202", "B", Grade.B_PLUS),
        GradeEntry("ILK1201", "B", Grade.B_PLUS),
        GradeEntry("ILK1205", "B", Grade.B_PLUS),
        GradeEntry("ILK1207", "B", Grade.B_PLUS),
        GradeEntry("ILK1204", "B", Grade.B_PLUS),
        GradeEntry("ILK1206", "B", Grade.B),
        GradeEntry("USU1101", "A", Grade.A),
        GradeEntry("USU1109", "B", Grade.A),
    ),
    Pair(2023, SemesterType.GANJIL) to listOf(
        GradeEntry("ILK2106", "B", Grade.A),
        GradeEntry("ILK2108", "B", Grade.A),
        GradeEntry("ILK2109", "B", Grade.B),
        GradeEntry("ILK2104", "B", Grade.A),
        GradeEntry("ILK3102", "B", Grade.B),
        GradeEntry("ILK3107", "B", Grade.B),
        GradeEntry("ILK2110", "B", Grade.A),
        GradeEntry("ILK2105", "B", Grade.A),
        GradeEntry("ILK2107", "B", Grade.B_PLUS),
        GradeEntry("MKWU001", "B", Grade.A),
        GradeEntry("MKWU002", "B", Grade.A),
    ),
    Pair(2023, SemesterType.GENAP) to listOf(
        GradeEntry("ILK3203", "B", Grade.B_PLUS),
        GradeEntry("ILK2203", "B", Grade.A),
        GradeEntry("ILK2210", "B", Grade.A),
        GradeEntry("ILK2211", "B", Grade.B_PLUS),
        GradeEntry("ILK2208", "B", Grade.D),
        GradeEntry("ILK2207", "B", Grade.B_PLUS),
        GradeEntry("ILK2209", "B", Grade.A),
        GradeEntry("ILK3204", "B", Grade.E),
        GradeEntry("ILK2204", "B", Grade.C_PLUS),
        GradeEntry("MKWU003", "B", Grade.A),
    ),
    Pair(2024, SemesterType.GANJIL) to listOf(
        GradeEntry("ILK4103", "B", Grade.A),
        GradeEntry("ILK4101", "B", Grade.A),
        GradeEntry("ILK4106", "B", Grade.A),
        GradeEntry("ILK3101", "B", Grade.A),
        GradeEntry("ILK3105", "B", Grade.B_PLUS),
        GradeEntry("ILK3106", "B", Grade.B_PLUS),
        GradeEntry("ILK3103", "B", Grade.A),
        GradeEntry("ILK3104", "B", Grade.B_PLUS),
        GradeEntry("ILK3109", "A", Grade.B_PLUS),
        GradeEntry("MKWU004", "B", Grade.A),
    ),
    Pair(2024, SemesterType.GENAP) to listOf(
        GradeEntry("ILK3207", "B", Grade.A),
        GradeEntry("ILK3208", "B", Grade.A),
        GradeEntry("ILK3204", "B", Grade.A),
        GradeEntry("ILK3201", "B", Grade.A),
        GradeEntry("ILK3202", "B", Grade.B_PLUS),
        GradeEntry("ILK3205", "B", Grade.A),
        GradeEntry("ILK3206", "B", Grade.A),
        GradeEntry("ILK3211", "A", Grade.B_PLUS),
        GradeEntry("ILK2208", "B", Grade.C_PLUS),
    ),
    Pair(2025, SemesterType.GANJIL) to listOf(
        GradeEntry("ILK4107", "B", Grade.B_PLUS),
        GradeEntry("ILK4105", "B", Grade.A),
        GradeEntry("ILK4104", "B", Grade.A),
        GradeEntry("ILK4202", "A", Grade.A),
        GradeEntry("ILK4299", "A", Grade.T),
        GradeEntry("ILK4109", "A", Grade.B_PLUS),
    ),
)

fun user4KhsData(): List<KhsData> = buildStagedKhs(user4Students, user4GradesBySemester)

val user5Students = listOf(
    StagedStudent("221401031", "Andreas - Sem 1", 1),
    StagedStudent("221401032", "Andreas - Sem 2", 2),
    StagedStudent("221401033", "Andreas - Sem 3", 3),
    StagedStudent("221401034", "Andreas - Sem 4", 4),
    StagedStudent("221401035", "Andreas - Sem 5", 5),
    StagedStudent("221401036", "Andreas - Sem 6", 6),
    StagedStudent("221401037", "Andreas - Sem 7", 7),
    StagedStudent("221401038", "Andreas - Sem 8", 8),
)

private val user5GradesBySemester: Map<Pair<Int, SemesterType>, List<GradeEntry>> = mapOf(
    Pair(2022, SemesterType.GANJIL) to listOf(
        GradeEntry("USU1106", "B", Grade.A),
        GradeEntry("USU1108", "B", Grade.A),
        GradeEntry("ILK1105", "B", Grade.A),
        GradeEntry("ILK1106", "B", Grade.A),
        GradeEntry("ILK1108", "B", Grade.A),
        GradeEntry("ILK1109", "B", Grade.A),
        GradeEntry("ILK1110", "B", Grade.A),
        GradeEntry("MTK1101", "B", Grade.A)
    ),
    Pair(2022, SemesterType.GENAP) to listOf(
        GradeEntry("USU1103", "A", Grade.A),
        GradeEntry("USU1109", "B", Grade.A),
        GradeEntry("ILK1201", "B", Grade.A),
        GradeEntry("ILK1202", "B", Grade.A),
        GradeEntry("ILK1203", "B", Grade.A),
        GradeEntry("ILK1204", "B", Grade.A),
        GradeEntry("ILK1205", "B", Grade.A),
        GradeEntry("ILK1206", "B", Grade.A),
        GradeEntry("ILK1207", "B", Grade.A),
    ),
    Pair(2023, SemesterType.GANJIL) to listOf(
        GradeEntry("MKWU31", "B", Grade.B_PLUS),
        GradeEntry("MKWU32", "B", Grade.A),
        GradeEntry("ILK2104", "B", Grade.A),
        GradeEntry("ILK2105", "B", Grade.A),
        GradeEntry("ILK2106", "B", Grade.A),
        GradeEntry("ILK2107", "B", Grade.A),
        GradeEntry("ILK2108", "B", Grade.A),
        GradeEntry("ILK2109", "B", Grade.B_PLUS),
        GradeEntry("ILK2110", "B", Grade.B_PLUS),
        GradeEntry("ILK3102", "B", Grade.A),
        GradeEntry("ILK3107", "A", Grade.B_PLUS),
    ),
    Pair(2023, SemesterType.GENAP) to listOf(
        GradeEntry("MKWU41", "B", Grade.E),
        GradeEntry("ILK2203", "B", Grade.E),
        GradeEntry("ILK2204", "B", Grade.E),
        GradeEntry("ILK2207", "B", Grade.D),
        GradeEntry("ILK2208", "B", Grade.E),
        GradeEntry("ILK2209", "B", Grade.E),
        GradeEntry("ILK2210", "B", Grade.E),
        GradeEntry("ILK2211", "B", Grade.E),
        GradeEntry("ILK3205", "B", Grade.E),
    ),
    Pair(2024, SemesterType.GENAP) to listOf(
        GradeEntry("MKWU41", "B", Grade.B_PLUS),
        GradeEntry("ILK2203", "B", Grade.A),
        GradeEntry("ILK2204", "B", Grade.A),
        GradeEntry("ILK2208", "B", Grade.A),
        GradeEntry("ILK2209", "B", Grade.A),
        GradeEntry("ILK2210", "B", Grade.B_PLUS),
        GradeEntry("ILK2211", "A", Grade.A),
    ),
    Pair(2025, SemesterType.GANJIL) to listOf(
        GradeEntry("MKWU51", "A", Grade.A),
        GradeEntry("ILK3101", "B", Grade.E),
        GradeEntry("ILK3103", "B", Grade.C),
        GradeEntry("ILK3104", "B", Grade.A),
        GradeEntry("ILK3105", "B", Grade.A),
        GradeEntry("ILK3106", "B", Grade.A),
        GradeEntry("ILK3109", "A", Grade.A),
        GradeEntry("ILK4101", "B", Grade.B_PLUS),
        GradeEntry("ILK4103", "B", Grade.A),
        GradeEntry("ILK4104", "B", Grade.A),
    ),
    Pair(2025, SemesterType.GENAP) to listOf(
        GradeEntry("ILK3201", "B", Grade.A),
        GradeEntry("ILK3202", "A", Grade.A),
        GradeEntry("ILK3203", "B", Grade.A),
        GradeEntry("ILK3204", "B", Grade.A),
        GradeEntry("ILK3205", "B", Grade.A),
        GradeEntry("ILK3206", "B", Grade.A),
        GradeEntry("ILK3207", "B", Grade.A),
        GradeEntry("ILK3208", "B", Grade.A),
        GradeEntry("ILK3210", "A", Grade.A),
    )
)

fun user5KhsData(): List<KhsData> = buildStagedKhs(user5Students, user5GradesBySemester)
