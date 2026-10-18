package id.andreasmlbngaol.kra.agent.data.tools

import id.andreasmlbngaol.kra.core.enums.CourseType
import id.andreasmlbngaol.kra.core.enums.Day
import id.andreasmlbngaol.kra.core.enums.Grade
import id.andreasmlbngaol.kra.core.enums.LockType
import kotlinx.serialization.Serializable

/**
 * Agent (LLM) tool output DTOs: the structured JSON contract each tool returns so the
 * model can read numbers & lists reliably. Distinct from the HTTP contract DTOs under
 * `...presentation.dto` (those are for the web app, not the LLM).
 */

// ---------------------------------------------------------------------------
// Student
// ---------------------------------------------------------------------------

@Serializable
data class StudentInfoResponse(
    val found: Boolean,
    val name: String? = null,
    /** Administrative semester (counts time enrolled, leaves included). */
    val semester: Int? = null,
    /** Plain-language summary of what the student still owes, computed from the KHS. */
    val semesterNote: String? = null,
    val message: String? = null
)

// ---------------------------------------------------------------------------
// KHS / grades
// ---------------------------------------------------------------------------

@Serializable
data class KhsEntryDto(
    val courseCode: String,
    val semesterTaken: Int,
    val grade: Grade
)

@Serializable
data class KhsResponse(
    val totalEntries: Int,
    val entries: List<KhsEntryDto>
)

@Serializable
data class IpSemesterResponse(
    val semester: Int,
    val ip: Double,
    val maxSksNextSemester: Int
)

@Serializable
data class TotalPassedSksResponse(
    val totalPassedSks: Int
)

@Serializable
data class UnlockedCourseDto(
    val courseCode: String,
    val courseName: String,
    val normalSemester: Int
)

@Serializable
data class RepeatableCourseDto(
    val code: String,
    val name: String,
    val sks: Int,
    val bestGrade: Grade,
    val mustRepeat: Boolean,
    /** Grade D and the student already exceeds the 8-SKS D limit. */
    val dRepeatMandatory: Boolean = false,
    val recommendedForRepeat: Boolean,
    val status: String,
    /** 1 + number of still-unpassed mandatory courses this course unlocks. */
    val importance: Int = 1,
    /** How many still-unpassed mandatory courses this course unlocks. */
    val unlocksCount: Int = 0,
    /** The courses behind [unlocksCount], in curriculum order. */
    val unlocks: List<UnlockedCourseDto> = emptyList(),
    val mbkmEligible: Boolean,
    val recognitionSemester: Int? = null
)

@Serializable
data class RepeatableCoursesResponse(
    val totalCourses: Int,
    /** Total SKS whose best grade is D. */
    val dGradeSks: Int = 0,
    /** Maximum D SKS allowed to graduate. */
    val dGradeMaxSks: Int = 8,
    /** SKS of D that must be repeated this term; 0 means repeating D is optional. */
    val dRepeatMinSks: Int = 0,
    val courses: List<RepeatableCourseDto>
)

// ---------------------------------------------------------------------------
// Courses / classes
// ---------------------------------------------------------------------------

@Serializable
data class CourseDto(
    val code: String,
    val name: String,
    val sks: Int,
    val normalSemester: Int,
    val type: CourseType,
    /**
     * Only filled by `getRemainingMandatoryCourses`: 1 + number of still-unpassed mandatory
     * courses that transitively require this one. Higher means "repeat this first".
     */
    val importance: Int? = null
)

@Serializable
data class AvailableCoursesResponse(
    val semester: Int,
    val totalCourses: Int,
    val courses: List<CourseDto>
)

@Serializable
data class CurriculumPackageItemDto(
    val courseCode: String,
    val courseName: String? = null,
    val sks: Int? = null,
    val type: CourseType? = null,
    val lockType: LockType
)

@Serializable
data class CurriculumPackageResponse(
    val semester: Int,
    val totalSks: Int,
    val items: List<CurriculumPackageItemDto>
)

@Serializable
data class ClassOfferingDto(
    val id: String,
    val classCode: String,
    val day: Day? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val room: String? = null,
    val quota: Int,
    val lecturers: List<String>
)

@Serializable
data class CourseClassesItem(
    val courseCode: String,
    val courseName: String? = null,
    val sks: Int? = null,
    val totalClasses: Int,
    val classes: List<ClassOfferingDto>
)

@Serializable
data class CourseClassesResponse(
    val totalCourses: Int,
    val courses: List<CourseClassesItem>
)

@Serializable
data class TotalSksResponse(
    val courseCodes: List<String>,
    val totalSks: Int
)

@Serializable
data class RemainingMandatoryCoursesResponse(
    val semester: Int,
    val totalSks: Int,
    val totalCourses: Int,
    val courses: List<CourseDto>
)

@Serializable
data class RequiredElectivesResponse(
    val semester: Int,
    val requiredChoose: Int,
    val totalOptions: Int,
    val courses: List<CourseDto>
)

@Serializable
data class EligibleCourseDto(
    val code: String,
    val name: String,
    val sks: Int,
    val normalSemester: Int,
    val type: CourseType,
    val pairingNote: String? = null
)

@Serializable
data class EligibleUpperCoursesResponse(
    val semester: Int,
    val totalCourses: Int,
    val courses: List<EligibleCourseDto>
)

@Serializable
data class CapstoneCourseDto(
    val code: String,
    val found: Boolean,
    val name: String? = null,
    val sks: Int? = null,
    val minSks: Int? = null,
    val eligible: Boolean? = null
)

@Serializable
data class CapstoneCoursesResponse(
    val totalPassedSks: Int,
    val courses: List<CapstoneCourseDto>
)

// ---------------------------------------------------------------------------
// KRS / draft / budget / validation
// ---------------------------------------------------------------------------

@Serializable
data class KrsDraftItemDto(
    val classOfferingId: String,
    val courseCode: String,
    val courseName: String,
    val classCode: String,
    val room: String? = null,
    val day: Day? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val sks: Int
)

@Serializable
data class KrsDraftResponse(
    val semester: Int,
    val exists: Boolean,
    val totalSks: Int,
    val items: List<KrsDraftItemDto>
)

@Serializable
data class ScheduleConflictDto(
    val classA: String,
    val classB: String,
    val day: Day
)

@Serializable
data class PrerequisiteViolationDto(
    val courseCode: String,
    val missingPrerequisites: List<String>
)

@Serializable
data class DuplicateCourseDto(
    val courseCode: String,
    val courseName: String,
    val classCodes: List<String>
)

@Serializable
data class ElectiveOptionDto(
    val code: String,
    val name: String,
    val sks: Int
)

@Serializable
data class ElectiveRequirementDto(
    val requiredChoose: Int,
    val selectedCount: Int,
    val satisfied: Boolean,
    val options: List<ElectiveOptionDto>
)

@Serializable
data class ValidationResponse(
    val totalSks: Int,
    val maxSks: Int,
    val mkwuReservedSks: Int,
    val remainingSks: Int,
    val overQuota: Boolean,
    val scheduleConflicts: List<ScheduleConflictDto>,
    val prerequisiteViolations: List<PrerequisiteViolationDto>,
    val duplicateCourses: List<DuplicateCourseDto>,
    val electiveRequirement: ElectiveRequirementDto,
    val valid: Boolean
)

@Serializable
data class SaveDraftResponse(
    val status: String,
    val savedCourseCount: Int? = null,
    val message: String,
    val validation: ValidationResponse
)

@Serializable
data class ToolErrorResponse(
    val error: Boolean = true,
    val message: String
)
