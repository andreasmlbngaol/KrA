package id.andreasmlbngaol.kra.agent.data.tools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import org.springframework.stereotype.Component
import id.andreasmlbngaol.kra.academic.domain.usecase.KhsUseCase

/**
 * Agent ToolSet exposing read-only KHS (transcript) operations to the LLM: full grade
 * history, semester IP with next-semester SKS limit, cumulative passed SKS, and
 * repeatable-course analysis.
 */
@Component
class KhsToolSet(
    private val khsUseCase: KhsUseCase
) : LoggingToolSet() {

    /** Returns the student's complete grade history (KHS) across all semesters. */
    @Tool
    @LLMDescription(
        "Riwayat nilai (KHS) lengkap mahasiswa dari semua semester. Ambil sendiri, jangan tanya mahasiswa. JSON {nim, totalEntries, entries:[{courseCode, semesterTaken, grade}]}."
    )
    suspend fun getKhs(
        @LLMDescription("Nomor Induk Mahasiswa") nim: String
    ): String {
        logCall("getKhs", "nim=$nim")
        val entries = khsUseCase.getKhsByNim(nim).map {
            KhsEntryDto(courseCode = it.courseCode, semesterTaken = it.semesterTaken, grade = it.grade)
        }
        return logResult(
            "getKhs",
            KhsResponse(nim = nim, totalEntries = entries.size, entries = entries).toJson()
        )
    }

    /** Returns the IP for a semester plus the maximum SKS allowed next semester. */
    @Tool
    @LLMDescription(
        "IP semester tertentu + batas SKS maksimal semester berikutnya (sudah dihitung sistem dari IP; 24 bila belum ada IP sebelumnya). Pakai batas SKS apa adanya, jangan petakan IP->SKS manual. JSON {semester, ip, maxSksNextSemester}."
    )
    suspend fun getIpSemester(
        @LLMDescription("Nomor Induk Mahasiswa") nim: String,
        @LLMDescription("Semester yang ingin dihitung IP-nya (semester sebelum semester berjalan)") semester: Int
    ): String {
        logCall("getIpSemester", "nim=$nim, semester=$semester")
        val ip = khsUseCase.getIpSemester(nim, semester)
        val maxSks = khsUseCase.getMaxSks(nim, semester)
        return logResult(
            "getIpSemester",
            ipResponse(semester = semester, ip = ip, maxSks = maxSks).toJson()
        )
    }

    /** Returns the cumulative passed SKS (grades E and T excluded). */
    @Tool
    @LLMDescription(
        "Total SKS lulus akumulatif (E & T tidak dihitung). Untuk cek syarat Min SKS matkul puncak. Jangan hitung manual. JSON {totalPassedSks}."
    )
    suspend fun getTotalPassedSks(
        @LLMDescription("Nomor Induk Mahasiswa") nim: String
    ): String {
        logCall("getTotalPassedSks", "nim=$nim")
        return logResult(
            "getTotalPassedSks",
            TotalPassedSksResponse(totalPassedSks = khsUseCase.getTotalPassedSks(nim)).toJson()
        )
    }

    /** Analyzes which courses can or must be repeated, filtered to the current parity. */
    @Tool
    @LLMDescription(
        "Daftar matkul non-A yang bisa/perlu diulang, dianalisa otomatis dari nilai terbaik per matkul, sudah difilter ke matkul yang dibuka di paritas semester berjalan. mustRepeat=true (nilai terbaik E/T) wajib diulang. recommendedForRepeat=true = nilai C+ ke bawah (C+/C/D), inilah ambang yang DISARANKAN ditawarkan proaktif di alur normal. recommendedForRepeat=false = nilai B/B+, JANGAN ditawarkan proaktif di alur normal; hanya untuk mengisi sisa kuota MBKM atau bila mahasiswa minta eksplisit. Pakai mbkmEligible & recognitionSemester apa adanya, jangan tentukan sendiri. JSON {totalCourses, courses:[{code, name, sks, bestGrade, mustRepeat, recommendedForRepeat, status, mbkmEligible, recognitionSemester}]}."
    )
    suspend fun getRepeatableCourses(
        @LLMDescription("Nomor Induk Mahasiswa") nim: String,
        @LLMDescription("Semester mahasiswa saat ini") semester: Int
    ): String {
        logCall("getRepeatableCourses", "nim=$nim, semester=$semester")
        val dtos = khsUseCase.getRepeatableCourses(nim, semester).map {
            RepeatableCourseDto(
                code = it.code,
                name = it.name,
                sks = it.sks,
                bestGrade = it.bestGrade,
                mustRepeat = it.mustRepeat,
                recommendedForRepeat = it.recommendedForRepeat,
                status = if (it.mustRepeat) "WAJIB diulang"
                else if (it.recommendedForRepeat) "Opsional - disarankan bila ingin perbaikan (C+ ke bawah)"
                else "Opsional - B/B+, di luar ambang proaktif (hanya untuk isi kuota MBKM atau bila diminta)",
                mbkmEligible = it.mbkmEligible,
                recognitionSemester = if (it.mbkmEligible) it.recognitionSemester else null
            )
        }
        return logResult(
            "getRepeatableCourses",
            RepeatableCoursesResponse(totalCourses = dtos.size, courses = dtos).toJson()
        )
    }
}
