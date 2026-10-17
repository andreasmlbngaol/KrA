package id.andreasmlbngaol.kra.academic.domain.solver

import id.andreasmlbngaol.kra.academic.domain.model.AcademicTerm
import id.andreasmlbngaol.kra.academic.domain.model.ClassOffering
import id.andreasmlbngaol.kra.academic.domain.model.Course
import id.andreasmlbngaol.kra.academic.domain.model.csp.CourseRole
import id.andreasmlbngaol.kra.academic.domain.model.csp.CspConstraintType
import id.andreasmlbngaol.kra.academic.domain.model.csp.CspDiagnosticCode
import id.andreasmlbngaol.kra.academic.domain.model.csp.CspProblem
import id.andreasmlbngaol.kra.academic.domain.model.csp.CspRelaxationAction
import id.andreasmlbngaol.kra.academic.domain.model.csp.CspStatus
import id.andreasmlbngaol.kra.academic.domain.model.csp.CspVariable
import id.andreasmlbngaol.kra.academic.domain.model.csp.KrsCspRequest
import id.andreasmlbngaol.kra.core.enums.CourseType
import id.andreasmlbngaol.kra.core.enums.Day
import id.andreasmlbngaol.kra.core.enums.SemesterType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * Pure solver tests — no Spring, no database. Each test builds a small CSP instance and
 * asserts on the solution or on the diagnostics, so the CSP layer is verifiable
 * independently of the agent.
 */
class KrsCspSolverTest {

    private val solver = KrsCspSolver()

    // --- Model builders ------------------------------------------------------

    private fun course(
        code: String,
        sks: Int = 3,
        normalSemester: Int = 3,
        prerequisites: List<String> = emptyList(),
        minSks: Int = 0
    ) = Course(
        code = code,
        name = "Mata Kuliah $code",
        sks = sks,
        normalSemester = normalSemester,
        type = CourseType.REGULAR_MANDATORY,
        minSks = minSks,
        prerequisites = prerequisites
    )

    private fun offering(
        courseCode: String,
        classCode: String = "A",
        day: Day? = null,
        start: String? = null,
        end: String? = null,
        quota: Int = 40,
        enrolled: Int = 0
    ) = ClassOffering(
        id = UUID.randomUUID(),
        courseCode = courseCode,
        classCode = classCode,
        term = AcademicTerm(id = 0, year = 2026, semesterType = SemesterType.GANJIL),
        room = "R1",
        day = day,
        startTime = start,
        endTime = end,
        quota = quota,
        enrolled = enrolled
    )

    private fun problem(
        variables: List<CspVariable>,
        target: Int = 24,
        mkwu: Int = 0,
        electiveCount: Int = 0,
        kom: String? = null,
        electiveCodes: Set<String> = emptySet(),
        dRepeatCodes: Set<String> = emptySet(),
        dRepeatMin: Int = 0,
        dSks: Int = 0
    ) = CspProblem(
        request = KrsCspRequest(
            nim = "230001",
            semester = 3,
            requiredCourseCodes = variables.filter { it.role == CourseRole.REQUIRED }.map { it.course.code },
            optionalCourseCodes = variables.filter { it.role == CourseRole.OPTIONAL }.map { it.course.code },
            studentClassCode = kom
        ),
        variables = variables,
        maxSks = 24,
        mkwuReservedSks = mkwu,
        effectiveTargetSks = target,
        requiredElectiveCount = electiveCount,
        passedCourseCodes = emptySet(),
        catalog = variables.map { it.course },
        electiveCourseCodes = electiveCodes,
        dRepeatCourseCodes = dRepeatCodes,
        dRepeatMinSks = dRepeatMin,
        dSks = dSks
    )

    private fun required(code: String, sks: Int = 3, offerings: List<ClassOffering>) =
        CspVariable(course = course(code, sks), role = CourseRole.REQUIRED, domain = offerings)

    private fun optional(code: String, sks: Int = 3, offerings: List<ClassOffering>) =
        CspVariable(course = course(code, sks), role = CourseRole.OPTIONAL, domain = offerings)

    // --- Tests ---------------------------------------------------------------

