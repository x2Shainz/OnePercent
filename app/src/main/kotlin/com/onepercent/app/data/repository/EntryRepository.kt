package com.onepercent.app.data.repository

import com.onepercent.app.data.model.Entry
import kotlinx.coroutines.flow.Flow

interface EntryRepository {
    /** Inserts a new entry with the given fields and returns its auto-generated ID. */
    suspend fun addEntry(title: String = "", body: String = "", sectionId: Long? = null): Long

    /** Updates the title and body of the entry with the given [id]. */
    suspend fun updateEntry(id: Long, title: String, body: String)

    /** Permanently deletes the given entry. */
    suspend fun deleteEntry(entry: Entry)

    /** Reactively emits the entry with [id], or null if it does not exist. */
    fun getEntryById(id: Long): Flow<Entry?>

    /** Reactively emits all entries ordered by creation time (oldest first). */
    fun getAllEntries(): Flow<List<Entry>>

    /** Reassigns the entry with [entryId] to [newSectionId] (null = free-floating). */
    suspend fun moveEntry(entryId: Long, newSectionId: Long?)

    /**
     * Reactively emits entries whose title or body contains [query].
     * Results are ordered newest-first and update as the DB changes.
     */
    fun searchEntries(query: String): Flow<List<Entry>>

    /**
     * Persists new [Entry.position] values for [entries], using each item's list index as
     * its new position. Runs all updates atomically so the DB never shows a partial reorder.
     */
    suspend fun reorderEntries(entries: List<Entry>)
}
