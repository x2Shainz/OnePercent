package com.onepercent.app.data.repository

import com.onepercent.app.data.model.Section
import kotlinx.coroutines.flow.Flow

interface SectionRepository {
    /** Inserts a new section with the given [name] and returns its auto-generated ID. */
    suspend fun addSection(name: String): Long

    /**
     * Deletes [section] and sets [com.onepercent.app.data.model.Entry.sectionId] to null
     * for all entries that belonged to it, so they become free-floating.
     * Both operations run inside a single database transaction.
     */
    suspend fun deleteSection(section: Section)

    /** Reactively emits all sections ordered by manual position (lowest first). */
    fun getAllSections(): Flow<List<Section>>

    /**
     * Persists new [Section.position] values for [sections], using each item's list index as
     * its new position. Runs all updates atomically so the DB never shows a partial reorder.
     */
    suspend fun reorderSections(sections: List<Section>)
}
