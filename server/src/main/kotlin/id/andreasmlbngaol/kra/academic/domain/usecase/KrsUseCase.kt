package id.andreasmlbngaol.kra.academic.domain.usecase

import id.andreasmlbngaol.kra.academic.domain.model.KrsDraftItem
import id.andreasmlbngaol.kra.academic.domain.model.SaveDraftKrsResult
import id.andreasmlbngaol.kra.academic.domain.repository.CourseRepository
import id.andreasmlbngaol.kra.academic.domain.repository.KrsRepository
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Use cases for building and persisting a student's KRS draft.
 *
 * Wraps [KrsRepository] for storage and delegates all rule checking to
 * [KrsValidationUseCase] before any draft is saved.
 */
@Service
class KrsUseCase(
    private val krsRepository: KrsRepository,
    private val courseRepository: CourseRepository,
    private val krsValidationUseCase: KrsValidationUseCase
) {
    /**
     * Builds the detailed, display-ready KRS draft for a semester by joining each saved
     * KRS entry with its class offering and course.
     */
    fun getKrsDraftDetails(nim: String, semester: Int): List<KrsDraftItem> {
        val krs = krsRepository.getKrsByNim(nim, semester)
        if (krs.isEmpty()) return emptyList()

        val offerings = courseRepository
            .getClassOfferingsByIds(krs.map { it.classOfferingId })
            .associateBy { it.id }
        val courses = courseRepository
            .getCoursesByCodes(offerings.values.map { it.courseCode }.distinct())
            .associateBy { it.code }

        return krs.mapNotNull { entry ->
            val offering = offerings[entry.classOfferingId] ?: return@mapNotNull null
            val course = courses[offering.courseCode]
            KrsDraftItem(
                classOfferingId = entry.classOfferingId,
                courseCode = offering.courseCode,
                courseName = course?.name ?: offering.courseCode,
                classCode = offering.classCode,
                room = offering.room,
                day = offering.day,
                startTime = offering.startTime,
                endTime = offering.endTime,
                sks = course?.sks ?: 0,
                semester = entry.semester,
                normalSemester = course?.normalSemester ?: 0,
                courseType = course?.type,
                lecturers = offering.lecturers.map { it.name }
            )
        }
    }

    /**
     * Saves a KRS draft, running validation first as a safety net. The draft is only
     * persisted when nothing blocks it; otherwise the save is cancelled and the reasons
     * are returned via [SaveDraftKrsResult.Rejected].
     */
    fun saveDraftKrs(nim: String, classOfferingIds: List<UUID>, semester: Int): SaveDraftKrsResult {
        val validation = krsValidationUseCase.validateDraft(nim, semester, classOfferingIds)
        val hasBlockingIssue = validation.scheduleConflicts.isNotEmpty() ||
            validation.prerequisiteViolations.isNotEmpty() ||
            validation.duplicateCourses.isNotEmpty() ||
            validation.remainingSks < 0 ||
            !validation.electiveRequirement.satisfied
        if (hasBlockingIssue) return SaveDraftKrsResult.Rejected(validation)

        krsRepository.saveDraftKrs(nim, classOfferingIds, semester)
        return SaveDraftKrsResult.Success(validation)
    }

    /** Removes a single course from a KRS draft. */
    fun deleteDraftItem(nim: String, classOfferingId: UUID, semester: Int): Boolean =
        krsRepository.deleteDraftItem(nim, classOfferingId, semester)
}
