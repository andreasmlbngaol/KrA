package id.andreasmlbngaol.kra.academic.data.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "lecturers")
class LecturerEntity(
    @Id
    @Column(name = "nidn", nullable = false, length = 10)
    var nidn: String = "",

    @Column(name = "name", nullable = false, length = 100)
    var name: String = ""
)
