package id.andreasmlbngaol.kra.academic.domain.usecase

import id.andreasmlbngaol.kra.academic.domain.model.CspSolveLog
import id.andreasmlbngaol.kra.academic.domain.model.csp.CourseRole
import id.andreasmlbngaol.kra.academic.domain.model.csp.CspConstraintType
import id.andreasmlbngaol.kra.academic.domain.model.csp.CspDiagnosticCode
import id.andreasmlbngaol.kra.academic.domain.model.csp.CspProblem
import id.andreasmlbngaol.kra.academic.domain.model.csp.CspSolution
import id.andreasmlbngaol.kra.academic.domain.model.csp.CspVariable
import id.andreasmlbngaol.kra.academic.domain.model.csp.KrsCapacityResult
import id.andreasmlbngaol.kra.academic.domain.model.csp.KrsCspRequest
import id.andreasmlbngaol.kra.academic.domain.model.csp.RejectedCourse
import id.andreasmlbngaol.kra.academic.domain.model.rules.AcademicRules
import id.andreasmlbngaol.kra.academic.domain.repository.CourseRepository
import id.andreasmlbngaol.kra.academic.domain.repository.CspSolveLogRepository
import id.andreasmlbngaol.kra.academic.domain.solver.KrsCspSolver
import id.andreasmlbngaol.kra.core.enums.CourseType
import org.springframework.stereotype.Service

/**
 * Builds a [CspProblem] from the academic database and runs the deterministic solver.
 *
 * This is the only place allowed to translate persisted academic data into CSP variables,
 * domains and constraint parameters. The LLM never constructs the model itself — it only
 * chooses *which* courses are required/optional and supplies preference weights.
 */
