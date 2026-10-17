package id.andreasmlbngaol.kra.academic.data.entity

import id.andreasmlbngaol.kra.core.enums.Day
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.UUID

/**
 * A class offering for ONE specific academic term. See the domain model for the rationale.
 */
@Entity
@Table(name = "class_offerings")
class ClassOfferingEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_code", nullable = false)
    var course: CourseEntity = CourseEntity(),

    @Column(name = "class_code", nullable = false, length = 10)
    var classCode: String = "",

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "term_id", nullable = false)
    var term: AcademicTermEntity = AcademicTermEntity(),

    @Column(name = "room", length = 30)
    var room: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "day", length = 10)
    var day: Day? = null,

    @Column(name = "start_time", length = 5)
    var startTime: String? = null,

    @Column(name = "end_time", length = 5)
    var endTime: String? = null,

    @Column(name = "quota", nullable = false)
    var quota: Int = 0,

    /** Students already enrolled; seats left = quota - enrolled (V2 migration). */
    @Column(name = "enrolled", nullable = false)
    var enrolled: Int = 0,

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "offering_lecturers", joinColumns = [JoinColumn(name = "offering_id")])
    @Column(name = "lecturer_nidn", nullable = false, length = 10)
    var lecturerNidns: MutableSet<String> = mutableSetOf()
)
