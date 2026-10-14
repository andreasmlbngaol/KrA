package id.andreasmlbngaol.kra.academic.data.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable

/** Composite key of [CoursePrerequisiteEntity]. */
data class CoursePrerequisiteId(
    var courseCode: String = "",
    var prerequisiteCode: String = ""
) : Serializable

@Entity
@IdClass(CoursePrerequisiteId::class)
@Table(name = "course_prerequisites")
class CoursePrerequisiteEntity(
    @Id
    @Column(name = "course_code", nullable = false, length = 20)
    var courseCode: String = "",

    @Id
    @Column(name = "prerequisite_code", nullable = false, length = 20)
    var prerequisiteCode: String = ""
)
