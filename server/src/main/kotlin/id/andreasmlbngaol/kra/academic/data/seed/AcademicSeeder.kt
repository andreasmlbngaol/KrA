package id.andreasmlbngaol.kra.academic.data.seed

import id.andreasmlbngaol.kra.academic.data.entity.AcademicTermEntity
import id.andreasmlbngaol.kra.academic.data.entity.ClassOfferingEntity
import id.andreasmlbngaol.kra.academic.data.entity.CourseConstraintEntity
import id.andreasmlbngaol.kra.academic.data.entity.CourseEntity
import id.andreasmlbngaol.kra.academic.data.entity.CoursePrerequisiteEntity
import id.andreasmlbngaol.kra.academic.data.entity.CurriculumPackageEntity
import id.andreasmlbngaol.kra.academic.data.entity.KhsEntity
import id.andreasmlbngaol.kra.academic.data.entity.LecturerEntity
import id.andreasmlbngaol.kra.academic.data.entity.StudentEntity
import id.andreasmlbngaol.kra.academic.data.repository.jpa.AcademicTermJpaRepository
import id.andreasmlbngaol.kra.academic.data.repository.jpa.ClassOfferingJpaRepository
import id.andreasmlbngaol.kra.academic.data.repository.jpa.CourseConstraintJpaRepository
import id.andreasmlbngaol.kra.academic.data.repository.jpa.CourseJpaRepository
import id.andreasmlbngaol.kra.academic.data.repository.jpa.CoursePrerequisiteJpaRepository
import id.andreasmlbngaol.kra.academic.data.repository.jpa.CurriculumPackageJpaRepository
import id.andreasmlbngaol.kra.academic.data.repository.jpa.KhsJpaRepository
import id.andreasmlbngaol.kra.academic.data.repository.jpa.LecturerJpaRepository
import id.andreasmlbngaol.kra.academic.data.repository.jpa.StudentJpaRepository
import id.andreasmlbngaol.kra.core.enums.SemesterType
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Idempotent seeder for academic reference + demo data. Runs once at startup and is a
 * no-op when the database already contains students.
 *
 * Ported from the old Ktor project's `DatabaseSeeder`: same order, same data, but using
 * Spring Data JPA instead of Exposed `batchInsert`.
 */
@Component
class AcademicSeeder(
    private val lecturers: LecturerJpaRepository,
    private val courses: CourseJpaRepository,
    private val constraints: CourseConstraintJpaRepository,
    private val prerequisites: CoursePrerequisiteJpaRepository,
    private val terms: AcademicTermJpaRepository,
    private val offerings: ClassOfferingJpaRepository,
    private val curriculum: CurriculumPackageJpaRepository,
    private val students: StudentJpaRepository,
    private val khs: KhsJpaRepository
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun run(args: ApplicationArguments) {
        if (students.count() > 0) return
        log.info("Seeding academic data…")

        seedLecturers()
        seedCourses()
        seedConstraints()
        seedPrerequisites()
        seedTerms()
        seedClassOfferings()
        seedStudents()
        seedCurriculumPackages()
        seedKhs()

        log.info("Academic data seeded.")
    }

    private fun seedLecturers() {
        lecturers.saveAll(dosenSeedList.map { LecturerEntity(it.nidn, it.nama) })
    }

    private fun seedCourses() {
        courses.saveAll(
            courseSeedList.map { CourseEntity(it.code, it.name, it.sks, it.normalSemester, it.type) }
        )
    }

    private fun seedConstraints() {
        constraints.saveAll(constraintSeedList.map { CourseConstraintEntity(it.courseCode, it.minSks) })
    }

    private fun seedPrerequisites() {
        prerequisites.saveAll(
            prerequisiteSeedList.map { CoursePrerequisiteEntity(it.first, it.second) }
        )
    }

    private fun seedTerms() {
        val rows = (2022..2025).flatMap { year -> SemesterType.entries.map { year to it } }
        terms.saveAll(rows.map { (year, type) -> AcademicTermEntity(UUID.randomUUID(), year, type, false) })
        terms.save(AcademicTermEntity(UUID.randomUUID(), 2026, SemesterType.GANJIL, true))
    }

    private fun seedClassOfferings() {
        val courseMeta = courseSeedList.associate { it.code to it.normalSemester }
        fun semesterTypeFor(code: String): SemesterType =
            if (courseMeta.getValue(code) % 2 == 1) SemesterType.GANJIL else SemesterType.GENAP

        val allTerms = terms.findAll()
        val courseEntities = courses.findAll().associateBy { it.code }
        val termEntities = allTerms.associateBy { it.year to it.semesterType }

        val toSave = mutableListOf<ClassOfferingEntity>()
        var index = 0
        for (term in allTerms) {
            val classData = classSeedList.filter {
                it.courseCode == "ILK4202" || it.courseCode == "ILK4299" ||
                    semesterTypeFor(it.courseCode) == term.semesterType
            }
            for (cls in classData) {
                toSave.add(
                    ClassOfferingEntity(
                        id = UUID.randomUUID(),
                        course = courseEntities.getValue(cls.courseCode),
                        classCode = cls.classCode,
                        term = termEntities.getValue(term.year to term.semesterType),
                        room = if (cls.day != null) "Gd. D - R.${101 + (index % 12)}" else null,
                        day = cls.day,
                        startTime = cls.startTime,
                        endTime = cls.endTime,
                        quota = cls.quota,
                        lecturerNidns = (classLecturers[cls.courseCode to cls.classCode] ?: emptyList())
                            .toMutableSet()
                    )
                )
                index++
            }
        }
        offerings.saveAll(toSave)
    }

    private fun seedStudents() {
        val all = user1Students + user2Students + user3Students + user4Students + user5Students
        students.saveAll(
            all.map { StudentEntity(it.nim, it.name, it.semester, 2000 + it.nim.take(2).toInt()) }
        )
    }

    private fun seedCurriculumPackages() {
        curriculum.saveAll(
            curriculumPackageSeedList.map { CurriculumPackageEntity(it.first, it.second, it.third) }
        )
    }

    private fun seedKhs() {
        val khsData = user1KhsData() + user2KhsData() + user3KhsData() + user4KhsData() + user5KhsData()

        val termCache = terms.findAll().associate { (it.year to it.semesterType) to it.id }
        val offeringCache = offerings.findAll().associate {
            Triple(it.course.code, it.classCode, it.term.id) to it.id
        }

        val rows = khsData.mapNotNull { data ->
            val termId = termCache[data.year to data.semesterType] ?: return@mapNotNull null
            val offeringId = offeringCache[Triple(data.courseCode, data.classCode, termId)]
                ?: return@mapNotNull null
            KhsEntity(data.nim, offeringId, data.grade)
        }
        khs.saveAll(rows)
    }
}
