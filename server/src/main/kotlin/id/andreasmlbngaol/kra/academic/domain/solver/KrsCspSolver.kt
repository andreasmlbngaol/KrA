package id.andreasmlbngaol.kra.academic.domain.solver

import id.andreasmlbngaol.kra.academic.domain.model.ClassOffering
import id.andreasmlbngaol.kra.academic.domain.model.csp.CourseRole
import id.andreasmlbngaol.kra.academic.domain.model.csp.CspAlternative
import id.andreasmlbngaol.kra.academic.domain.model.csp.CspAlternativeItem
import id.andreasmlbngaol.kra.academic.domain.model.csp.CspAssignment
import id.andreasmlbngaol.kra.academic.domain.model.csp.CspConstraintType
import id.andreasmlbngaol.kra.academic.domain.model.csp.CspDiagnostic
import id.andreasmlbngaol.kra.academic.domain.model.csp.CspDiagnosticCode
import id.andreasmlbngaol.kra.academic.domain.model.csp.CspObjective
import id.andreasmlbngaol.kra.academic.domain.model.csp.CspProblem
import id.andreasmlbngaol.kra.academic.domain.model.csp.CspRelaxation
import id.andreasmlbngaol.kra.academic.domain.model.csp.CspRelaxationAction
import id.andreasmlbngaol.kra.academic.domain.model.csp.CspSearchStats
import id.andreasmlbngaol.kra.academic.domain.model.csp.CspSolution
import id.andreasmlbngaol.kra.academic.domain.model.csp.CspStatus
import id.andreasmlbngaol.kra.academic.domain.model.csp.CspVariable
import id.andreasmlbngaol.kra.academic.domain.model.csp.KrsCapacityResult
import id.andreasmlbngaol.kra.academic.domain.model.rules.AcademicRules
import java.util.ArrayDeque

/**
 * Deterministic CSP solver for KRS composition.
 *
 * Model
 * -----
 * - Variable  : one course that must (REQUIRED) or may (OPTIONAL) be taken.
 * - Domain    : the class offerings of that course in the active term that still have
 *               a free seat (`quota - enrolled > 0`). OPTIONAL variables additionally
 *               carry the value `null`, meaning "not taken this semester".
 * - Constraints:
 *     unary    prerequisite, min SKS, quota, KOM lock (preprocessed / domain-level)
 *     binary   no schedule overlap (TBD schedules never conflict)
 *     global   total SKS budget, exact elective count, practical-needs-theory, MBKM cap
 *
 * Algorithm
 * ---------
 * 1. AC-3 on the initial domains prunes every value that has no support in a neighbour.
 * 2. Depth-first backtracking with MAC (Maintaining Arc Consistency): after each
 *    assignment the affected arcs are re-queued, so forward checking and full arc
 *    consistency propagation happen on every node.
 * 3. Variable ordering MRV (minimum remaining values), value ordering prefers the
 *    student's KOM and earlier schedule slots.
 * 4. Branch and bound on (total SKS, interest weight, KOM switches).
 *
 * The solver is pure: no database, no Spring, no clock other than its own timing.
 */
class KrsCspSolver(private val nodeBudget: Int = 200_000) {

