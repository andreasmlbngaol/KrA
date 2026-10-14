package id.andreasmlbngaol.kra.agent.data.tools

import id.andreasmlbngaol.kra.academic.domain.model.ClassOffering
import id.andreasmlbngaol.kra.academic.domain.model.Course
import id.andreasmlbngaol.kra.academic.domain.model.KrsDraftItem
import id.andreasmlbngaol.kra.academic.domain.model.KrsValidationResult
import kotlinx.serialization.json.Json
import kotlin.math.round

/**
 * Shared [Json] instance for every agent tool.
 *
 * All tools return structured JSON (a String) so the LLM can read numbers and lists
 * reliably instead of parsing prose. prettyPrint is off to keep payloads compact;
 * encodeDefaults keeps empty lists / boolean flags visible to the model.
 */
val toolJson: Json = Json {
    prettyPrint = false
    encodeDefaults = true
}

/** Serializes a DTO to a JSON String. */
inline fun <reified T> T.toJson(): String = toolJson.encodeToString(this)

/** Rounds a Double to two decimal places. */
private fun Double.round2(): Double = round(this * 100.0) / 100.0

// ---------------------------------------------------------------------------
// Domain model -> DTO mappers
// ---------------------------------------------------------------------------

fun Course.toDto(): CourseDto = CourseDto(
    code = code,
    name = name,
    sks = sks,
    normalSemester = normalSemester,
    type = type
)

fun ClassOffering.toDto(): ClassOfferingDto = ClassOfferingDto(
    id = id.toString(),
    classCode = classCode,
    day = day,
    startTime = startTime,
    endTime = endTime,
    room = room,
    quota = quota,
    lecturers = lecturers.map { it.name }
)

fun KrsDraftItem.toDto(): KrsDraftItemDto = KrsDraftItemDto(
    classOfferingId = classOfferingId.toString(),
    courseCode = courseCode,
    courseName = courseName,
    classCode = classCode,
    room = room,
    day = day,
    startTime = startTime,
    endTime = endTime,
    sks = sks
)

/**
 * Maps a [KrsValidationResult] to its [ValidationResponse], deriving `overQuota` and the
 * overall `valid` flag from the individual checks.
 */
fun KrsValidationResult.toResponse(): ValidationResponse = ValidationResponse(
    totalSks = totalSks,
    maxSks = maxSks,
    mkwuReservedSks = mkwuReservedSks,
    remainingSks = remainingSks,
    overQuota = remainingSks < 0,
    scheduleConflicts = scheduleConflicts.map {
        ScheduleConflictDto(classA = it.classA, classB = it.classB, day = it.day)
    },
    prerequisiteViolations = prerequisiteViolations.map {
        PrerequisiteViolationDto(courseCode = it.courseCode, missingPrerequisites = it.missingPrerequisites)
    },
    duplicateCourses = duplicateCourses.map {
        DuplicateCourseDto(courseCode = it.courseCode, courseName = it.courseName, classCodes = it.classCodes)
    },
    electiveRequirement = ElectiveRequirementDto(
        requiredChoose = electiveRequirement.requiredChoose,
        selectedCount = electiveRequirement.selectedCount,
        satisfied = electiveRequirement.satisfied,
        options = electiveRequirement.options.map {
            ElectiveOptionDto(code = it.code, name = it.name, sks = it.sks)
        }
    ),
    valid = remainingSks >= 0 && scheduleConflicts.isEmpty() &&
        prerequisiteViolations.isEmpty() && duplicateCourses.isEmpty() && electiveRequirement.satisfied
)

/** Builds an [IpSemesterResponse], rounding the IP to two decimals. */
fun ipResponse(semester: Int, ip: Double, maxSks: Int): IpSemesterResponse =
    IpSemesterResponse(semester = semester, ip = ip.round2(), maxSksNextSemester = maxSks)
