package id.andreasmlbngaol.kra.agent.data.tools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import id.andreasmlbngaol.kra.academic.domain.model.KrsValidationResult
import id.andreasmlbngaol.kra.academic.domain.model.SaveDraftKrsResult
import id.andreasmlbngaol.kra.academic.domain.usecase.CourseUseCase
import id.andreasmlbngaol.kra.academic.domain.usecase.KhsUseCase
import id.andreasmlbngaol.kra.academic.domain.usecase.KrsUseCase
import id.andreasmlbngaol.kra.academic.domain.usecase.KrsValidationUseCase
import java.util.UUID

/**
 * Agent ToolSet exposing KRS draft operations to the LLM: reading the saved draft,
 * computing SKS budgets (regular and MBKM), validating a candidate draft, and saving it.
 * Saving runs validation first and only persists a clean draft.
 */
class KrsToolSet(
    private val courseUseCase: CourseUseCase,
    private val khsUseCase: KhsUseCase,
    private val krsUseCase: KrsUseCase,
    private val krsValidationUseCase: KrsValidationUseCase,
    /**
     * Invoked right after a KRS draft is successfully saved (passed validation). The
     * streaming layer uses it to emit a "draftSaved" event so the UI can show a shortcut.
     * Defaults to a no-op so the tool set can be built without a callback.
     */
    private val onDraftSaved: () -> Unit = {}
) : LoggingToolSet() {

    /** Returns the saved KRS draft for a semester. */
    @Tool
    @LLMDescription(
        "Isi draft KRS tersimpan untuk semester tertentu. Cek di awal apakah sudah ada draft, dan untuk menampilkan isi KRS sebelum diajukan ke Dosen PA. JSON {semester, exists, totalSks, items:[{classOfferingId, courseCode, courseName, classCode, room, day, startTime, endTime, sks}]}. exists=false = belum ada draft; field jadwal null = TBD."
    )
    suspend fun getKrsDraft(
        @LLMDescription("Nomor Induk Mahasiswa") nim: String,
        @LLMDescription("Semester berjalan") semester: Int
    ): String {
        logCall("getKrsDraft", "nim=$nim, semester=$semester")
        val items = krsUseCase.getKrsDraftDetails(nim, semester)
        return logResult(
            "getKrsDraft",
            KrsDraftResponse(
                semester = semester,
                exists = items.isNotEmpty(),
                totalSks = items.sumOf { it.sks },
                items = items.map { it.toDto() }
            ).toJson()
        )
    }

    /**
     * Saves a KRS draft from a comma-separated list of course-class UUIDs. Call only after
     * explicit student confirmation. Validation runs first; if it fails the draft is NOT saved.
     */
    @Tool
    @LLMDescription(
        "Menyimpan draft KRS dari daftar UUID course class (dipisah koma). Panggil HANYA setelah konfirmasi eksplisit mahasiswa. Otomatis memvalidasi dulu: bila bentrok jadwal / pelanggaran prasyarat / over kuota / matkul ganda, draft TIDAK disimpan dan dikembalikan daftar masalahnya. UUID didapat dari getCourseClasses. JSON {status, savedCourseCount, message, validation:{...}}. status SAVED jika tersimpan, REJECTED jika ditolak validasi."
    )
    suspend fun saveDraftKrs(
        @LLMDescription("Nomor Induk Mahasiswa") nim: String,
        @LLMDescription("Daftar UUID course class yang dipilih, dipisahkan koma. Contoh: uuid1,uuid2,uuid3") courseClassIds: String,
        @LLMDescription("Semester berjalan") semester: Int
    ): String {
        logCall("saveDraftKrs", "nim=$nim, semester=$semester, courseClassIds=$courseClassIds")
        return try {
            val ids = courseClassIds.split(",").map { UUID.fromString(it.trim()) }
            when (val result = krsUseCase.saveDraftKrs(nim, ids, semester)) {
                is SaveDraftKrsResult.Success -> logResult(
                    "saveDraftKrs",
                    SaveDraftResponse(
                        status = "SAVED",
                        savedCourseCount = ids.size,
                        message = "Draft KRS berhasil disimpan untuk ${ids.size} matkul (total ${result.validation.totalSks} SKS). " +
                            "Silakan ajukan KRS ke Dosen Pembimbing Akademik untuk mendapatkan persetujuan.",
                        validation = result.validation.toResponse()
                    ).toJson()
                ).also { onDraftSaved() }

                is SaveDraftKrsResult.Rejected -> logResult(
                    "saveDraftKrs",
                    SaveDraftResponse(
                        status = "REJECTED",
                        savedCourseCount = null,
                        message = buildRejectionMessage(result.validation),
                        validation = result.validation.toResponse()
                    ).toJson()
                )
            }
        } catch (e: Exception) {
            logResult(
                "saveDraftKrs",
                ToolErrorResponse(message = "Gagal menyimpan draft KRS: ${e.message}").toJson()
            )
        }
    }

    /** Builds the human-readable rejection message listing every blocking validation issue. */
    private fun buildRejectionMessage(validation: KrsValidationResult): String {
        val builder = StringBuilder()
        builder.appendLine("Draft KRS TIDAK disimpan karena masih ada masalah yang harus diperbaiki dulu:")
        if (validation.remainingSks < 0) {
            builder.appendLine(
                "- Total SKS draft (${validation.totalSks}) melebihi kuota. " +
                    "Batas ${validation.maxSks}, disisihkan untuk MKWU ${validation.mkwuReservedSks}, " +
                    "kelebihan ${-validation.remainingSks} SKS."
            )
        }
        if (validation.scheduleConflicts.isNotEmpty()) {
            builder.appendLine("- Konflik jadwal:")
            validation.scheduleConflicts.forEach {
                builder.appendLine("  • ${it.classA} bentrok dengan ${it.classB} di hari ${it.day}")
            }
        }
        if (validation.prerequisiteViolations.isNotEmpty()) {
            builder.appendLine("- Pelanggaran prasyarat:")
            validation.prerequisiteViolations.forEach {
                builder.appendLine(
                    "  • ${it.courseCode} belum memenuhi prasyarat: ${it.missingPrerequisites.joinToString(", ")}"
                )
            }
        }
        if (validation.duplicateCourses.isNotEmpty()) {
            builder.appendLine("- Mata kuliah ganda (satu matkul tidak boleh diambil lebih dari sekali):")
            validation.duplicateCourses.forEach {
                builder.appendLine(
                    "  • ${it.courseCode} - ${it.courseName} terpilih ${it.classCodes.size}x (kelas: ${it.classCodes.joinToString(", ")})"
                )
            }
        }
        if (!validation.electiveRequirement.satisfied) {
            val opsi = validation.electiveRequirement.options.joinToString(", ") {
                "${it.code} - ${it.name} (${it.sks} SKS)"
            }
            builder.appendLine(
                "- Matkul pilihan wajib belum dipilih: semester ini WAJIB memilih tepat " +
                    "${validation.electiveRequirement.requiredChoose} matkul pilihan, " +
                    "tapi baru ${validation.electiveRequirement.selectedCount} yang dipilih. " +
                    "Pilihan tersedia: $opsi."
            )
        }
        builder.appendLine("Perbaiki masalah di atas, lalu minta konfirmasi mahasiswa dan coba simpan kembali.")
        return builder.toString().trim()
    }

    /** Computes the remaining SKS budget for new upper/extra courses. */
    @Tool
    @LLMDescription(
        "Sisa kuota SKS untuk matkul atas/tambahan BARU; otomatis memperhitungkan batas IP, SKS disisihkan MKWU, dan SEMUA matkul yang sudah ada di draft. targetSks = target mahasiswa (sudah termasuk MKWU); isi 0 untuk batas maksimal. plannedCourseCodes = SEMUA kode matkul yang sudah di draft/rencana saat ini (wajib + ulang + pilihan + atas), dipisah koma; kosongkan jika draft masih kosong. WAJIB dipakai untuk anggaran SKS - jangan hitung sisa kuota manual. JSON {maxSks, targetSks, effectiveTargetSks, mkwuReservedSks, mandatorySks, upperBudgetSks, committedSks, availableForNewSks, projectedTotalWithMkwu}. availableForNewSks = sisa SKS untuk matkul BARU (sudah dikurangi MKWU + semua matkul di draft) - PAKAI ANGKA INI. committedSks = total SKS matkul yang sudah di draft. projectedTotalWithMkwu = committedSks + MKWU."
    )
    suspend fun getSksBudget(
        @LLMDescription("Nomor Induk Mahasiswa") nim: String,
        @LLMDescription("Semester berjalan") semester: Int,
        @LLMDescription("Target total SKS yang diinginkan mahasiswa, sudah termasuk MKWU. Isi 0 jika ingin memakai batas maksimal.") targetSks: Int,
        @LLMDescription("SEMUA kode matkul yang sudah ada di draft/rencana saat ini (wajib + ulang + pilihan + atas), dipisah koma. Kosongkan ('') jika draft masih kosong.") plannedCourseCodes: String
    ): String {
        logCall("getSksBudget", "nim=$nim, semester=$semester, targetSks=$targetSks, plannedCourseCodes=$plannedCourseCodes")
        val maxSks = khsUseCase.getMaxSks(nim, (semester - 1).coerceAtLeast(0))
        val reserved = krsValidationUseCase.getMkwuReservedSks(semester)
        val mandatorySks = courseUseCase.getTotalSksByCodes(
            courseUseCase.getRemainingMandatoryCourses(nim, semester).map { it.code }
        )
        val plannedCodes = plannedCourseCodes.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val committedSks = if (plannedCodes.isEmpty()) mandatorySks else courseUseCase.getTotalSksByCodes(plannedCodes)
        val effectiveTarget = if (targetSks <= 0) maxSks else minOf(targetSks, maxSks)
        val upperBudget = (effectiveTarget - reserved - mandatorySks).coerceAtLeast(0)
        val availableForNew = (effectiveTarget - reserved - committedSks).coerceAtLeast(0)
        return logResult(
            "getSksBudget",
            SksBudgetResponse(
                maxSks = maxSks,
                targetSks = if (targetSks > 0) targetSks else null,
                effectiveTargetSks = effectiveTarget,
                mkwuReservedSks = reserved,
                mandatorySks = mandatorySks,
                upperBudgetSks = upperBudget,
                committedSks = committedSks,
                availableForNewSks = availableForNew,
                projectedTotalWithMkwu = committedSks + reserved
            ).toJson()
        )
    }

    /** SKS budget specific to MBKM students (semester 6/7): cap = min(20, IP-based limit). */
    @Tool
    @LLMDescription(
        "Anggaran SKS KHUSUS mahasiswa MBKM (Studi Independen/Magang MBKM, sem 6/7): batas = min(20, batas IP) untuk SELURUH draft. Pakai HANYA bila mahasiswa benar-benar MBKM; selain itu pakai getSksBudget. targetSks isi 0 untuk maksimal. plannedCourseCodes = SEMUA kode matkul yang sudah di draft/rencana saat ini, dipisah koma; kosongkan jika draft masih kosong. Jangan hitung manual. JSON {maxSksByIp, mbkmCap, targetSks, effectiveTargetSks, mandatorySks, remainingSks, committedSks, availableForNewSks}. availableForNewSks = sisa SKS untuk matkul BARU (sudah dikurangi semua matkul di draft) - PAKAI ANGKA INI."
    )
    suspend fun getMbkmSksBudget(
        @LLMDescription("Nomor Induk Mahasiswa") nim: String,
        @LLMDescription("Semester berjalan (hanya relevan untuk semester 6 atau 7)") semester: Int,
        @LLMDescription("Target total SKS yang diinginkan mahasiswa. Isi 0 untuk memakai batas maksimal MBKM.") targetSks: Int,
        @LLMDescription("SEMUA kode matkul yang sudah ada di draft/rencana saat ini, dipisah koma. Kosongkan ('') jika draft masih kosong.") plannedCourseCodes: String
    ): String {
        logCall("getMbkmSksBudget", "nim=$nim, semester=$semester, targetSks=$targetSks, plannedCourseCodes=$plannedCourseCodes")
        val maxSksByIp = khsUseCase.getMaxSks(nim, (semester - 1).coerceAtLeast(0))
        val mbkmCap = minOf(20, maxSksByIp)
        val mandatorySks = courseUseCase.getTotalSksByCodes(
            courseUseCase.getRemainingMandatoryCourses(nim, semester).map { it.code }
        )
        val plannedCodes = plannedCourseCodes.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val committedSks = if (plannedCodes.isEmpty()) mandatorySks else courseUseCase.getTotalSksByCodes(plannedCodes)
        val effectiveTarget = if (targetSks <= 0) mbkmCap else minOf(targetSks, mbkmCap)
        val availableForNew = (effectiveTarget - committedSks).coerceAtLeast(0)
        return logResult(
            "getMbkmSksBudget",
            MbkmSksBudgetResponse(
                maxSksByIp = maxSksByIp,
                mbkmCap = mbkmCap,
                targetSks = if (targetSks > 0) targetSks else null,
                effectiveTargetSks = effectiveTarget,
                mandatorySks = mandatorySks,
                remainingSks = availableForNew,
                committedSks = committedSks,
                availableForNewSks = availableForNew
            ).toJson()
        )
    }

    /** Validates a candidate KRS draft; must be called before saveDraftKrs. */
    @Tool
    @LLMDescription(
        "Validasi draft KRS (daftar UUID course class dipisah koma): total SKS, batas, SKS disisihkan MKWU, sisa kuota, bentrok jadwal, pelanggaran prasyarat. WAJIB dipanggil sebelum saveDraftKrs; jangan cek SKS/jadwal/prasyarat manual. JSON {totalSks, maxSks, mkwuReservedSks, remainingSks, overQuota, scheduleConflicts:[{classA, classB, day}], prerequisiteViolations:[{courseCode, missingPrerequisites}], duplicateCourses:[{courseCode, courseName, classCodes}], valid}. valid=true boleh disimpan; overQuota=true = SKS melebihi kuota; duplicateCourses tidak kosong = ada matkul terpilih lebih dari sekali (mis. kode sama beda KOM) sehingga draft DITOLAK."
    )
    suspend fun validateKrsDraft(
        @LLMDescription("Nomor Induk Mahasiswa") nim: String,
        @LLMDescription("Semester berjalan") semester: Int,
        @LLMDescription("Daftar UUID course class yang dipilih, dipisahkan koma. Contoh: uuid1,uuid2,uuid3") courseClassIds: String
    ): String {
        logCall("validateKrsDraft", "nim=$nim, semester=$semester, courseClassIds=$courseClassIds")
        return try {
            val ids = courseClassIds.split(",").map { UUID.fromString(it.trim()) }
            logResult("validateKrsDraft", krsValidationUseCase.validateDraft(nim, semester, ids).toResponse().toJson())
        } catch (e: Exception) {
            logResult(
                "validateKrsDraft",
                ToolErrorResponse(message = "Gagal memvalidasi draft KRS: ${e.message}").toJson()
            )
        }
    }
}
