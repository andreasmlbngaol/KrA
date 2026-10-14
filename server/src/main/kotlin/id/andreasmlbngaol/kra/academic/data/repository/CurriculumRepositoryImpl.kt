package id.andreasmlbngaol.kra.academic.data.repository

import id.andreasmlbngaol.kra.academic.data.repository.jpa.CurriculumPackageJpaRepository
import id.andreasmlbngaol.kra.academic.domain.model.CurriculumPackage
import id.andreasmlbngaol.kra.academic.domain.repository.CurriculumRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional(readOnly = true)
class CurriculumRepositoryImpl(
    private val packages: CurriculumPackageJpaRepository
) : CurriculumRepository {
    override fun getCurriculumPackage(semester: Int): List<CurriculumPackage> =
        packages.findBySemester(semester).map {
            CurriculumPackage(semester = it.semester, courseCode = it.courseCode, lockType = it.lockType)
        }
}
