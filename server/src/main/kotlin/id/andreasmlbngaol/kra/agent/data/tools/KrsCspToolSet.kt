package id.andreasmlbngaol.kra.agent.data.tools

import id.andreasmlbngaol.kra.agent.data.session.SessionTokenRegistry
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import id.andreasmlbngaol.kra.academic.domain.model.csp.CspAssignment
import id.andreasmlbngaol.kra.academic.domain.model.csp.CspDiagnostic
import id.andreasmlbngaol.kra.academic.domain.model.csp.CspRelaxation
import id.andreasmlbngaol.kra.academic.domain.model.csp.CspSearchStats
import id.andreasmlbngaol.kra.academic.domain.model.csp.CspSolution
import id.andreasmlbngaol.kra.academic.domain.model.csp.KrsCapacityResult
import id.andreasmlbngaol.kra.academic.domain.model.csp.KrsCspRequest
import id.andreasmlbngaol.kra.academic.domain.usecase.KrsCspUseCase
import org.springframework.stereotype.Component

/**
 * Agent tools backed by the deterministic CSP solver.
 *
 * These are the ONLY tools allowed to produce a course/class combination. The LLM decides
 * *what* the student wants (required vs optional courses, interest weights, target SKS);
 * the solver decides *whether* and *how* it fits.
 */