@Service
class KrsCspUseCase(
    private val courseUseCase: CourseUseCase,
    private val khsUseCase: KhsUseCase,
    private val courseRepository: CourseRepository,
    private val cspSolveLogRepository: CspSolveLogRepository
) {

    private val solver = KrsCspSolver()

    /** Solves one KRS instance and records solver telemetry. */
    fun solve(request: KrsCspRequest): CspSolution {
        val problem = buildProblem(request)
        val solution = solver.solve(problem)
        recordTelemetry(problem, solution)
        return solution
    }

    /** Maximum realistically achievable SKS plus the constraints that cap it. */
    fun capacity(request: KrsCspRequest): KrsCapacityResult {
        val problem = buildProblem(request.copy(targetSks = 0))
        return solver.capacity(problem)
    }

    /**
     * Capacity for a whole semester, assembling the candidate pool automatically:
     * remaining mandatory courses + this semester's elective group + eligible upper courses.
     * Used by the "maksimalkan atau santai?" step, before the student picks anything.
     */
    fun capacityForSemester(
        nim: String,
        semester: Int,
        studentClassCode: String?,
        mbkmMode: Boolean
    ): KrsCapacityResult {
        val mandatory = courseUseCase.getRemainingMandatoryCourses(nim, semester).map { it.code }
        val context = courseUseCase.resolveSemesterContext(nim, semester)
        val electives = context.electiveSemester
            ?.let { courseUseCase.getSemesterElectives(it) }
            ?.map { it.code }
            .orEmpty()
        val upper = courseUseCase.getEligibleUpperCourses(nim, semester).map { it.code }
        return capacity(
            KrsCspRequest(
                nim = nim,
                semester = semester,
                requiredCourseCodes = mandatory,
                optionalCourseCodes = (electives + upper).distinct(),
                studentClassCode = studentClassCode,
                targetSks = 0,
                mbkmMode = mbkmMode
            )
        )
    }

    /** Exposed for tests: builds the instance without running the search. */
    fun buildProblem(request: KrsCspRequest): CspProblem {
        val normalized = request.copy(studentClassCode = normalizeKom(request.studentClassCode))
        val semester = normalized.semester

        // One resolver decides every semester-scoped number, so the solver and the
        // validator can never disagree about the cap, the MKWU slot, or the elective group.
        val context = courseUseCase.resolveSemesterContext(normalized.nim, semester)
        val maxSks = khsUseCase.getMaxSks(normalized.nim, context.lastGraded)
        val mkwuReserved = context.mkwuReservedSks
        val cap = if (normalized.mbkmMode) minOf(20, maxSks) else maxSks
        val effectiveTarget = if (normalized.targetSks <= 0) cap else minOf(normalized.targetSks, cap)

        val passedCodes = khsUseCase.getPassedCourseCodes(normalized.nim).toSet()
        val passedSks = khsUseCase.getTotalPassedSks(normalized.nim)

        val requiredCodes = clean(normalized.requiredCourseCodes)
        val excluded = clean(normalized.excludedCourseCodes).toSet()
        // When the D load exceeds 8 SKS, repeating D courses stops being optional. Add
        // every D-graded course to the pool so the solver can cover the minimum even if
        // the LLM forgot to list them; an explicit exclusion still wins.
        val dRepeatCodes = if (context.dRepeatMinSks > 0) {
            courseUseCase.getDGradedCourses(normalized.nim).map { it.code }
        } else {
            emptyList()
        }
        val optionalCodes = (clean(normalized.optionalCourseCodes) + dRepeatCodes)
            .distinct()
            .filter { it !in requiredCodes && it !in excluded }
        val candidateCodes = (requiredCodes + optionalCodes).filter { it !in excluded }

        val courses = courseRepository.getCoursesByCodes(candidateCodes).associateBy { it.code }
        val offeringsByCourse = courseRepository
            .getClassOfferingsByCourseCodes(candidateCodes)
            .groupBy { it.courseCode }
        val catalog = (courseUseCase.getOfferedCourses() + courses.values).distinctBy { it.code }

        val importance = courseUseCase.getImportanceScores(normalized.nim)
        val rejected = mutableListOf<RejectedCourse>()
        val variables = mutableListOf<CspVariable>()

        candidateCodes.forEach { code ->
            val role = if (code in requiredCodes) CourseRole.REQUIRED else CourseRole.OPTIONAL
            val course = courses[code]

            if (course == null) {
                if (role == CourseRole.REQUIRED) {
                    rejected += RejectedCourse(
                        course = placeholder(code),
                        code = CspDiagnosticCode.NO_AVAILABLE_CLASS,
                        constraint = CspConstraintType.PARITY,
                        detail = "Kode mata kuliah $code tidak ditemukan di katalog."
                    )
                }
                return@forEach
            }

            val rejection = unaryRejection(course, passedCodes, passedSks, catalog, candidateCodes)
            if (rejection != null) {
                if (role == CourseRole.REQUIRED) rejected += rejection
                return@forEach
            }

            variables += CspVariable(
                course = course,
                role = role,
                domain = offeringsByCourse[code].orEmpty(),
                importance = importance[code] ?: 1,
                // Only a mandatory course of the student's own semester is pinned to
                // their KOM. Electives, repeat courses and upper-semester courses may
                // use the single merged class even when it is labelled with another KOM.
                komLocked = course.type == CourseType.REGULAR_MANDATORY && course.normalSemester == semester
            )
        }

        val electiveCodes = context.electiveSemester
            ?.let { courseUseCase.getSemesterElectives(it) }
            ?.map { it.code }
            ?.toSet()
            .orEmpty()
        val requiredElectiveCount = if (electiveCodes.isEmpty()) 0 else 1

        return CspProblem(
            request = normalized,
            variables = variables,
            maxSks = maxSks,
            mkwuReservedSks = mkwuReserved,
            effectiveTargetSks = effectiveTarget,
            requiredElectiveCount = requiredElectiveCount,
            passedCourseCodes = passedCodes,
            catalog = catalog,
            electiveCourseCodes = electiveCodes,
            rejectedCourses = rejected,
            dRepeatCourseCodes = dRepeatCodes
                .filter { code -> variables.any { it.course.code == code } }
                .toSet(),
            dSks = context.dSks,
            dRepeatMinSks = context.dRepeatMinSks,
            activeConstraintCount = countActiveConstraints(
                courses = courses.values.toList(),
                requiredElectiveCount = requiredElectiveCount,
                komLocked = normalized.studentClassCode != null,
                mbkm = normalized.mbkmMode,
                dRepeat = context.dRepeatMinSks > 0
            )
        )
    }

    // ---------------------------------------------------------------------
    // Preprocessing (unary constraints)
    // ---------------------------------------------------------------------

    private fun unaryRejection(
        course: id.andreasmlbngaol.kra.academic.domain.model.Course,
        passedCodes: Set<String>,
        passedSks: Int,
        catalog: List<id.andreasmlbngaol.kra.academic.domain.model.Course>,
        candidateCodes: List<String>
    ): RejectedCourse? {
        if (course.minSks > passedSks) {
            return RejectedCourse(
                course = course,
                code = CspDiagnosticCode.MIN_SKS_UNMET,
                constraint = CspConstraintType.MIN_SKS,
                detail = "${course.code} membutuhkan minimal ${course.minSks} SKS lulus (saat ini $passedSks)."
            )
        }
        val missing = course.prerequisites.filter { it !in passedCodes }
        if (missing.isNotEmpty()) {
            return RejectedCourse(
                course = course,
                code = CspDiagnosticCode.PREREQUISITE_UNMET,
                constraint = CspConstraintType.PREREQUISITE,
                detail = "${course.code} belum memenuhi prasyarat: ${missing.joinToString(", ")}."
            )
        }
        if (AcademicRules.requiresTheory(course, catalog, passedCodes)) {
            val theory = AcademicRules.theoryOf(course, catalog)
            if (theory != null && theory.code !in candidateCodes) {
                return RejectedCourse(
                    course = course,
                    code = CspDiagnosticCode.PRACTICAL_THEORY_MISSING,
                    constraint = CspConstraintType.PRACTICAL_PAIRING,
                    detail = "${course.code} memerlukan teori ${theory.code} yang belum diambil."
                )
            }
        }
        return null
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private fun clean(codes: List<String>): List<String> =
        codes.map { it.trim().uppercase() }.filter { it.isNotEmpty() }.distinct()

    private fun normalizeKom(raw: String?): String? {
        val value = raw?.trim()?.uppercase() ?: return null
        if (value.isEmpty()) return null
        return value.removePrefix("KOM").trim().takeIf { it.isNotEmpty() }
    }

    private fun placeholder(code: String) = id.andreasmlbngaol.kra.academic.domain.model.Course(
        code = code,
        name = code,
        sks = 0,
        normalSemester = 0,
        type = CourseType.REGULAR_MANDATORY
    )

    private fun countActiveConstraints(
        courses: List<id.andreasmlbngaol.kra.academic.domain.model.Course>,
        requiredElectiveCount: Int,
        komLocked: Boolean,
        mbkm: Boolean,
        dRepeat: Boolean
    ): Int {
        var count = 2 // QUOTA + SCHEDULE_CONFLICT selalu aktif
        if (courses.any { it.prerequisites.isNotEmpty() }) count++ // PREREQUISITE
        if (courses.any { it.minSks > 0 }) count++ // MIN_SKS
        if (courses.any { AcademicRules.isPractical(it) }) count++ // PRACTICAL_PAIRING
        count++ // TOTAL_SKS
        if (requiredElectiveCount > 0) count++ // ELECTIVE_COUNT
        if (komLocked) count++ // KOM_CONSISTENCY
        if (mbkm) count++ // MBKM_CAP
        if (dRepeat) count++ // D_GRADE_LIMIT
        return count
    }

    private fun recordTelemetry(problem: CspProblem, solution: CspSolution) {
        runCatching {
            cspSolveLogRepository.record(
                CspSolveLog(
                    nim = problem.request.nim,
                    semester = problem.request.semester,
                    status = solution.status,
                    objective = solution.objective,
                    variables = problem.variables.size,
                    constraints = problem.activeConstraintCount,
                    domainValues = problem.domainValueCount,
                    targetSks = problem.effectiveTargetSks,
                    resultSks = solution.totalSks,
                    nodes = solution.stats.nodes,
                    backtracks = solution.stats.backtracks,
                    ac3Revisions = solution.stats.ac3Revisions,
                    forwardChecks = solution.stats.forwardChecks,
                    durationMs = solution.stats.durationMs,
                    alternatives = solution.alternatives.size,
                    mbkmMode = problem.request.mbkmMode
                )
            )
        }
    }
}
