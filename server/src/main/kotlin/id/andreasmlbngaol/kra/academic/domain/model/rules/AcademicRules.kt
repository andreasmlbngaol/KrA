package id.andreasmlbngaol.kra.academic.domain.model.rules

import id.andreasmlbngaol.kra.academic.domain.model.Course
import id.andreasmlbngaol.kra.core.enums.Grade

/**
 * Single source of truth for the academic rules of Prodi Ilmu Komputer USU.
 *
 * Both the imperative validator ([id.andreasmlbngaol.kra.academic.domain.usecase.KrsValidationUseCase])
 * and the CSP solver ([id.andreasmlbngaol.kra.academic.domain.solver.KrsCspSolver]) MUST read the
 * rules from here. Keeping two independent copies of these numbers is how a solver and a
 * validator silently drift apart.
 */
object AcademicRules {

    /** Capstone courses (Kerja Praktek/Magang and Tugas Akhir). */
    val CAPSTONE_COURSE_CODES: Set<String> = setOf("ILK4202", "ILK4299")

    /** Practical courses are named "Praktikum <nama teori>". */
    const val PRACTICAL_PREFIX = "Praktikum "

    /** A course counts as passed when the grade is neither E nor T. */
    fun isPassed(grade: Grade): Boolean = grade != Grade.E && grade != Grade.T

    /**
     * Maximum SKS with grade D that may still count toward graduation. A student may
     * carry at most this many D SKS; anything above it must be improved by repeating.
     */
    const val MAX_D_SKS = 8

    /**
     * SKS of D that must be repeated so the remaining D load is at most [MAX_D_SKS].
     * Zero means repeating a D course is still optional.
     */
    fun dRepeatMinSks(dSks: Int): Int = (dSks - MAX_D_SKS).coerceAtLeast(0)

    /**
     * Parses a class time ("HH:mm" or "HH.mm") into minutes since midnight.
     *
     * The seed data keeps both separators ("08:00" and "08.00"). Comparing the raw
     * strings makes "10:30" and "10.30" look different and creates false schedule
     * conflicts at boundaries (e.g. 08:00-10:30 vs 10.30-13.00).
     */
    fun timeToMinutes(time: String?): Int? {
        val normalized = time?.trim()?.replace('.', ':') ?: return null
        val parts = normalized.split(':')
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        return hour * 60 + minute
    }

    /**
     * Maximum SKS quota derived from the previous semester's IP.
     * IP >= 3.00 -> 24, >= 2.50 -> 22, >= 2.00 -> 20, >= 1.50 -> 18, otherwise 16.
     */
    fun maxSksForIp(ip: Double): Int = when {
        ip >= 3.00 -> 24
        ip >= 2.50 -> 22
        ip >= 2.00 -> 20
        ip >= 1.50 -> 18
        else -> 16
    }

    /**
     * SKS reserved for MKWU (filled manually by the student in the campus portal,
     * therefore NOT part of the KRS draft but still consuming the quota).
     */
    fun mkwuReservedSks(semester: Int): Int = when (semester) {
        3 -> 4
        4 -> 2
        5 -> 2
        else -> 0
    }

    /** True when [course] is a practical course ("Praktikum ..."). */
    fun isPractical(course: Course): Boolean = course.name.startsWith(PRACTICAL_PREFIX)

    /** The theory course paired with a practical course, if it exists in [catalog]. */
    fun theoryOf(practical: Course, catalog: Collection<Course>): Course? {
        if (!isPractical(practical)) return null
        val theoryName = practical.name.removePrefix(PRACTICAL_PREFIX)
        return catalog.firstOrNull { it.name == theoryName }
    }

    /** True when [course] needs a theory course taken together (or already passed). */
    fun requiresTheory(course: Course, catalog: Collection<Course>, passedCodes: Set<String>): Boolean {
        if (!isPractical(course)) return false
        val theory = theoryOf(course, catalog) ?: return false
        return theory.code !in passedCodes
    }
}
