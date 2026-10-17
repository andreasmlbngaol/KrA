package id.andreasmlbngaol.kra.academic.domain.model.csp

import kotlinx.serialization.Serializable

/**
 * Every status/reason in the CSP layer is an enum (never a free-form string) so the
 * compiler — and the LLM's tool schema — can only ever see a closed set of values.
 */

/** Outcome of a solve: a complete assignment exists, or it does not. */
@Serializable
enum class CspStatus {
    FEASIBLE,
    INFEASIBLE
}

/** What the solver optimises once feasibility is established. */
@Serializable
enum class CspObjective {
    /** Fill as many SKS as possible without exceeding the budget. */
    MAXIMIZE_SKS,

    /** Get as close as possible to the student's explicit target. */
    MEET_TARGET,

    /** Only decide whether the request is satisfiable. */
    FEASIBILITY_ONLY
}

/** Role of a course inside the problem instance. */
@Serializable
enum class CourseRole {
    /**
     * Should be assigned. A draft that leaves it out is still a valid KRS — the course is
     * reported in `unassignedRequired` with the reason — but the solver always prefers a
     * solution that places it.
     */
    REQUIRED,

    /** May be assigned when the budget and the schedule allow it. */
    OPTIONAL,

    /** Must never be assigned. */
    EXCLUDED
}

/** The kinds of constraint the solver enforces. */
@Serializable
enum class CspConstraintType {
    PREREQUISITE,
    PARITY,
    MIN_SKS,
    QUOTA,
    SCHEDULE_CONFLICT,
    PRACTICAL_PAIRING,
    TOTAL_SKS,
    ELECTIVE_COUNT,
    KOM_CONSISTENCY,
    MBKM_CAP,

    /** At most 8 SKS with grade D may remain at graduation. */
    D_GRADE_LIMIT
}

/** Why a course could not be placed, or why the instance is infeasible. */
@Serializable
enum class CspDiagnosticCode {
    BUDGET_EXCEEDED,
    SCHEDULE_CONFLICT,
    NO_AVAILABLE_CLASS,
    QUOTA_FULL,
    PREREQUISITE_UNMET,
    PARITY_MISMATCH,
    MIN_SKS_UNMET,
    ELECTIVE_UNSATISFIED,
    KOM_UNAVAILABLE,
    PRACTICAL_THEORY_MISSING,

    /** The draft does not repeat enough D-graded SKS to get under the 8-SKS limit. */
    D_GRADE_LIMIT
}

/** A concrete action that would make an infeasible instance feasible. */
@Serializable
enum class CspRelaxationAction {
    RAISE_TARGET,
    DROP_COURSE,
    SWITCH_KOM,
    ADD_ELECTIVE,
    REDUCE_SKS
}
