package id.andreasmlbngaol.kra.academic.data.repository.jpa

import id.andreasmlbngaol.kra.academic.data.entity.KhsEntity
import id.andreasmlbngaol.kra.academic.data.entity.KhsId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

/** A KHS row joined with the course/term data needed to rebuild the domain model. */
interface KhsRow {
    fun getNim(): String
    fun getGrade(): id.andreasmlbngaol.kra.core.enums.Grade
    fun getCourseCode(): String
    fun getClassCode(): String
    fun getTermYear(): Int
    fun getSemesterType(): id.andreasmlbngaol.kra.core.enums.SemesterType
}

interface KhsJpaRepository : JpaRepository<KhsEntity, KhsId> {
    @Query(
        """
        select k.nim as nim, k.grade as grade, o.course.code as courseCode,
               o.classCode as classCode, o.term.year as termYear, o.term.semesterType as semesterType
        from KhsEntity k, ClassOfferingEntity o
        where k.offeringId = o.id and k.nim = :nim
        """
    )
    fun findRowsByNim(@Param("nim") nim: String): List<KhsRow>

    /** Sum of SKS for passed courses (grade not E/T), via the joined offering's course. */
    @Query(
        """
        select coalesce(sum(c.sks), 0) from KhsEntity k, ClassOfferingEntity o, CourseEntity c
        where k.offeringId = o.id and o.course.code = c.code
          and k.nim = :nim and k.grade not in (id.andreasmlbngaol.kra.core.enums.Grade.E, id.andreasmlbngaol.kra.core.enums.Grade.T)
        """
    )
    fun sumPassedSks(@Param("nim") nim: String): Int
}
