package id.andreasmlbngaol.kra.agent.data.tools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import org.springframework.stereotype.Component
import id.andreasmlbngaol.kra.academic.domain.usecase.StudentUseCase

/**
 * Agent ToolSet exposing student lookup to the LLM. The Indonesian [LLMDescription] is a
 * runtime instruction for the model and is intentionally left in Indonesian.
 */
@Component
class StudentInfoToolSet(
    private val studentUseCase: StudentUseCase
) : LoggingToolSet() {

    /**
     * Looks up a student's name and semester by NIM; usually the first tool called.
     *
     * @param nim The student's NIM (9 characters).
     * @return JSON with the student data, or found=false when the NIM is unknown.
     */
    @Tool
    @LLMDescription(
        "Mengambil data mahasiswa (nama & semester) berdasarkan NIM. Panggil pertama kali sebelum proses lain. JSON {found, nim, name, semester}; found=false jika mahasiswa tidak ditemukan."
    )
    suspend fun getStudentInfo(
        @LLMDescription("Nomor Induk Mahasiswa (9 karakter)") nim: String
    ): String {
        logCall("getStudentInfo", "nim=$nim")
        val student = studentUseCase.getStudentByNim(nim)
            ?: return logResult(
                "getStudentInfo",
                StudentInfoResponse(
                    found = false,
                    nim = nim,
                    message = "Mahasiswa dengan NIM $nim tidak ditemukan"
                ).toJson()
            )
        return logResult(
            "getStudentInfo",
            StudentInfoResponse(
                found = true,
                nim = nim,
                name = student.name,
                semester = student.semester
            ).toJson()
        )
    }
}