@Component
class KrsCspToolSet(
    private val krsCspUseCase: KrsCspUseCase,
    sessions: SessionTokenRegistry
) : LoggingToolSet(sessions) {

    @Tool
    @LLMDescription(
        "Menyusun kombinasi KRS secara DETERMINISTIK dengan solver CSP (AC-3 + MAC + backtracking). " +
            "Ini SATU-SATUNYA sumber kombinasi mata kuliah dan kelas. JANGAN menyusun atau menebak kombinasi sendiri. " +
            "Solver memilih kelas/KOM, menghindari bentrok jadwal, menegakkan prasyarat, kuota, batas SKS, jumlah matkul pilihan, " +
            "dan aturan praktikum-butuh-teori. " +
            "requiredCourseCodes = matkul yang WAJIB masuk (wajib prodi + matkul ulang yang dipilih + matkul pilihan wajib). " +
            "optionalCourseCodes = kandidat tambahan yang boleh diambil bila kuota/jadwal memungkinkan. " +
            "excludedCourseCodes = matkul yang harus dihindari. " +
            "studentClassCode = KOM mahasiswa (A/B/C). HANYA matkul wajib semester berjalan yang dikunci ke KOM ini; " +
            "matkul pilihan, matkul ulang, dan matkul semester atas boleh memakai kelas KOM lain bila itu satu-satunya kelas yang dibuka. " +
            "targetSks = target total SKS termasuk MKWU; isi 0 untuk batas maksimal. " +
            "mbkmMode = true bila mahasiswa mengambil MBKM (batas menjadi min(20, batas IP)). " +
            "interestWeights = bobot minat 0..1 per kode matkul, format 'ILK3101:0.9,ILK3102:0.4' (boleh kosong). " +
            "maxAlternatives = jumlah alternatif yang diminta (0-3). " +
            "JSON {status, totalSks, maxSks, effectiveTargetSks, mkwuReservedSks, remainingSks, " +
            "assignments:[{courseCode, courseName, sks, classOfferingId, classCode, day, startTime, endTime, room, komSwitched}], " +
            "unassignedRequired, diagnostics:[{code, constraint, courseCodes, detail}], " +
            "relaxations:[{action, courseCodes, gainSks, detail}], alternatives, search}. " +
            "status FEASIBLE = draft valid (pakai assignments apa adanya). Draft TIDAK harus penuh: jika unassignedRequired tidak kosong, " +
            "draft tetap sah. Sebutkan matkul yang belum bisa masuk beserta alasannya di diagnostics, lalu tawarkan simpan. " +
            "status INFEASIBLE = tidak ada satu pun matkul yang bisa ditempatkan; sampaikan diagnostics/relaxations, JANGAN mengarang kombinasi. " +
            "ATURAN NILAI D: total SKS bernilai D maksimal 8. dRepeatMinSks > 0 berarti mengulang D wajib, dan draft sebaiknya memuat minimal SKS itu; " +
            "dRepeatCoveredSks = SKS D yang sudah diulang di draft. Jika dRepeatCoveredSks < dRepeatMinSks, sebutkan kekurangannya dengan bahasa sederhana, " +
            "tetapi draft tetap sah dan boleh disimpan."
    )
    suspend fun solveKrs(
        @LLMDescription("Token sesi mahasiswa dari konteks sistem, bukan NIM") sessionToken: String,
        @LLMDescription("Semester berjalan") semester: Int,
        @LLMDescription("Kode matkul WAJIB, dipisah koma. Contoh: ILK3101,ILK3102") requiredCourseCodes: String,
        @LLMDescription("Kode matkul OPSIONAL/kandidat tambahan, dipisah koma. Kosongkan ('') bila tidak ada.") optionalCourseCodes: String,
        @LLMDescription("Kode matkul yang harus dihindari, dipisah koma. Kosongkan ('') bila tidak ada.") excludedCourseCodes: String,
        @LLMDescription("KOM mahasiswa: A, B, atau C. Kosongkan ('') bila belum diketahui.") studentClassCode: String,
        @LLMDescription("Target total SKS termasuk MKWU. Isi 0 untuk batas maksimal sesuai IP.") targetSks: Int,
        @LLMDescription("true bila mahasiswa mengambil MBKM semester ini (batas SKS menjadi min(20, batas IP)).") mbkmMode: Boolean,
        @LLMDescription("Bobot minat 0..1 per matkul, format 'ILK3101:0.9,ILK3102:0.4'. Kosongkan ('') bila tidak ada.") interestWeights: String,
        @LLMDescription("Jumlah alternatif solusi yang diminta (0-3).") maxAlternatives: Int
    ): String {
        val nim = resolveNim("solveKrs", sessionToken) ?: return sessionExpired()
        logCall(
            "solveKrs",
            "nim=$nim, semester=$semester, required=$requiredCourseCodes, optional=$optionalCourseCodes, " +
                "excluded=$excludedCourseCodes, kom=$studentClassCode, target=$targetSks, mbkm=$mbkmMode"
        )
        val request = KrsCspRequest(
            nim = nim,
            semester = semester,
            requiredCourseCodes = splitCodes(requiredCourseCodes),
            optionalCourseCodes = splitCodes(optionalCourseCodes),
            excludedCourseCodes = splitCodes(excludedCourseCodes),
            studentClassCode = studentClassCode.takeIf { it.isNotBlank() },
            targetSks = targetSks.coerceAtLeast(0),
            mbkmMode = mbkmMode,
            interestWeights = parseWeights(interestWeights),
            maxAlternatives = maxAlternatives.coerceIn(0, 5)
        )
        return logResult("solveKrs", krsCspUseCase.solve(request).toResponse().toJson())
    }

    @Tool
    @LLMDescription(
        "Menghitung kapasitas SKS REALISTIS mahasiswa semester ini memakai solver CSP: " +
            "berapa SKS yang benar-benar bisa diambil setelah memperhitungkan bentrok jadwal, kuota kelas, KOM, dan sisa matkul wajib. " +
            "Pakai ini SEBELUM menanyakan strategi (maksimalkan vs santai) agar angka yang disebut bukan tebakan. " +
            "JSON {maxSksByIp, mkwuReservedSks, theoreticalMaxSks, maxAchievableSks, requiredSks, bindingConstraints:[...], search}. " +
            "maxSksByIp = batas SKS dari IP; mkwuReservedSks = SKS yang disisihkan untuk MKWU; " +
            "theoreticalMaxSks = kuota untuk draft (= maxSksByIp - mkwuReservedSks); maxAchievableSks = yang realistis; " +
            "bindingConstraints menjelaskan apa yang membatasi (mis. SCHEDULE_CONFLICT, QUOTA, TOTAL_SKS)."
    )
    suspend fun getKrsCapacity(
        @LLMDescription("Token sesi mahasiswa dari konteks sistem, bukan NIM") sessionToken: String,
        @LLMDescription("Semester berjalan") semester: Int,
        @LLMDescription("KOM mahasiswa: A, B, atau C. Kosongkan ('') bila belum diketahui.") studentClassCode: String,
        @LLMDescription("true bila mahasiswa mengambil MBKM semester ini.") mbkmMode: Boolean
    ): String {
        val nim = resolveNim("getKrsCapacity", sessionToken) ?: return sessionExpired()
        logCall("getKrsCapacity", "nim=$nim, semester=$semester, kom=$studentClassCode, mbkm=$mbkmMode")
        val result = krsCspUseCase.capacityForSemester(
            nim = nim,
            semester = semester,
            studentClassCode = studentClassCode.takeIf { it.isNotBlank() },
            mbkmMode = mbkmMode
        )
        return logResult("getKrsCapacity", result.toResponse().toJson())
    }

    // ---------------------------------------------------------------------
    // Mapping
    // ---------------------------------------------------------------------

    private fun CspSolution.toResponse() = SolveKrsResponse(
        status = status,
        totalSks = totalSks,
        maxSks = maxSks,
        effectiveTargetSks = effectiveTargetSks,
        mkwuReservedSks = mkwuReservedSks,
        remainingSks = remainingSks,
        assignments = assignments.map { it.toDto() },
        unassignedRequired = unassignedRequired.map {
            CourseRefDto(courseCode = it.code, courseName = it.name, sks = it.sks)
        },
        diagnostics = diagnostics.map { it.toDto() },
        relaxations = relaxations.map { it.toDto() },
        alternatives = alternatives.map { it.toDto() },
        dSks = dSks,
        dRepeatMinSks = dRepeatMinSks,
        dRepeatCoveredSks = dRepeatCoveredSks,
        search = stats.toDto()
    )

    private fun CspAssignment.toDto() = CspAssignmentDto(
        courseCode = course.code,
        courseName = course.name,
        sks = course.sks,
        classOfferingId = offering.id.toString(),
        classCode = offering.classCode,
        day = offering.day?.name,
        startTime = offering.startTime,
        endTime = offering.endTime,
        room = offering.room,
        komSwitched = komSwitched
    )

    private fun CspDiagnostic.toDto() = CspDiagnosticDto(
        code = code,
        constraint = constraint,
        courseCodes = courseCodes,
        detail = detail
    )

    private fun CspRelaxation.toDto() = CspRelaxationDto(
        action = action,
        courseCodes = courseCodes,
        gainSks = gainSks,
        detail = detail
    )

    private fun id.andreasmlbngaol.kra.academic.domain.model.csp.CspAlternative.toDto() =
        CspAlternativeDto(
            label = label,
            totalSks = totalSks,
            deltaFromTarget = deltaFromTarget,
            items = items.map {
                CspAlternativeItemDto(
                    courseCode = it.courseCode,
                    classCode = it.classCode
                )
            }
        )

    private fun CspSearchStats.toDto() = CspSearchDto(
        nodes = nodes,
        backtracks = backtracks,
        ac3Revisions = ac3Revisions,
        forwardChecks = forwardChecks,
        durationMs = durationMs,
        solutionsFound = solutionsFound
    )

    private fun KrsCapacityResult.toResponse() = KrsCapacityResponse(
        maxSksByIp = maxSksByIp,
        mkwuReservedSks = mkwuReservedSks,
        theoreticalMaxSks = theoreticalMaxSks,
        maxAchievableSks = maxAchievableSks,
        requiredSks = requiredSks,
        bindingConstraints = bindingConstraints,
        search = stats.toDto()
    )

    private fun splitCodes(raw: String): List<String> =
        raw.split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() }.distinct()

    /** Accepts "ILK3101:0.9,ILK3102:0.4" (also tolerates '=' and stray spaces). */
    private fun parseWeights(raw: String): Map<String, Double> =
        raw.split(",").mapNotNull { part ->
            val trimmed = part.trim()
            if (trimmed.isEmpty()) return@mapNotNull null
            val separator = trimmed.indexOfFirst { it == ':' || it == '=' }
            if (separator <= 0) return@mapNotNull null
            val code = trimmed.substring(0, separator).trim().uppercase()
            val value = trimmed.substring(separator + 1).trim().toDoubleOrNull() ?: return@mapNotNull null
            code to value.coerceIn(0.0, 1.0)
        }.toMap()
}
