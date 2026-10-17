package id.andreasmlbngaol.kra.agent.data

/**
 * Maps raw agent tool names to natural Indonesian step descriptions shown as a process
 * timeline in the chat UI. Kept in Indonesian (they are shown to students) with concise,
 * active phrasing rather than technical function names.
 */
object ToolStepLabels {

    private val labels: Map<String, String> = mapOf(
        "getStudentInfo" to "Mengecek data mahasiswa",
        "getKhs" to "Membaca riwayat nilai (KHS)",
        "getIpSemester" to "Menghitung IP & batas SKS semester",
        "getTotalPassedSks" to "Menghitung total SKS yang sudah lulus",
        "getRepeatableCourses" to "Mencari mata kuliah yang perlu/bisa diulang",
        "getAvailableCourses" to "Melihat mata kuliah yang ditawarkan",
        "getCurriculumPackage" to "Membuka paket mata kuliah wajib semester",
        "getCourseClasses" to "Mengambil kelas & jadwal mata kuliah",
        "calculateTotalSks" to "Menghitung total SKS",
        "getRemainingMandatoryCourses" to "Mendata mata kuliah wajib yang belum lulus",
        "getRequiredElectives" to "Mendata matkul pilihan wajib semester",
        "getEligibleUpperCourses" to "Mencari mata kuliah semester atas yang bisa diambil",
        "getCapstoneCourses" to "Mengecek kelayakan KP/Magang & Tugas Akhir",
        "getKrsDraft" to "Membuka draft KRS tersimpan",
        "saveDraftKrs" to "Menyimpan draft KRS",
        "validateKrsDraft" to "Memeriksa draft KRS (jadwal & prasyarat)",
        "solveKrs" to "Menyusun kombinasi KRS (solver CSP)",
        "getKrsCapacity" to "Menghitung kapasitas SKS realistis"
    )

    /** Friendly label for a tool; generic fallback when the tool is not mapped. */
    fun labelFor(toolName: String): String = labels[toolName] ?: "Memproses data akademik"
}
