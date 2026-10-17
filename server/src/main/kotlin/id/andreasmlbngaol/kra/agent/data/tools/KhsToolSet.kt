package id.andreasmlbngaol.kra.agent.data.tools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import org.springframework.stereotype.Component
import id.andreasmlbngaol.kra.academic.domain.model.rules.AcademicRules
import id.andreasmlbngaol.kra.academic.domain.usecase.CourseUseCase
import id.andreasmlbngaol.kra.academic.domain.usecase.KhsUseCase
import id.andreasmlbngaol.kra.core.enums.Grade

/**
 * Agent ToolSet exposing read-only KHS (transcript) operations to the LLM: full grade
 * history, semester IP with next-semester SKS limit, cumulative passed SKS, and
 * repeatable-course analysis.
 */
@Component
class KhsToolSet(
    private val khsUseCase: KhsUseCase,
    private val courseUseCase: CourseUseCase
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

    /** Analyzes which courses can or must be repeated, filtered to active-term offerings. */
    @Tool
    @LLMDescription(
        "Daftar matkul non-A yang bisa/perlu diulang, dianalisa otomatis dari nilai terbaik per matkul, sudah difilter ke matkul yang punya kelas/offering di term aktif. " +
            "mustRepeat=true (nilai terbaik E/T) WAJIB diulang. " +
            "ATURAN NILAI D: total SKS bernilai D maksimal 8. Field dGradeSks = total D saat ini, dGradeMaxSks = 8, dRepeatMinSks = SKS D yang WAJIB diulang. " +
            "dRepeatMinSks > 0 berarti mengulang D bukan lagi opsional: mahasiswa harus mengulang matkul D minimal sebanyak angka itu. " +
            "dRepeatMandatory=true menandai matkul D yang termasuk kelompok wajib diulang tersebut. " +
            "recommendedForRepeat=true = nilai C+ ke bawah (C+/C/D), ambang yang DISARANKAN ditawarkan proaktif. " +
            "recommendedForRepeat=false = nilai B/B+, JANGAN ditawarkan proaktif; hanya untuk isi kuota MBKM atau bila diminta eksplisit. " +
            "importance = 1 + jumlah matkul wajib lain yang belum lulus dan terbuka setelah matkul ini lulus. unlocksCount = jumlah matkul itu, unlocks = daftarnya (courseCode, courseName, normalSemester). " +
            "Saat menawarkan matkul ulang, WAJIB jelaskan importance dan berapa matkul lain yang dibukanya memakai unlocks, sebutkan courseName-nya, JANGAN kodenya. Jangan mengarang. " +
            "Pakai mbkmEligible & recognitionSemester apa adanya. " +
            "JSON {totalCourses, dGradeSks, dGradeMaxSks, dRepeatMinSks, courses:[{code, name, sks, bestGrade, mustRepeat, dRepeatMandatory, recommendedForRepeat, status, importance, unlocksCount, unlocks, mbkmEligible, recognitionSemester}]}."
    )
    suspend fun getRepeatableCourses(
        @LLMDescription("Nomor Induk Mahasiswa") nim: String,
        @LLMDescription("Semester mahasiswa saat ini") semester: Int
    ): String {
        logCall("getRepeatableCourses", "nim=$nim, semester=$semester")
        val dSks = courseUseCase.getDSks(nim)
        val dRepeatMin = AcademicRules.dRepeatMinSks(dSks)
        val importance = courseUseCase.getImportance(nim)
        val dtos = khsUseCase.getRepeatableCourses(nim, semester).map {
            val info = importance[it.code]
            RepeatableCourseDto(
                code = it.code,
                name = it.name,
                sks = it.sks,
                bestGrade = it.bestGrade,
                mustRepeat = it.mustRepeat,
                dRepeatMandatory = it.bestGrade == Grade.D && dRepeatMin > 0,
                recommendedForRepeat = it.recommendedForRepeat,
                status = when {
                    it.mustRepeat -> "WAJIB diulang (nilai terbaik E/T)"
                    it.bestGrade == Grade.D && dRepeatMin > 0 ->
                        "Wajib diulang sebagian: total nilai D $dSks SKS melebihi batas " +
                            "${AcademicRules.MAX_D_SKS} SKS, minimal $dRepeatMin SKS D harus diulang"
                    it.recommendedForRepeat -> "Opsional - disarankan bila ingin perbaikan (C+ ke bawah)"
                    else -> "Opsional - B/B+, di luar ambang proaktif (hanya untuk isi kuota MBKM atau bila diminta)"
                },
                importance = info?.score ?: 1,
                unlocksCount = info?.unlockedCourses?.size ?: 0,
                unlocks = info?.unlockedCourses.orEmpty().map { course ->
                    UnlockedCourseDto(course.code, course.name, course.normalSemester)
                },
                mbkmEligible = it.mbkmEligible,
                recognitionSemester = if (it.mbkmEligible) it.recognitionSemester else null
            )
        }
        return logResult(
            "getRepeatableCourses",
            RepeatableCoursesResponse(
                totalCourses = dtos.size,
                dGradeSks = dSks,
                dGradeMaxSks = AcademicRules.MAX_D_SKS,
                dRepeatMinSks = dRepeatMin,
                courses = dtos
            ).toJson()
        )
    }
}
