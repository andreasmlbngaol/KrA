package id.andreasmlbngaol.kra.academic.domain.model

import id.andreasmlbngaol.kra.core.enums.Day

/** Two classes that overlap on the same day. */
data class ScheduleConflict(
    val classA: String,
    val classB: String,
    val day: Day
)

/** A course in the draft whose prerequisites are not yet satisfied. */
data class PrerequisiteViolation(
    val courseCode: String,
    val missingPrerequisites: List<String>
)

/** A course selected more than once (e.g. the same code across different KOM). */
data class DuplicateCourse(
    val courseCode: String,
    val courseName: String,
    val classCodes: List<String>
)

/** One option within a mandatory elective group. */
data class ElectiveOption(
    val code: String,
    val name: String,
    val sks: Int
)

/** The "choose N electives" requirement for the semester and how many are selected. */
data class ElectiveRequirement(
    val requiredChoose: Int,
    val selectedCount: Int,
    val options: List<ElectiveOption>
) {
    /** Whether enough electives have been selected. */
    val satisfied: Boolean get() = selectedCount >= requiredChoose
}

/**
 * Aggregate result of validating a KRS draft: totals and quota plus any blocking issues.
 */
data class KrsValidationResult(
    val totalSks: Int,
    val maxSks: Int,
    val mkwuReservedSks: Int,
    val remainingSks: Int,
    val scheduleConflicts: List<ScheduleConflict>,
    val prerequisiteViolations: List<PrerequisiteViolation>,
    val duplicateCourses: List<DuplicateCourse>,
    val electiveRequirement: ElectiveRequirement
)
