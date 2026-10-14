package id.andreasmlbngaol.kra.academic.data.entity

import id.andreasmlbngaol.kra.core.enums.LockType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable

/** Composite key of [CurriculumPackageEntity]. */
data class CurriculumPackageId(
    var semester: Int = 0,
    var courseCode: String = ""
) : Serializable

@Entity
@IdClass(CurriculumPackageId::class)
@Table(name = "curriculum_packages")
class CurriculumPackageEntity(
    @Id
    @Column(name = "semester", nullable = false)
    var semester: Int = 0,

    @Id
    @Column(name = "course_code", nullable = false, length = 20)
    var courseCode: String = "",

    @Enumerated(EnumType.STRING)
    @Column(name = "lock_type", nullable = false, length = 10)
    var lockType: LockType = LockType.SOFT
)
