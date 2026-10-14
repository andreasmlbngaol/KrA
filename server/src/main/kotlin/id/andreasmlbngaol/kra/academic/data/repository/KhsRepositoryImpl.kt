package id.andreasmlbngaol.kra.academic.data.repository

import id.andreasmlbngaol.kra.academic.data.entity.KhsEntity
import id.andreasmlbngaol.kra.academic.data.repository.jpa.ClassOfferingJpaRepository
import id.andreasmlbngaol.kra.academic.data.repository.jpa.KhsJpaRepository
import id.andreasmlbngaol.kra.academic.data.repository.jpa.StudentJpaRepository
import id.andreasmlbngaol.kra.academic.domain.model.Khs
import id.andreasmlbngaol.kra.academic.domain.repository.KhsRepository
import id.andreasmlbngaol.kra.core.enums.Grade
import id.andreasmlbngaol.kra.core.enums.SemesterType
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional
class KhsRepositoryImpl(
    private val khs: KhsJpaRepository,
    private val offerings: ClassOfferingJpaRepository,
    private val students: StudentJpaRepository
) : KhsRepository {

    override fun getKhsByNim(nim: String): List<Khs> {
        val entryYear = students.findById(nim).orElse(null)?.entryYear ?: return emptyList()
        return khs.findRowsByNim(nim).map { it.toDomain(entryYear) }
    }

    override fun getTotalPassedSks(nim: String): Int = khs.sumPassedSks(nim)

    override fun getKhsBySemester(nim: String, semester: Int): List<Khs> =
        getKhsByNim(nim).filter { it.semesterTaken == semester }

    override fun getPassedCourseCodes(nim: String): List<String> =
        khs.findRowsByNim(nim)
            .filter { it.getGrade() != Grade.E && it.getGrade() != Grade.T }
            .map { it.getCourseCode() }
            .distinct()

    override fun addKhs(khs: Khs): Boolean {
        // KHS rows are keyed by offering; resolve the offering from course+class+term.
        val offering = offerings.findAll().firstOrNull {
            it.course.code == khs.courseCode && it.classCode == khs.classCode &&
                it.term.year == khs.academicYear && it.term.semesterType == khs.semesterType
        } ?: return false
        this.khs.save(KhsEntity(khs.nim, offering.id, khs.grade))
        return true
    }

    override fun deleteKhs(nim: String, courseCode: String, semesterTaken: Int): Boolean {
        // A KHS row is keyed by (nim, offering); resolve the offering from course + term.
        val row = khs.findRowsByNim(nim).firstOrNull { it.getCourseCode() == courseCode } ?: return false
        val offering = offerings.findAll().firstOrNull {
            it.course.code == row.getCourseCode() && it.classCode == row.getClassCode()
        } ?: return false
        khs.deleteById(id.andreasmlbngaol.kra.academic.data.entity.KhsId(nim, offering.id))
        return true
    }

    private fun id.andreasmlbngaol.kra.academic.data.repository.jpa.KhsRow.toDomain(entryYear: Int): Khs = Khs(
        nim = getNim(),
        courseCode = getCourseCode(),
        classCode = getClassCode(),
        grade = getGrade(),
        // posisi_angkatan: GANJIL of entryYear = semester 1, GENAP = 2, and so on.
        semesterTaken = (getTermYear() - entryYear) * 2 + if (getSemesterType() == SemesterType.GANJIL) 1 else 2,
        academicYear = getTermYear(),
        semesterType = getSemesterType()
    )
}
