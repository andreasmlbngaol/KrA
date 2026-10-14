package id.andreasmlbngaol.kra.academic.domain.repository

import id.andreasmlbngaol.kra.academic.domain.model.CurriculumPackage

/** Read-only access to the curriculum packages (mandatory course sets per semester). */
interface CurriculumRepository {
    /** Returns the curriculum package entries for [semester]. */
    fun getCurriculumPackage(semester: Int): List<CurriculumPackage>
}
