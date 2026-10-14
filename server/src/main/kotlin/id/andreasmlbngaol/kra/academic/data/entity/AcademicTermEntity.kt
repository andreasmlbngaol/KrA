package id.andreasmlbngaol.kra.academic.data.entity

import id.andreasmlbngaol.kra.core.enums.SemesterType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.util.UUID

@Entity
@Table(
    name = "academic_terms",
    uniqueConstraints = [UniqueConstraint(columnNames = ["year", "semester_type"])]
)
class AcademicTermEntity(
    @Id
    @Column(name = "id")
    var id: UUID = UUID.randomUUID(),

    @Column(name = "year", nullable = false)
    var year: Int = 0,

    @Enumerated(EnumType.STRING)
    @Column(name = "semester_type", nullable = false, length = 10)
    var semesterType: SemesterType = SemesterType.GANJIL,

    /** Source of truth for the current "term berjalan". Only one row may be active. */
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = false
)
