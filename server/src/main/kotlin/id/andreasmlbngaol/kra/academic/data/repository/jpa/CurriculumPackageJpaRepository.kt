package id.andreasmlbngaol.kra.academic.data.repository.jpa

import id.andreasmlbngaol.kra.academic.data.entity.CurriculumPackageEntity
import id.andreasmlbngaol.kra.academic.data.entity.CurriculumPackageId
import org.springframework.data.jpa.repository.JpaRepository

interface CurriculumPackageJpaRepository : JpaRepository<CurriculumPackageEntity, CurriculumPackageId> {
    fun findBySemester(semester: Int): List<CurriculumPackageEntity>
}
