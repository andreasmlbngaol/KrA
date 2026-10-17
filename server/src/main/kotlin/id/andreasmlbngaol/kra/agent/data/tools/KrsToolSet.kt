package id.andreasmlbngaol.kra.agent.data.tools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import id.andreasmlbngaol.kra.academic.domain.model.KrsValidationResult
import id.andreasmlbngaol.kra.academic.domain.model.SaveDraftKrsResult
import id.andreasmlbngaol.kra.academic.domain.usecase.KrsUseCase
import id.andreasmlbngaol.kra.academic.domain.usecase.KrsValidationUseCase
import java.util.UUID

/**
 * Agent ToolSet exposing KRS draft operations to the LLM: reading the saved draft,
 * computing SKS budgets (regular and MBKM), validating a candidate draft, and saving it.
 * Saving runs validation first and only persists a clean draft.
 */
class KrsToolSet(
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
