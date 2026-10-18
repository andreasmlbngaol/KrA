package id.andreasmlbngaol.kra.agent.data.tools

import id.andreasmlbngaol.kra.agent.data.session.SessionTokenRegistry
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import org.springframework.stereotype.Component
import id.andreasmlbngaol.kra.academic.domain.usecase.CourseUseCase
import id.andreasmlbngaol.kra.academic.domain.usecase.StudentUseCase

/**
 * Agent ToolSet exposing student lookup to the LLM. The Indonesian [LLMDescription] is a
 * runtime instruction for the model and is intentionally left in Indonesian.
 */
@Component
class StudentInfoToolSet(
    private val studentUseCase: StudentUseCase,
    private val courseUseCase: CourseUseCase,
    sessions: SessionTokenRegistry
) : LoggingToolSet(sessions) {

    /**
     * Looks up the session student's name and semester; usually the first tool called.
     *
     * @param sessionToken Opaque token from the system context, never the NIM.
     * @return JSON with the student data, or found=false when the session is unknown.
     */
    @Tool
    @LLMDescription(
        "Mengambil data mahasiswa pada sesi ini. Panggil pertama kali sebelum proses lain. " +
            "JSON {found, name, semester, semesterNote}; found=false jika mahasiswa tidak ditemukan. " +
            "semester = semester administratif (menghitung cuti). semesterNote = ringkasan matkul wajib yang masih tersisa, " +
            "dihitung dari KHS; pakai ini untuk tahu tingkat matkul yang sedang ditempuh tanpa menebak."
    )
    suspend fun getStudentInfo(
        @LLMDescription("Token sesi mahasiswa dari konteks sistem, bukan NIM") sessionToken: String
    ): String {
        val nim = resolveNim("getStudentInfo", sessionToken) ?: return sessionExpired()
        logCall("getStudentInfo", "nim=$nim")
        val student = studentUseCase.getStudentByNim(nim)
            ?: return logResult(
                "getStudentInfo",
                StudentInfoResponse(
                    found = false,
                    message = "Data mahasiswa untuk sesi ini tidak ditemukan"
                ).toJson()
            )
        val context = courseUseCase.resolveSemesterContext(nim, student.semester)
        val remaining = courseUseCase.getRemainingMandatoryCourses(nim, student.semester)
        val note = buildString {
            if (remaining.isEmpty()) {
                append("Semua matkul wajib sampai semester ${student.semester} sudah lulus.")
            } else {
                append("Masih ada ${remaining.size} matkul wajib belum lulus, mulai dari semester ")
                append("${context.lowestIncomplete ?: student.semester}.")
            }
            if (context.lastGraded in 1 until student.semester - 1) {
                append(" Nilai terakhir tercatat di semester ${context.lastGraded}.")
            }
        }
        return logResult(
            "getStudentInfo",
            StudentInfoResponse(
                found = true,
                name = student.name,
                semester = student.semester,
                semesterNote = note
            ).toJson()
        )
    }
}