    /** Solves the instance and returns a complete, explainable result. */
    fun solve(problem: CspProblem): CspSolution {
        val objective = if (problem.request.targetSks > 0) CspObjective.MEET_TARGET else CspObjective.MAXIMIZE_SKS
        val startedNs = System.nanoTime()
        val search = Search(problem, objective, nodeBudget)
        val best = search.run()
        val elapsedMs = (System.nanoTime() - startedNs) / 1_000_000

        if (best == null) {
            val (diagnostics, relaxations) = diagnose(problem)
            return CspSolution(
                status = CspStatus.INFEASIBLE,
                objective = objective,
                assignments = emptyList(),
                totalSks = 0,
                maxSks = problem.maxSks,
                mkwuReservedSks = problem.mkwuReservedSks,
                effectiveTargetSks = problem.effectiveTargetSks,
                remainingSks = problem.courseBudgetSks,
                unassignedRequired = problem.variables
                    .filter { it.role == CourseRole.REQUIRED }
                    .map { it.course },
                diagnostics = (diagnostics + listOfNotNull(dShortfallDiagnostic(problem, 0)))
                    .distinctBy { it.code to it.courseCodes },
                relaxations = relaxations,
                alternatives = emptyList(),
                stats = search.stats(elapsedMs),
                dSks = problem.dSks,
                dRepeatMinSks = problem.dRepeatMinSks,
                dRepeatCoveredSks = 0
            )
        }

        val assignments = best.assignment
        val totalSks = assignments.sumOf { it.course.sks }
        val budget = problem.courseBudgetSks
        val placedCodes = assignments.map { it.course.code }.toSet()
        val unplaced = problem.variables.filter {
            it.role == CourseRole.REQUIRED && it.course.code !in placedCodes
        }
        val dCovered = assignments
            .filter { it.course.code in problem.dRepeatCourseCodes }
            .sumOf { it.course.sks }

        return CspSolution(
            status = CspStatus.FEASIBLE,
            objective = objective,
            assignments = assignments,
            totalSks = totalSks,
            maxSks = problem.maxSks,
            mkwuReservedSks = problem.mkwuReservedSks,
            effectiveTargetSks = problem.effectiveTargetSks,
            remainingSks = (budget - totalSks).coerceAtLeast(0),
            unassignedRequired = unplaced.map { it.course },
            diagnostics = (problem.rejectedCourses.map {
                CspDiagnostic(it.code, it.constraint, listOf(it.course.code), it.detail)
            } + unplaced.map { explainUnplaced(problem, it, totalSks) } +
                listOfNotNull(dShortfallDiagnostic(problem, dCovered)))
                .distinctBy { it.code to it.courseCodes },
            relaxations = emptyList(),
            alternatives = search.alternatives(best),
            stats = search.stats(elapsedMs),
            dSks = problem.dSks,
            dRepeatMinSks = problem.dRepeatMinSks,
            dRepeatCoveredSks = dCovered
        )
    }

    /**
     * Maximum SKS the student can realistically take, plus which constraint families
     * prevent reaching the theoretical maximum.
     */
    fun capacity(problem: CspProblem): KrsCapacityResult {
        val startedNs = System.nanoTime()
        val search = Search(problem, CspObjective.MAXIMIZE_SKS, nodeBudget)
        val best = search.run()
        val elapsedMs = (System.nanoTime() - startedNs) / 1_000_000

        val theoretical = problem.courseBudgetSks
        val achievable = best?.assignment?.sumOf { it.course.sks } ?: 0
        val requiredSks = problem.variables.filter { it.role == CourseRole.REQUIRED }.sumOf { it.course.sks }

        val binding = buildList {
            if (requiredSks > theoretical) add(CspConstraintType.TOTAL_SKS)
            if (achievable < theoretical) {
                if (problem.variables.none { it.role == CourseRole.OPTIONAL }) {
                    add(CspConstraintType.TOTAL_SKS)
                } else {
                    add(CspConstraintType.SCHEDULE_CONFLICT)
                    add(CspConstraintType.QUOTA)
                }
            }
        }.distinct()

        return KrsCapacityResult(
            maxSksByIp = problem.maxSks,
            mkwuReservedSks = problem.mkwuReservedSks,
            theoreticalMaxSks = theoretical,
            maxAchievableSks = achievable,
            requiredSks = requiredSks,
            bindingConstraints = binding,
            stats = search.stats(elapsedMs)
        )
    }

    // ---------------------------------------------------------------------
    // Diagnostics for an infeasible instance
    // ---------------------------------------------------------------------

