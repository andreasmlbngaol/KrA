package id.andreasmlbngaol.kra.academic.data.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable
import java.util.UUID

/** Composite key of [KrsEntity]. */
data class KrsId(
    var nim: String = "",
    var classOfferingId: UUID = UUID(0, 0),
    var semester: Int = 0
) : Serializable

@Entity
@IdClass(KrsId::class)
@Table(name = "krs")
class KrsEntity(
    @Id
    @Column(name = "nim", nullable = false, length = 9)
    var nim: String = "",

    @Id
    @Column(name = "class_offering_id", nullable = false)
    var classOfferingId: UUID = UUID(0, 0),

    @Id
    @Column(name = "semester", nullable = false)
    var semester: Int = 0
)
