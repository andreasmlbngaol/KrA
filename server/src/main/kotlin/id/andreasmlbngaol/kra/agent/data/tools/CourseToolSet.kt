package id.andreasmlbngaol.kra.agent.data.tools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import org.springframework.stereotype.Component
import id.andreasmlbngaol.kra.academic.domain.usecase.CourseUseCase
import id.andreasmlbngaol.kra.academic.domain.usecase.KhsUseCase

/**
 * Agent ToolSet exposing read-only course-catalog operations to the LLM: available
 * courses, curriculum packages, class offerings/schedules, SKS totals, remaining
 * mandatory courses, required electives, eligible upper-semester courses, and
 * capstone (KP/TA) eligibility.
 */
@Component
class CourseToolSet(
    private val courseUseCase: CourseUseCase,
    private val khsUseCase: KhsUseCase
) : LoggingToolSet() {

    /** Lists courses that have a class offering in the active term, without prerequisite filtering. */
    @Tool
    @LLMDescription(
        "Daftar matkul yang PUNYA KELAS di term aktif (offering), tanpa filter paritas maupun prasyarat. Inilah sumber kebenaran ketersediaan: selama matkul ada di daftar ini, matkul boleh diambil (prasyarat tetap dicek solver). BELUM difilter prasyarat - untuk matkul atas yang prasyaratnya terpenuhi pakai getEligibleUpperCourses. Berguna untuk verifikasi keberadaan/identitas matkul. JSON {semester, totalCourses, courses:[{code, name, sks, normalSemester, type}]}."
    )
    suspend fun getAvailableCourses(
        @LLMDescription("Semester mahasiswa saat ini (hanya metadata; ketersediaan mengikuti offering term aktif)") semester: Int
    ): String {
        logCall("getAvailableCourses", "semester=$semester")
        val courses = courseUseCase.getOfferedCourses()
        return logResult(
            "getAvailableCourses",
            AvailableCoursesResponse(
                semester = semester,
                totalCourses = courses.size,
                courses = courses.map { it.toDto() }
            ).toJson()
        )
    }

    /** Returns the mandatory curriculum package for [semester] (no KHS filtering). */
    @Tool
    @LLMDescription(
        "Paketan matkul wajib per semester. Semester 1-2 HARD LOCK; semester 3+ SOFT LOCK (TIDAK memfilter KHS, jadi jangan dipakai menyusun daftar wajib di semester 3+; pakai getRemainingMandatoryCourses). JSON {semester, totalSks, items:[{courseCode, courseName, sks, type, lockType}]}."
    )
    suspend fun getCurriculumPackage(
        @LLMDescription("Semester mahasiswa saat ini") semester: Int
    ): String {
        logCall("getCurriculumPackage", "semester=$semester")
        val packages = courseUseCase.getCurriculumPackage(semester)
        val courseMap = courseUseCase.getCoursesByCodes(packages.map { it.courseCode }).associateBy { it.code }
        val totalSks = courseUseCase.getTotalSksByCodes(packages.map { it.courseCode })
        val items = packages.map {
            val course = courseMap[it.courseCode]
            CurriculumPackageItemDto(
                courseCode = it.courseCode,
                courseName = course?.name,
                sks = course?.sks,
                type = course?.type,
                lockType = it.lockType
            )
        }
        return logResult(
            "getCurriculumPackage",
            CurriculumPackageResponse(semester = semester, totalSks = totalSks, items = items).toJson()
        )
    }

    /** Returns the class offerings and schedules for one or more courses in a single call. */
    @Tool
    @LLMDescription(
        "Kelas + jadwal untuk SATU ATAU BANYAK matkul sekaligus. Berikan semua kode matkul yang perlu dicek dalam SATU panggilan (dipisah koma) - jangan panggil berulang per matkul. JSON {totalCourses, courses:[{courseCode, courseName, sks, totalClasses, classes:[{id, classCode, day, startTime, endTime, room, quota, lecturers:[nama]}]}]}. day/startTime/endTime/room null jika TBD. Pakai field id sebagai courseClassId saat memvalidasi/menyimpan draft."
    )
    suspend fun getCourseClasses(
        @LLMDescription("Satu atau beberapa kode matkul, dipisah koma. Contoh: ILK3101,ILK3105,ILK3106") courseCodes: String
    ): String {
        logCall("getCourseClasses", "courseCodes=$courseCodes")
        val codes = courseCodes.split(",").map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        val items = codes.map { code ->
            val allClasses = courseUseCase.getClassOfferings(code)
            val course = courseUseCase.getCourseByCode(code)
            CourseClassesItem(
                courseCode = code,
                courseName = course?.name,
                sks = course?.sks,
                totalClasses = allClasses.size,
                classes = allClasses.map { it.toDto() }
            )
        }
        return logResult(
            "getCourseClasses",
            CourseClassesResponse(totalCourses = items.size, courses = items).toJson()
        )
    }

    /** Sums the total SKS for a comma-separated list of course codes. */
    @Tool
    @LLMDescription(
        "Menghitung total SKS dari daftar kode matkul (dipisah koma). Pakai ini untuk menghindari salah hitung; jangan jumlah SKS manual. JSON {courseCodes, totalSks}."
    )
    suspend fun calculateTotalSks(
        @LLMDescription("Daftar kode matkul dipisahkan koma. Contoh: ILK2104,ILK2105,ILK2106") courseCodes: String
    ): String {
        logCall("calculateTotalSks", "courseCodes=$courseCodes")
        val codes = courseCodes.split(",").map { it.trim() }
        return logResult(
            "calculateTotalSks",
            TotalSksResponse(courseCodes = codes, totalSks = courseUseCase.getTotalSksByCodes(codes)).toJson()
        )
    }

    /** Lists mandatory courses for [semester] the student has not passed yet. */
    @Tool
    @LLMDescription(
        "Matkul wajib prodi yang BELUM lulus sampai semester yang diminta, KUMULATIF (sudah difilter terhadap KHS; grade selain E/T dianggap lulus & tidak muncul). " +
            "Mahasiswa semester 8 tetap bisa punya matkul semester 5 di daftar ini. Dasar utama pengisian matkul wajib prodi. " +
            "importance = 1 + jumlah matkul wajib lain yang belum lulus dan membutuhkan matkul ini; importance > 1 berarti wajib diulang/diambil lebih dulu. " +
            "JSON {semester, totalSks, totalCourses, courses:[{code, name, sks, normalSemester, type, importance}]}."
    )
    suspend fun getRemainingMandatoryCourses(
        @LLMDescription("Nomor Induk Mahasiswa") nim: String,
        @LLMDescription("Semester berjalan") semester: Int
    ): String {
        logCall("getRemainingMandatoryCourses", "nim=$nim, semester=$semester")
        val courses = courseUseCase.getRemainingMandatoryCourses(nim, semester)
        val importance = courseUseCase.getImportanceScores(nim)
        val totalSks = courseUseCase.getTotalSksByCodes(courses.map { it.code })
        return logResult(
            "getRemainingMandatoryCourses",
            RemainingMandatoryCoursesResponse(
                semester = semester,
                totalSks = totalSks,
                totalCourses = courses.size,
                courses = courses.map { it.toDto(importance[it.code] ?: 1) }
            ).toJson()
        )
    }

    /** Returns the mandatory "choose exactly 1" elective group for [semester]. */
    @Tool
    @LLMDescription(
        "Kelompok matkul pilihan WAJIB untuk semester berjalan (mahasiswa HARUS memilih tepat 1). Ini sumber resmi & SATU-SATUNYA untuk menawarkan matkul pilihan 'pilih 1 dari N' di semester 5/6/7. requiredChoose=1 bila semester ini punya kelompok pilihan, 0 bila tidak ada (mis. semester 3/4/8). Mengulang matkul pilihan semester bawah TIDAK muncul di sini. JSON {semester, requiredChoose, totalOptions, courses:[{code, name, sks, normalSemester, type}]}."
    )
    suspend fun getRequiredElectives(
        @LLMDescription("Semester mahasiswa saat ini") semester: Int
    ): String {
        logCall("getRequiredElectives", "semester=$semester")
        val electives = courseUseCase.getSemesterElectives(semester)
        return logResult(
            "getRequiredElectives",
            RequiredElectivesResponse(
                semester = semester,
                requiredChoose = if (electives.isEmpty()) 0 else 1,
                totalOptions = electives.size,
                courses = electives.map { it.toDto() }
            ).toJson()
        )
    }

    /** Lists upper-semester REGULAR_MANDATORY courses the student may take early. */
    @Tool
    @LLMDescription(
        "Matkul semester atas yang eligible: punya kelas di term aktif (offering), prasyarat terpenuhi, dan normalSemester > semester sekarang. HANYA REGULAR_MANDATORY; matkul pilihan/peminatan dan matkul puncak (KP/Magang ILK4202, Tugas Akhir ILK4299) otomatis dikecualikan. JSON {semester, totalCourses, courses:[{code, name, sks, normalSemester, type}]}. pairingNote menjelaskan kaitan teori-praktikum (null bila tak relevan)."
    )
    suspend fun getEligibleUpperCourses(
        @LLMDescription("Nomor Induk Mahasiswa") nim: String,
        @LLMDescription("Semester mahasiswa saat ini") semester: Int
    ): String {
        logCall("getEligibleUpperCourses", "nim=$nim, semester=$semester")
        val passedCourses = khsUseCase.getPassedCourseCodes(nim).toSet()

        val allCourses = courseUseCase.getOfferedCourses()

        // Eligibility itself lives in the use case so the CSP solver and this tool agree.
        val eligible = courseUseCase.getEligibleUpperCourses(nim, semester)

        val eligibleByName = eligible.associateBy { it.name }

        val dtos = eligible.map { course ->
            val pairingNote: String? = when {
                course.name.startsWith("Praktikum ") -> {
                    val theory = allCourses.find { it.name == course.name.removePrefix("Praktikum ") }
                    when {
                        theory == null ->
                            "CATATAN: matkul praktikum tidak boleh diambil tanpa matkul teori pasangannya"
                        theory.code in passedCourses ->
                            "Teori pasangannya (${theory.code} - ${theory.name}) SUDAH LULUS, jadi praktikum ini boleh diambil sendiri"
                        else ->
                            "WAJIB diambil bersama matkul teori ${theory.code} - ${theory.name} (kecuali teori itu sudah lulus)"
                    }
                }
                eligibleByName.containsKey("Praktikum ${course.name}") -> {
                    val prak = eligibleByName.getValue("Praktikum ${course.name}")
                    "Punya praktikum pendamping: ${prak.code} - ${prak.name} (opsional; ambil bersama jika kuota cukup, tetapi teori ini boleh diambil sendiri)"
                }
                else -> null
            }
            EligibleCourseDto(
                code = course.code,
                name = course.name,
                sks = course.sks,
                normalSemester = course.normalSemester,
                type = course.type,
                pairingNote = pairingNote
            )
        }

        return logResult(
            "getEligibleUpperCourses",
            EligibleUpperCoursesResponse(semester = semester, totalCourses = dtos.size, courses = dtos).toJson()
        )
    }

    /** Checks capstone eligibility for KP/Magang (ILK4202) and Tugas Akhir (ILK4299). */
    @Tool
    @LLMDescription(
        "Cek kelayakan matkul puncak KP/Magang (ILK4202) & Tugas Akhir (ILK4299) dari total SKS lulus. Jangan bandingkan SKS lulus vs Min SKS manual. JSON {totalPassedSks, courses:[{code, found, name, sks, minSks, eligible}]}. eligible=true bila Min SKS terpenuhi; found=false bila data matkul tidak ada."
    )
    suspend fun getCapstoneCourses(
        @LLMDescription("Nomor Induk Mahasiswa") nim: String
    ): String {
        logCall("getCapstoneCourses", "nim=$nim")
        val passedSks = khsUseCase.getTotalPassedSks(nim)
        val dtos = listOf("ILK4202", "ILK4299").map { code ->
            val course = courseUseCase.getCourseByCode(code)
            if (course == null) {
                CapstoneCourseDto(code = code, found = false)
            } else {
                val minSks = course.minSks
                CapstoneCourseDto(
                    code = course.code,
                    found = true,
                    name = course.name,
                    sks = course.sks,
                    minSks = minSks,
                    eligible = passedSks >= minSks
                )
            }
        }
        return logResult(
            "getCapstoneCourses",
            CapstoneCoursesResponse(totalPassedSks = passedSks, courses = dtos).toJson()
        )
    }
}
