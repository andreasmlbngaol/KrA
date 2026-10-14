package id.andreasmlbngaol.kra.academic.data.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "students")
class StudentEntity(
    @Id
    @Column(name = "nim", nullable = false, length = 9)
    var nim: String = "",

    @Column(name = "name", nullable = false, length = 100)
    var name: String = "",

    /** Official semester per SIA; display/audit only, NOT used for CSP eligibility. */
    @Column(name = "semester", nullable = false)
    var semester: Int = 0,

    /** Basis for posisi_angkatan (computed at query time, not stored). */
    @Column(name = "entry_year", nullable = false)
    var entryYear: Int = 0
)
