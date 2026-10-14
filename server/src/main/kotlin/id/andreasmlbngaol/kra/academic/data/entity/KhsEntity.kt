package id.andreasmlbngaol.kra.academic.data.entity

import id.andreasmlbngaol.kra.core.enums.Grade
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable

/** Composite key of [KhsEntity]. */
data class KhsId(
    var nim: String = "",
    var offeringId: java.util.UUID = java.util.UUID(0, 0)
) : Serializable

@Entity
@IdClass(KhsId::class)
@Table(name = "khs")
class KhsEntity(
    @Id
    @Column(name = "nim", nullable = false, length = 9)
    var nim: String = "",

    @Id
    @Column(name = "offering_id", nullable = false)
    var offeringId: java.util.UUID = java.util.UUID(0, 0),

    @Enumerated(EnumType.STRING)
    @Column(name = "grade", nullable = false, length = 10)
    var grade: Grade = Grade.A
)
