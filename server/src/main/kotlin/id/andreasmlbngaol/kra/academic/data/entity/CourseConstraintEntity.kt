package id.andreasmlbngaol.kra.academic.data.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * Prodi-internal CSP constraint for a course, 1:1 with [CourseEntity] but deliberately
 * separated from the official catalog table.
 */
@Entity
@Table(name = "course_constraints")
class CourseConstraintEntity(
    @Id
    @Column(name = "course_code", nullable = false, length = 20)
    var courseCode: String = "",

    /** Minimum passed SKS required to take this course (capstone courses; 0 otherwise). */
    @Column(name = "min_sks", nullable = false)
    var minSks: Int = 0
)
