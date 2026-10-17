package id.andreasmlbngaol.kra.academic.domain.model.csp

import id.andreasmlbngaol.kra.academic.domain.model.ClassOffering
import id.andreasmlbngaol.kra.academic.domain.model.Course

/**
 * Immutable description of one KRS problem instance.
 *
 * The instance is produced by the use case (course-level filtering + domain construction)
 * and consumed by the solver (search). The solver never touches the database.
 */
data class CspProblem(
    val request: KrsCspRequest,
    val variables: List<CspVariable>,
    val maxSks: Int,
    val mkwuReservedSks: Int,
    val effectiveTargetSks: Int,
    val requiredElectiveCount: Int,
    val passedCourseCodes: Set<String>,
    val catalog: List<Course>,
    /** Courses that belong to this semester's "choose exactly N" elective group. */
    val electiveCourseCodes: Set<String> = emptySet(),
    /** Required courses removed during preprocessing, with the reason. */
    val rejectedCourses: List<RejectedCourse> = emptyList(),
    /** Courses whose best grade is D; repeating them reduces the remaining D load. */
    val dRepeatCourseCodes: Set<String> = emptySet(),
    /** Total SKS whose best grade is D. */
    val dSks: Int = 0,
    /** SKS of D that must be repeated so at most 8 SKS of D remain. */
    val dRepeatMinSks: Int = 0,
    /** Number of constraint families actually instantiated (for the evaluation log). */
    val activeConstraintCount: Int = 0
) {
    /** SKS budget available for the courses inside the draft (MKWU already set aside). */
    val courseBudgetSks: Int get() = (effectiveTargetSks - mkwuReservedSks).coerceAtLeast(0)

    val domainValueCount: Int get() = variables.sumOf { it.domain.size }
}

/** One variable of the CSP: a course and the classes it may be placed in. */
data class CspVariable(
    val course: Course,
    val role: CourseRole,
    val domain: List<ClassOffering>,
    /**
     * Only the student's own-semester mandatory courses are pinned to their KOM.
     * Electives, repeat courses and upper-semester courses may be taken in another
     * KOM when that is the only class offered (a merged/joint class).
     */
    val komLocked: Boolean = true,
    /**
     * `1 + number of still-unpassed mandatory courses that transitively require this one`.
     * The solver maximises the sum of this over the required courses it places, so a course
     * that blocks four later courses is scheduled before one that blocks none.
     */
    val importance: Int = 1
)

/** A required course that preprocessing had to drop (unary constraint failed). */
data class RejectedCourse(
    val course: Course,
    val code: CspDiagnosticCode,
    val constraint: CspConstraintType,
    val detail: String
)

/** One course placed in one class. */
data class CspAssignment(
    val course: Course,
    val offering: ClassOffering,
    /** true when the chosen class differs from the student's KOM. */
    val komSwitched: Boolean
)

/** Structured explanation of why something could not be placed. */
data class CspDiagnostic(
    val code: CspDiagnosticCode,
    val constraint: CspConstraintType,
    val courseCodes: List<String>,
    val detail: String
)

/** A concrete change that would restore feasibility. */
data class CspRelaxation(
    val action: CspRelaxationAction,
    val courseCodes: List<String>,
    val gainSks: Int,
    val detail: String
)

/** A compact alternative solution shown to the student. */
data class CspAlternative(
    val label: String,
    val totalSks: Int,
    val deltaFromTarget: Int,
    val items: List<CspAlternativeItem>
)

data class CspAlternativeItem(
    val courseCode: String,
    val classCode: String
)

/** Search statistics — the empirical evidence for the efficiency claims. */
data class CspSearchStats(
    val nodes: Int,
    val backtracks: Int,
    val ac3Revisions: Int,
    val forwardChecks: Int,
    val durationMs: Long,
    val solutionsFound: Int
) {
    companion object {
        val EMPTY = CspSearchStats(0, 0, 0, 0, 0L, 0)
    }
}

/** Full result of one solve. */
data class CspSolution(
    val status: CspStatus,
    val objective: CspObjective,
    val assignments: List<CspAssignment>,
    val totalSks: Int,
    val maxSks: Int,
    val mkwuReservedSks: Int,
    val effectiveTargetSks: Int,
    val remainingSks: Int,
    val unassignedRequired: List<Course>,
    val diagnostics: List<CspDiagnostic>,
    val relaxations: List<CspRelaxation>,
    val alternatives: List<CspAlternative>,
    val stats: CspSearchStats,
    /** Total SKS whose best grade is D. */
    val dSks: Int = 0,
    /** SKS of D that must be repeated so at most 8 SKS of D remain. */
    val dRepeatMinSks: Int = 0,
    /** SKS of D actually repeated in this draft. */
    val dRepeatCoveredSks: Int = 0
) {
    val feasible: Boolean get() = status == CspStatus.FEASIBLE
}

/** Answer to "how many SKS can this student realistically take?". */
data class KrsCapacityResult(
    /** Raw SKS ceiling derived from the previous semester's IP (before MKWU reservation). */
    val maxSksByIp: Int,
    /** SKS set aside for MKWU (filled manually in the portal, not part of the draft). */
    val mkwuReservedSks: Int,
    /** SKS budget available for the KRS draft: [maxSksByIp] - [mkwuReservedSks]. */
    val theoreticalMaxSks: Int,
    val maxAchievableSks: Int,
    val requiredSks: Int,
    val bindingConstraints: List<CspConstraintType>,
    val stats: CspSearchStats
)

/** Request accepted by the use case / tool. */
data class KrsCspRequest(
    val nim: String,
    val semester: Int,
    val requiredCourseCodes: List<String>,
    val optionalCourseCodes: List<String> = emptyList(),
    val excludedCourseCodes: List<String> = emptyList(),
    /** The student's KOM (A/B/C). Only own-semester mandatory courses are locked to it. */
    val studentClassCode: String? = null,
    /** 0 = use the maximum allowed by IP (and by the MBKM cap when applicable). */
    val targetSks: Int = 0,
    val mbkmMode: Boolean = false,
    /** Soft-constraint weights from the LLM: courseCode -> 0.0..1.0. */
    val interestWeights: Map<String, Double> = emptyMap(),
    val maxAlternatives: Int = 3
)