    @Test
    fun `assigns every required course when schedules do not clash`() {
        val problem = problem(
            listOf(
                required("ILK3101", offerings = listOf(offering("ILK3101", "A", Day.MONDAY, "08:00", "10:00"))),
                required("ILK3102", offerings = listOf(offering("ILK3102", "A", Day.TUESDAY, "08:00", "10:00")))
            )
        )

        val solution = solver.solve(problem)

        assertEquals(CspStatus.FEASIBLE, solution.status)
        assertEquals(6, solution.totalSks)
        assertEquals(2, solution.assignments.size)
        // AC-3 runs on the initial domains; with no conflicting value there is nothing to
        // revise, but the search must still have visited nodes.
        assertTrue(solution.stats.nodes > 0)
    }

    @Test
    fun `picks an alternative class to avoid a schedule conflict`() {
        val problem = problem(
            listOf(
                required(
                    "ILK3101",
                    offerings = listOf(
                        offering("ILK3101", "A", Day.MONDAY, "08:00", "10:00"),
                        offering("ILK3101", "B", Day.MONDAY, "13:00", "15:00")
                    )
                ),
                required("ILK3102", offerings = listOf(offering("ILK3102", "A", Day.MONDAY, "08:00", "10:00")))
            )
        )

        val solution = solver.solve(problem)

        assertEquals(CspStatus.FEASIBLE, solution.status)
        val first = solution.assignments.first { it.course.code == "ILK3101" }
        assertEquals("B", first.offering.classCode)
    }

    @Test
    fun `reports a schedule conflict when no class combination is compatible`() {
        val problem = problem(
            listOf(
                required("ILK3101", offerings = listOf(offering("ILK3101", "A", Day.MONDAY, "08:00", "10:00"))),
                required("ILK3102", offerings = listOf(offering("ILK3102", "A", Day.MONDAY, "09:00", "11:00")))
            )
        )

        val solution = solver.solve(problem)

        // A KRS is valid when it breaks no rule, not when it is full: the course that cannot
        // be scheduled is reported, the rest of the draft still stands.
        assertEquals(CspStatus.FEASIBLE, solution.status)
        assertEquals(1, solution.assignments.size)
        assertEquals(1, solution.unassignedRequired.size)
        assertTrue(solution.diagnostics.any { it.code == CspDiagnosticCode.SCHEDULE_CONFLICT })
    }

    @Test
    fun `reports budget exceeded when mandatory SKS exceed the target`() {
        val problem = problem(
            variables = listOf(
                required("ILK3101", 3, listOf(offering("ILK3101", "A", Day.MONDAY, "08:00", "10:00"))),
                required("ILK3102", 3, listOf(offering("ILK3102", "A", Day.TUESDAY, "08:00", "10:00")))
            ),
            target = 5
        )

        val solution = solver.solve(problem)

        assertEquals(CspStatus.FEASIBLE, solution.status)
        assertEquals(3, solution.totalSks)
        assertEquals(1, solution.unassignedRequired.size)
        assertTrue(solution.diagnostics.any { it.code == CspDiagnosticCode.BUDGET_EXCEEDED })
    }

    @Test
    fun `locks required courses to the student's KOM`() {
        val problem = problem(
            variables = listOf(
                required("ILK3101", offerings = listOf(offering("ILK3101", "B", Day.MONDAY, "08:00", "10:00")))
            ),
            kom = "A"
        )

        val solution = solver.solve(problem)

        assertEquals(CspStatus.INFEASIBLE, solution.status)
        assertTrue(solution.diagnostics.any { it.code == CspDiagnosticCode.KOM_UNAVAILABLE })
        assertTrue(solution.relaxations.any { it.action == CspRelaxationAction.SWITCH_KOM })
    }