    private fun diagnose(problem: CspProblem): Pair<List<CspDiagnostic>, List<CspRelaxation>> {
        val diagnostics = mutableListOf<CspDiagnostic>()
        val relaxations = mutableListOf<CspRelaxation>()
        val budget = problem.courseBudgetSks

        problem.rejectedCourses.forEach {
            diagnostics += CspDiagnostic(it.code, it.constraint, listOf(it.course.code), it.detail)
        }

        val required = problem.variables.filter { it.role == CourseRole.REQUIRED }
        val requiredSks = required.sumOf { it.course.sks }

        if (requiredSks > budget) {
            diagnostics += CspDiagnostic(
                code = CspDiagnosticCode.BUDGET_EXCEEDED,
                constraint = CspConstraintType.TOTAL_SKS,
                courseCodes = required.map { it.course.code },
                detail = "Total SKS wajib $requiredSks melebihi anggaran $budget SKS."
            )
            relaxations += CspRelaxation(
                action = CspRelaxationAction.RAISE_TARGET,
                courseCodes = emptyList(),
                gainSks = requiredSks - budget,
                detail = "Naikkan target minimal ${requiredSks - budget} SKS, atau kurangi mata kuliah wajib."
            )
        }

        val kom = problem.request.studentClassCode?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }
        required.forEach { variable ->
            val seats = variable.domain.filter { it.quota - it.enrolled > 0 }
            when {
                variable.domain.isEmpty() -> diagnostics += CspDiagnostic(
                    code = CspDiagnosticCode.NO_AVAILABLE_CLASS,
                    constraint = CspConstraintType.PARITY,
                    courseCodes = listOf(variable.course.code),
                    detail = "${variable.course.code} tidak ditawarkan pada semester ini."
                )

                seats.isEmpty() -> diagnostics += CspDiagnostic(
                    code = CspDiagnosticCode.QUOTA_FULL,
                    constraint = CspConstraintType.QUOTA,
                    courseCodes = listOf(variable.course.code),
                    detail = "Semua kelas ${variable.course.code} sudah penuh."
                )

                variable.komLocked && kom != null && seats.none { it.classCode.equals(kom, true) } -> {
                    diagnostics += CspDiagnostic(
                        code = CspDiagnosticCode.KOM_UNAVAILABLE,
                        constraint = CspConstraintType.KOM_CONSISTENCY,
                        courseCodes = listOf(variable.course.code),
                        detail = "Kelas KOM $kom untuk ${variable.course.code} tidak tersedia."
                    )
                    relaxations += CspRelaxation(
                        action = CspRelaxationAction.SWITCH_KOM,
                        courseCodes = listOf(variable.course.code),
                        gainSks = variable.course.sks,
                        detail = "Izinkan ${variable.course.code} diambil di KOM lain."
                    )
                }
            }
        }

        if (problem.requiredElectiveCount > 0) {
            val electiveVariables = problem.variables.filter { it.course.code in problem.electiveCourseCodes }
            if (electiveVariables.isEmpty()) {
                diagnostics += CspDiagnostic(
                    code = CspDiagnosticCode.ELECTIVE_UNSATISFIED,
                    constraint = CspConstraintType.ELECTIVE_COUNT,
                    courseCodes = emptyList(),
                    detail = "Semester ini mewajibkan ${problem.requiredElectiveCount} mata kuliah pilihan, " +
                        "tetapi belum ada pilihan yang diajukan."
                )
                relaxations += CspRelaxation(
                    action = CspRelaxationAction.ADD_ELECTIVE,
                    courseCodes = emptyList(),
                    gainSks = 0,
                    detail = "Pilih satu mata kuliah pilihan untuk semester ini."
                )
            }
        }

        // Which single required course, if dropped, would make the instance feasible?
        if (required.size <= MAX_DROP_TESTS) {
            required.forEach { variable ->
                if (isFeasibleWithout(problem, variable.course.code)) {
                    relaxations += CspRelaxation(
                        action = CspRelaxationAction.DROP_COURSE,
                        courseCodes = listOf(variable.course.code),
                        gainSks = variable.course.sks,
                        detail = "Tanpa ${variable.course.code} (${variable.course.name}) jadwal menjadi layak."
                    )
                }
            }
        }

