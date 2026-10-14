package id.andreasmlbngaol.kra.academic.data.entity

import id.andreasmlbngaol.kra.core.enums.CourseType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * Mirrors the official catalog data for a course.
 * CSP-relevant prodi-internal fields live in [CourseConstraintEntity] instead, so a future
 * catalog re-sync never clobbers manually-maintained constraint data.
 */
@Entity
@Table(name = "courses")
class CourseEntity(
    @Id
    @Column(name = "code", nullable = false, length = 20)
    var code: String = "",

    @Column(name = "name", nullable = false, length = 100)
    var name: String = "",

    @Column(name = "sks", nullable = false)
    var sks: Int = 0,

    @Column(name = "normal_semester", nullable = false)
    var normalSemester: Int = 0,

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    var type: CourseType = CourseType.REGULAR_MANDATORY
)