    @Test
    fun `prefers the courses that unlock the most later courses`() {
        val problem = problem(
            variables = listOf(
                required("ILK2203", 3, listOf(offering("ILK2203", "A", Day.MONDAY, "08:00", "10:00")))
                    .copy(importance = 5),
                required("ILK2208", 3, listOf(offering("ILK2208", "A", Day.TUESDAY, "08:00", "10:00")))
                    .copy(importance = 4),
                required("ILK2207", 3, listOf(offering("ILK2207", "A", Day.WEDNESDAY, "08:00", "10:00")))
                    .copy(importance = 1)
            ),
            target = 6
        )

        val solution = solver.solve(problem)

        assertEquals(CspStatus.FEASIBLE, solution.status)
        assertEquals(setOf("ILK2203", "ILK2208"), solution.assignments.map { it.course.code }.toSet())
        assertEquals(listOf("ILK2207"), solution.unassignedRequired.map { it.code })
    }

    @Test
    fun `keeps a partial draft when a mandatory course has no class in the student's KOM`() {
        val problem = problem(
            variables = listOf(
                required("ILK3101", offerings = listOf(offering("ILK3101", "B", Day.MONDAY, "08:00", "10:00"))),
                required("ILK3102", offerings = listOf(offering("ILK3102", "A", Day.TUESDAY, "08:00", "10:00")))
            ),
            kom = "A"
        )

        val solution = solver.solve(problem)

        // A prodi-side problem must not block the whole KRS: the course that has no class in
        // the student's KOM is reported, the rest of the draft is still saveable.
        assertEquals(CspStatus.FEASIBLE, solution.status)
        assertEquals(listOf("ILK3102"), solution.assignments.map { it.course.code })
        assertEquals(listOf("ILK3101"), solution.unassignedRequired.map { it.code })
        assertTrue(solution.diagnostics.any { it.code == CspDiagnosticCode.KOM_UNAVAILABLE })
    }

    @Test
    fun `reports a full class as a quota failure`() {
        val problem = problem(
            listOf(
                required(
                    "ILK3101",
                    offerings = listOf(offering("ILK3101", "A", Day.MONDAY, "08:00", "10:00", quota = 40, enrolled = 40))
                )
            )
        )

        val solution = solver.solve(problem)

        assertEquals(CspStatus.INFEASIBLE, solution.status)
        assertTrue(solution.diagnostics.any { it.code == CspDiagnosticCode.QUOTA_FULL })
    }

    @Test
    fun `skips optional courses when the budget is already full`() {
        val problem = problem(
            variables = listOf(
                required("ILK3101", 3, listOf(offering("ILK3101", "A", Day.MONDAY, "08:00", "10:00"))),
                optional("ILK3201", 3, listOf(offering("ILK3201", "A", Day.TUESDAY, "08:00", "10:00")))
            ),
            target = 3
        )

        val solution = solver.solve(problem)

        assertEquals(CspStatus.FEASIBLE, solution.status)
        assertEquals(3, solution.totalSks)
        assertTrue(solution.assignments.none { it.course.code == "ILK3201" })
    }

    @Test
    fun `takes optional courses when the budget still allows it`() {
        val problem = problem(
            variables = listOf(
                required("ILK3101", 3, listOf(offering("ILK3101", "A", Day.MONDAY, "08:00", "10:00"))),
                optional("ILK3201", 3, listOf(offering("ILK3201", "A", Day.TUESDAY, "08:00", "10:00")))
            ),
            target = 6
        )

        val solution = solver.solve(problem)

        assertEquals(CspStatus.FEASIBLE, solution.status)
        assertEquals(6, solution.totalSks)
    }

    @Test
    fun `reserves MKWU SKS from the course budget`() {
        val problem = problem(
            variables = listOf(
                required("ILK3101", 3, listOf(offering("ILK3101", "A", Day.MONDAY, "08:00", "10:00"))),
                required("ILK3102", 3, listOf(offering("ILK3102", "A", Day.TUESDAY, "08:00", "10:00")))
            ),
            target = 8,
            mkwu = 4
        )

        val solution = solver.solve(problem)

        assertEquals(CspStatus.FEASIBLE, solution.status)
        assertEquals(3, solution.totalSks)
        assertEquals(1, solution.unassignedRequired.size)
        assertTrue(solution.diagnostics.any { it.code == CspDiagnosticCode.BUDGET_EXCEEDED })
    }

