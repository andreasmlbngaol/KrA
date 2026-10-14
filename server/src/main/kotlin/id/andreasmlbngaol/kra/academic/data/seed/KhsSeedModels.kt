package id.andreasmlbngaol.kra.academic.data.seed

import id.andreasmlbngaol.kra.core.enums.Grade
import id.andreasmlbngaol.kra.core.enums.SemesterType

/** One KHS row to be inserted (resolved to an offering at seed time). */
data class KhsData(
    val nim: String,
    val year: Int,
    val semesterType: SemesterType,
    val courseCode: String,
    val classCode: String,
    val grade: Grade
)

/** One course in the per-semester grade template. */
data class GradeEntry(val courseCode: String, val classCode: String, val grade: Grade)

/**
 * A staged-scenario student.
 * @param semester the student's CURRENT semester (KHS is filled up to semester - 1).
 */
data class StagedStudent(val nim: String, val name: String, val semester: Int)

/**
 * Builds all KHS rows for a set of staged-scenario students: each student takes grades from
 * [gradesBySemester] for semesters 1..(semester - 1).
 */
fun buildStagedKhs(
    students: List<StagedStudent>,
    gradesBySemester: Map<Pair<Int, SemesterType>, List<GradeEntry>>,
): List<KhsData> {
    val gradesAsList = gradesBySemester.toList()

    return students.flatMap { student ->
        (1 until student.semester).flatMap { sem ->
            val (academicTerm, grades) = gradesAsList[sem - 1]
            val (year, semesterType) = academicTerm

            grades.map { gradeEntry ->
                KhsData(
                    nim = student.nim,
                    year = year,
                    semesterType = semesterType,
                    courseCode = gradeEntry.courseCode,
                    classCode = gradeEntry.classCode,
                    grade = gradeEntry.grade
                )
            }
        }
    }
}
