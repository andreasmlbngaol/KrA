package id.andreasmlbngaol.kra.agent.data.tools

import id.andreasmlbngaol.kra.academic.domain.model.csp.CspConstraintType
import id.andreasmlbngaol.kra.academic.domain.model.csp.CspDiagnosticCode
import id.andreasmlbngaol.kra.academic.domain.model.csp.CspRelaxationAction
import id.andreasmlbngaol.kra.academic.domain.model.csp.CspStatus
import kotlinx.serialization.Serializable

/**
 * JSON contract of the CSP tools. Every enum is serialized by name, so the model only
 * ever sees a closed vocabulary (FEASIBLE, BUDGET_EXCEEDED, DROP_COURSE, ...).
 */

@Serializable
data class CspAssignmentDto(
    val courseCode: String,
    val courseName: String,
    val sks: Int,
    val classOfferingId: String,
    val classCode: String,
    val day: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val room: String? = null,
    val komSwitched: Boolean = false
)

@Serializable
data class CourseRefDto(
    val courseCode: String,
    val courseName: String,
    val sks: Int
)

@Serializable
data class CspDiagnosticDto(
    val code: CspDiagnosticCode,
    val constraint: CspConstraintType,
    val courseCodes: List<String>,
    val detail: String
)

@Serializable
data class CspRelaxationDto(
    val action: CspRelaxationAction,
    val courseCodes: List<String>,
    val gainSks: Int,
    val detail: String
)

@Serializable
data class CspAlternativeItemDto(
    val courseCode: String,
    val classCode: String
)

@Serializable
data class CspAlternativeDto(
    val label: String,
    val totalSks: Int,
    val deltaFromTarget: Int,
    val items: List<CspAlternativeItemDto>
)

@Serializable
data class CspSearchDto(
    val nodes: Int,
    val backtracks: Int,
    val ac3Revisions: Int,
    val forwardChecks: Int,
    val durationMs: Long,
    val solutionsFound: Int
)

@Serializable
data class SolveKrsResponse(
    val status: CspStatus,
    val totalSks: Int,
    val maxSks: Int,
    val effectiveTargetSks: Int,
    val mkwuReservedSks: Int,
    val remainingSks: Int,
    val assignments: List<CspAssignmentDto>,
    val unassignedRequired: List<CourseRefDto>,
    val diagnostics: List<CspDiagnosticDto>,
    val relaxations: List<CspRelaxationDto>,
    val alternatives: List<CspAlternativeDto>,
    /** Total SKS whose best grade is D. */
    val dSks: Int = 0,
    /** SKS of D that must be repeated so at most 8 SKS of D remain. */
    val dRepeatMinSks: Int = 0,
    /** SKS of D actually repeated in this draft. */
    val dRepeatCoveredSks: Int = 0,
    val search: CspSearchDto
)

@Serializable
data class KrsCapacityResponse(
    val maxSksByIp: Int,
    val mkwuReservedSks: Int,
    val theoreticalMaxSks: Int,
    val maxAchievableSks: Int,
    val requiredSks: Int,
    val bindingConstraints: List<CspConstraintType>,
    val search: CspSearchDto
)
