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

    /** Reactively emits all sections ordered by creation time (oldest first). */
    fun getAllSections(): Flow<List<Section>>
}
