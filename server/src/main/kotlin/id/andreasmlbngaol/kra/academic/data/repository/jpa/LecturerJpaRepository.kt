package id.andreasmlbngaol.kra.academic.data.repository.jpa

import id.andreasmlbngaol.kra.academic.data.entity.LecturerEntity
import org.springframework.data.jpa.repository.JpaRepository

interface LecturerJpaRepository : JpaRepository<LecturerEntity, String>