        // Pairwise schedule conflicts among required courses (when both have seats).
        val conflictPairs = mutableListOf<List<String>>()
        for (i in required.indices) {
            for (j in i + 1 until required.size) {
                val a = required[i]
                val b = required[j]
                val seatsA = a.domain.filter { it.quota - it.enrolled > 0 }
                val seatsB = b.domain.filter { it.quota - it.enrolled > 0 }
                if (seatsA.isEmpty() || seatsB.isEmpty()) continue
                val anyCompatible = seatsA.any { ca -> seatsB.any { cb -> compatible(ca, cb) } }
                if (!anyCompatible) {
                    conflictPairs += listOf(a.course.code, b.course.code)
                }
            }
        }
        if (conflictPairs.isNotEmpty()) {
            diagnostics += CspDiagnostic(
                code = CspDiagnosticCode.SCHEDULE_CONFLICT,
                constraint = CspConstraintType.SCHEDULE_CONFLICT,
                courseCodes = conflictPairs.flatten().distinct(),
                detail = "Tidak ada kombinasi kelas yang bebas bentrok untuk: " +
                    conflictPairs.joinToString("; ") { it.joinToString(" vs ") } + "."
            )
        }

        return diagnostics.distinctBy { it.code to it.courseCodes } to
            relaxations.distinctBy { it.action to it.courseCodes }
    }

    /**
     * The D-grade rule: at most 8 SKS with grade D may remain at graduation. When the
     * draft repeats less than the shortfall, the draft is still valid (the repeat can be
     * done later), but the student must be told the exact remaining obligation.
     */
    private fun dShortfallDiagnostic(problem: CspProblem, coveredSks: Int): CspDiagnostic? {
        if (problem.dRepeatMinSks <= 0 || coveredSks >= problem.dRepeatMinSks) return null
        return CspDiagnostic(
            code = CspDiagnosticCode.D_GRADE_LIMIT,
            constraint = CspConstraintType.D_GRADE_LIMIT,
            courseCodes = problem.dRepeatCourseCodes.toList().sorted(),
            detail = "Total nilai D ${problem.dSks} SKS melebihi batas ${AcademicRules.MAX_D_SKS} SKS. " +
                "Minimal ${problem.dRepeatMinSks} SKS matkul D harus diulang; " +
                "draft ini baru mengulang $coveredSks SKS."
        )
    }

    /**
     * Why a required course was left out of an otherwise feasible draft. The draft stays
     * valid; the student (and the LLM) still need the reason.
     */
    private fun explainUnplaced(problem: CspProblem, variable: CspVariable, usedSks: Int): CspDiagnostic {
        val code = variable.course.code
        val kom = problem.request.studentClassCode?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }
        val seats = variable.domain.filter { it.quota - it.enrolled > 0 }
        return when {
            variable.domain.isEmpty() -> CspDiagnostic(
                code = CspDiagnosticCode.NO_AVAILABLE_CLASS,
                constraint = CspConstraintType.PARITY,
                courseCodes = listOf(code),
                detail = "$code tidak ditawarkan pada term aktif."
            )

            seats.isEmpty() -> CspDiagnostic(
                code = CspDiagnosticCode.QUOTA_FULL,
                constraint = CspConstraintType.QUOTA,
                courseCodes = listOf(code),
                detail = "Semua kelas $code sudah penuh."
            )

            variable.komLocked && kom != null && seats.none { it.classCode.equals(kom, true) } -> CspDiagnostic(
                code = CspDiagnosticCode.KOM_UNAVAILABLE,
                constraint = CspConstraintType.KOM_CONSISTENCY,
                courseCodes = listOf(code),
                detail = "Kelas KOM $kom untuk $code tidak tersedia."
            )

            usedSks + variable.course.sks > problem.courseBudgetSks -> CspDiagnostic(
                code = CspDiagnosticCode.BUDGET_EXCEEDED,
                constraint = CspConstraintType.TOTAL_SKS,
                courseCodes = listOf(code),
                detail = "Kuota draft tersisa ${(problem.courseBudgetSks - usedSks).coerceAtLeast(0)} SKS, " +
                    "tidak cukup untuk $code (${variable.course.sks} SKS)."
            )

            else -> CspDiagnostic(
                code = CspDiagnosticCode.SCHEDULE_CONFLICT,
                constraint = CspConstraintType.SCHEDULE_CONFLICT,
                courseCodes = listOf(code),
                detail = "$code tidak bisa ditempatkan tanpa bentrok jadwal atau melebihi kuota SKS."
            )
        }
    }

    private fun isFeasibleWithout(problem: CspProblem, courseCode: String): Boolean {
        val reduced = problem.copy(variables = problem.variables.filter { it.course.code != courseCode })
        if (reduced.variables.isEmpty()) return true
        val probe = Search(reduced, CspObjective.FEASIBILITY_ONLY, PROBE_NODE_BUDGET)
        return probe.run() != null
    }

    // ---------------------------------------------------------------------
    // Search
    // ---------------------------------------------------------------------

    private class Search(
        private val problem: CspProblem,
        private val objective: CspObjective,
        private val budget: Int
    ) {
        private val variables: List<CspVariable> = problem.variables
        private val byCode: Map<String, CspVariable> = variables.associateBy { it.course.code }
        private val kom: String? = problem.request.studentClassCode?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }
        private val sksBudget: Int = problem.courseBudgetSks

        private val domains = LinkedHashMap<String, MutableList<ClassOffering?>>()
        private val assigned = LinkedHashMap<String, ClassOffering?>()

        private var nodes = 0
        private var backtracks = 0
        private var ac3Revisions = 0
        private var forwardChecks = 0
        private var solutionsFound = 0
        private var stopped = false

        private val candidates = mutableListOf<Candidate>()

        init {
            variables.forEach { variable ->
                domains[variable.course.code] = buildDomain(variable)
            }
        }

        fun run(): Candidate? {
            if (domains.values.any { it.isEmpty() }) return null
            val initialQueue = ArrayDeque<Arc>()
            for (a in variables) {
                for (b in variables) {
                    if (a.course.code != b.course.code) initialQueue.add(Arc(a.course.code, b.course.code))
                }
            }
            if (!ac3(initialQueue)) return null
            search()
            return candidates.maxByOrNull { it.score }
        }

        fun stats(elapsedMs: Long): CspSearchStats = CspSearchStats(
            nodes = nodes,
            backtracks = backtracks,
            ac3Revisions = ac3Revisions,
            forwardChecks = forwardChecks,
            durationMs = elapsedMs,
            solutionsFound = solutionsFound
        )

        fun alternatives(best: Candidate): List<CspAlternative> {
            val limit = problem.request.maxAlternatives.coerceIn(0, 5)
            if (limit == 0) return emptyList()
            return candidates
                .sortedByDescending { it.score }
                .distinctBy { it.assignment.map { a -> a.course.code }.sorted() }
                .take(limit + 1)
                .filter { it.assignment.map { a -> a.course.code }.sorted() != best.assignment.map { a -> a.course.code }.sorted() }
                .take(limit)
                .mapIndexed { index, candidate ->
                    CspAlternative(
                        label = "Alternatif ${index + 1}",
                        totalSks = candidate.totalSks,
                        deltaFromTarget = candidate.totalSks - problem.effectiveTargetSks,
                        items = candidate.assignment.map { CspAlternativeItem(it.course.code, it.offering.classCode) }
                    )
                }
        }

        // --- Domain construction -------------------------------------------------

        private fun buildDomain(variable: CspVariable): MutableList<ClassOffering?> {
            val seats = variable.domain.filter { it.quota - it.enrolled > 0 }
            val locked = if (variable.komLocked && kom != null) {
                seats.filter { it.classCode.equals(kom, true) }
            } else {
                seats
            }
            val ordered = locked.sortedWith(
                compareBy<ClassOffering> { if (kom != null && it.classCode.equals(kom, true)) 0 else 1 }
                    .thenBy { it.classCode }
                    .thenBy { it.day?.name ?: "~" }
                    .thenBy { it.startTime ?: "~" }
            )
            val values = ArrayList<ClassOffering?>(ordered.size + 1)
            values.addAll(ordered)
            // Every variable may be left out. A KRS is valid when it breaks no rule, not when
            // it is full: a mandatory course that cannot be scheduled (prodi-side problem)
            // is reported in `unassignedRequired` instead of making the whole draft fail.
            values.add(null)
            return values
        }

        // --- Search --------------------------------------------------------------

        private fun search() {
            if (stopped) return
            nodes++
            if (nodes > budget) {
                stopped = true
                return
            }

            val next = selectVariable()
            if (next == null) {
                recordSolution()
                return
            }

            val domain = domains.getValue(next.course.code)
            val values = ArrayList(domain)
            for (value in values) {
                if (stopped) return
                if (!globallyConsistent(next.course.code, value)) continue

                assigned[next.course.code] = value
                val snapshot = snapshot()

                if (mac(next.course.code, value)) {
                    search()
                } else {
                    backtracks++
                }

                restore(snapshot)
                assigned.remove(next.course.code)
            }
        }

        /** MAC: prune neighbours of [code] and propagate arc consistency. */
        private fun mac(code: String, value: ClassOffering?): Boolean {
            val queue = ArrayDeque<Arc>()
            variables.forEach { other ->
                if (other.course.code != code && !assigned.containsKey(other.course.code)) {
                    queue.add(Arc(other.course.code, code))
                }
            }
            // The assigned variable itself is fixed to one value.
            domains[code] = mutableListOf(value)
            return ac3(queue)
        }

        private fun ac3(queue: ArrayDeque<Arc>): Boolean {
            while (queue.isNotEmpty()) {
                val arc = queue.removeFirst()
                if (revise(arc)) {
                    val domain = domains.getValue(arc.from)
                    if (domain.isEmpty()) return false
                    variables.forEach { other ->
                        val otherCode = other.course.code
                        if (otherCode != arc.from && otherCode != arc.to &&
                            !assigned.containsKey(otherCode)
                        ) {
                            queue.add(Arc(otherCode, arc.from))
                        }
                    }
                }
            }
            return true
        }

        /** Removes values of Xi that have no compatible support in Xj. */
        private fun revise(arc: Arc): Boolean {
            val di = domains.getValue(arc.from)
            val dj = domains.getValue(arc.to)
            val removed = di.removeAll { vi -> dj.none { vj -> compatible(vi, vj) } }
            if (removed) {
                ac3Revisions++
                if (assigned.containsKey(arc.from)) forwardChecks++
            }
            return removed
        }

        private fun selectVariable(): CspVariable? =
            variables
                .filter { !assigned.containsKey(it.course.code) }
                .minWithOrNull(
                    compareBy<CspVariable> { domains.getValue(it.course.code).size }
                        .thenBy { if (it.role == CourseRole.REQUIRED) 0 else 1 }
                        .thenBy { it.course.code }
                )

        /** Early check of the global constraints that can already be evaluated. */
        private fun globallyConsistent(code: String, value: ClassOffering?): Boolean {
            val currentSks = assigned.entries
                .filter { it.value != null }
                .sumOf { byCode.getValue(it.key).course.sks }
            val candidateSks = value?.let { byCode.getValue(code).course.sks } ?: 0
            if (currentSks + candidateSks > sksBudget) return false

            if (problem.requiredElectiveCount > 0) {
                val elected = assigned.entries.count { (key, v) ->
                    v != null && key in problem.electiveCourseCodes
                } + if (value != null && code in problem.electiveCourseCodes) 1 else 0
                if (elected > problem.requiredElectiveCount) return false
            }
            return true
        }

        private fun recordSolution() {
            // Every variable is assigned, but the value may be null (left out).
            val picked = assigned.entries
                .mapNotNull { (code, value) -> value?.let { byCode.getValue(code) to it } }
            if (picked.isEmpty()) return

            val totalSks = picked.sumOf { it.first.course.sks }
            if (totalSks > sksBudget) return

            // "At most N" electives stays a hard rule; "exactly N" does not. An elective
            // that cannot be placed leaves the draft valid, just incomplete.
            if (problem.requiredElectiveCount > 0) {
                val elected = picked.count { it.first.course.code in problem.electiveCourseCodes }
                if (elected > problem.requiredElectiveCount) return
            }

            if (!practicalPairingSatisfied(picked.map { it.first.course.code }.toSet())) return

            solutionsFound++

            val assignments = picked
                .sortedBy { it.first.course.code }
                .map { (variable, offering) ->
                    CspAssignment(
                        course = variable.course,
                        offering = offering,
                        komSwitched = kom != null && !offering.classCode.equals(kom, true)
                    )
                }

            val placedRequired = picked.count { it.first.role == CourseRole.REQUIRED }
            val interest = picked.sumOf { problem.request.interestWeights[it.first.course.code] ?: 0.0 }
            val switches = assignments.count { it.komSwitched }
            // Priority is how much future work the course unlocks, not how many courses fit:
            // a 3-SKS prerequisite for four later courses must beat three unrelated 1-SKS
            // courses. Ties fall back to required count, then SKS, then interest.
            val unlocked = picked
                .filter { it.first.role == CourseRole.REQUIRED }
                .sumOf { it.first.importance }
            // Covering the D shortfall is a graduation requirement, so it outranks
            // importance. Only the first dRepeatMinSks SKS count, otherwise the solver
            // would fill the whole budget with D repeats.
            val dCovered = picked
                .filter { it.first.course.code in problem.dRepeatCourseCodes }
                .sumOf { it.first.course.sks }
            val dCredit = minOf(dCovered, problem.dRepeatMinSks)
            val score = dCredit * 100_000_000_000_000.0 +
                unlocked * 1_000_000_000_000.0 +
                placedRequired * 1_000_000_000.0 +
                totalSks * 1_000_000.0 +
                interest * 10_000.0 -
                switches * 100.0

            candidates += Candidate(assignments, totalSks, score)

            if (objective == CspObjective.FEASIBILITY_ONLY) stopped = true
        }

        private fun practicalPairingSatisfied(selected: Set<String>): Boolean {
            val practicals = selected.mapNotNull { byCode[it] }.filter { AcademicRules.isPractical(it.course) }
            if (practicals.isEmpty()) return true
            return practicals.all { practical ->
                val theory = AcademicRules.theoryOf(practical.course, problem.catalog)
                    ?: return@all true
                theory.code in selected || theory.code in problem.passedCourseCodes
            }
        }

        private fun snapshot(): Map<String, MutableList<ClassOffering?>> =
            domains.mapValues { (_, values) -> ArrayList(values) }

        private fun restore(snapshot: Map<String, MutableList<ClassOffering?>>) {
            snapshot.forEach { (code, values) -> domains[code] = ArrayList(values) }
        }
    }

    private data class Arc(val from: String, val to: String)

    data class Candidate(
        val assignment: List<CspAssignment>,
        val totalSks: Int,
        val score: Double
    )

    companion object {
        /** Two class offerings conflict when they share a day and overlap in time. */
        fun compatible(a: ClassOffering?, b: ClassOffering?): Boolean {
            if (a == null || b == null) return true
            if (a.day == null || b.day == null) return true
            if (a.day != b.day) return true
            val aStart = AcademicRules.timeToMinutes(a.startTime) ?: return true
            val aEnd = AcademicRules.timeToMinutes(a.endTime) ?: return true
            val bStart = AcademicRules.timeToMinutes(b.startTime) ?: return true
            val bEnd = AcademicRules.timeToMinutes(b.endTime) ?: return true
            return !(aStart < bEnd && bStart < aEnd)
        }

        private const val PROBE_NODE_BUDGET = 20_000
        private const val MAX_DROP_TESTS = 6
    }
}