    @Test
    fun `electives may use a merged class from another KOM`() {
        val problem = problem(
            variables = listOf(
                CspVariable(
                    course = course("ILK3109", 3).copy(type = CourseType.REGULAR_ELECTIVE),
                    role = CourseRole.REQUIRED,
                    domain = listOf(offering("ILK3109", "A", Day.MONDAY, "13:50", "16:20")),
                    komLocked = false
                )
            ),
            kom = "B"
        )

        val solution = solver.solve(problem)

        assertEquals(CspStatus.FEASIBLE, solution.status)
        assertEquals(1, solution.assignments.size)
        assertEquals("A", solution.assignments.single().offering.classCode)
        assertTrue(solution.assignments.single().komSwitched)
    }

    @Test
    fun `requires exactly one elective from the elective group`() {
        val problem = problem(
            variables = listOf(
                required("ILK3101", 3, listOf(offering("ILK3101", "A", Day.MONDAY, "08:00", "10:00"))),
                optional("ILK3301", 3, listOf(offering("ILK3301", "A", Day.TUESDAY, "08:00", "10:00"))),
                optional("ILK3302", 3, listOf(offering("ILK3302", "A", Day.WEDNESDAY, "08:00", "10:00")))
            ),
            target = 24,
            electiveCount = 1,
            electiveCodes = setOf("ILK3301", "ILK3302")
        )

        val solution = solver.solve(problem)

        assertEquals(CspStatus.FEASIBLE, solution.status)
        assertEquals(1, solution.assignments.count { it.course.code in setOf("ILK3301", "ILK3302") })
    }

    @Test
    fun `capacity reports the achievable SKS and the binding constraint`() {
        val problem = problem(
            variables = listOf(
                required("ILK3101", 3, listOf(offering("ILK3101", "A", Day.MONDAY, "08:00", "10:00"))),
                optional("ILK3201", 3, listOf(offering("ILK3201", "A", Day.MONDAY, "09:00", "11:00")))
            ),
            target = 24
        )

        val capacity = solver.capacity(problem)

        assertEquals(24, capacity.theoreticalMaxSks)
        assertEquals(3, capacity.maxAchievableSks)
        assertTrue(capacity.bindingConstraints.contains(CspConstraintType.SCHEDULE_CONFLICT))
    }

    @Test
    fun `tbd schedules never conflict`() {
        val problem = problem(
            listOf(
                required("ILK3101", offerings = listOf(offering("ILK3101", "A"))),
                required("ILK3102", offerings = listOf(offering("ILK3102", "A")))
            )
        )

        val solution = solver.solve(problem)

        assertEquals(CspStatus.FEASIBLE, solution.status)
        assertFalse(solution.assignments.isEmpty())
    }

    @Test
    fun `covers the D shortfall before filling other optional courses`() {
        val problem = problem(
            listOf(
                optional("ILK2203", 3, listOf(offering("ILK2203"))),
                optional("ILK3101", 3, listOf(offering("ILK3101"))),
                optional("ILK3102", 3, listOf(offering("ILK3102")))
            ),
            target = 3,
            dRepeatCodes = setOf("ILK2203"),
            dRepeatMin = 3,
            dSks = 11
        )

        val solution = solver.solve(problem)

        assertEquals(CspStatus.FEASIBLE, solution.status)
        assertEquals(listOf("ILK2203"), solution.assignments.map { it.course.code })
        assertEquals(3, solution.dRepeatCoveredSks)
        assertTrue(solution.diagnostics.none { it.code == CspDiagnosticCode.D_GRADE_LIMIT })
    }

    @Test
    fun `reports the D shortfall without invalidating the draft`() {
        val problem = problem(
            listOf(
                optional("ILK2203", 3, emptyList()),
                optional("ILK3101", 3, listOf(offering("ILK3101")))
            ),
            target = 6,
            dRepeatCodes = setOf("ILK2203"),
            dRepeatMin = 3,
            dSks = 11
        )

        val solution = solver.solve(problem)

        assertEquals(CspStatus.FEASIBLE, solution.status)
        assertEquals(listOf("ILK3101"), solution.assignments.map { it.course.code })
        assertEquals(0, solution.dRepeatCoveredSks)
        assertTrue(solution.diagnostics.any { it.code == CspDiagnosticCode.D_GRADE_LIMIT })
    }
}
