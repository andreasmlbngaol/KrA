package id.andreasmlbngaol.kra.academic.domain.repository

import id.andreasmlbngaol.kra.academic.domain.model.Khs

/** Read/write access to student KHS (transcript) records. */
interface KhsRepository {
    /** Returns all KHS entries for a student. */
    fun getKhsByNim(nim: String): List<Khs>

    /** Returns the cumulative passed SKS (grades E and T excluded). */
    fun getTotalPassedSks(nim: String): Int

    /** Returns the KHS entries a student took in a given semester. */
    fun getKhsBySemester(nim: String, semester: Int): List<Khs>

    /** Returns the codes of all courses the student has passed. */
    fun getPassedCourseCodes(nim: String): List<String>

    /** Inserts a KHS entry; returns true on success. */
    fun addKhs(khs: Khs): Boolean

    /** Deletes a specific KHS entry; returns true when a row was removed. */
    fun deleteKhs(nim: String, courseCode: String, semesterTaken: Int): Boolean
}
