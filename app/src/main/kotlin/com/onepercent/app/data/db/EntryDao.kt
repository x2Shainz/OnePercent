package com.onepercent.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.onepercent.app.data.model.Entry
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {

    /** Inserts a new entry and returns its auto-generated row ID. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: Entry): Long

    /**
     * Updates only the [title] and [body] of the entry with the given [id].
     * A targeted query is used so that [Entry.sectionId] and [Entry.createdAt] are not overwritten.
     */
    @Query("UPDATE entries SET title = :title, body = :body WHERE id = :id")
    suspend fun updateEntry(id: Long, title: String, body: String)

    /** Reassigns the entry with [id] to [sectionId] (null = free-floating). */
    @Query("UPDATE entries SET sectionId = :sectionId WHERE id = :id")
    suspend fun updateSection(id: Long, sectionId: Long?)

    /** Permanently removes the given entry. */
    @Delete
    suspend fun deleteEntry(entry: Entry)

    /**
     * Sets [Entry.sectionId] to null for every entry belonging to [sectionId].
     * Called inside a transaction before deleting the section, so those entries
     * become free-floating rather than being orphaned.
     */
    @Query("UPDATE entries SET sectionId = NULL WHERE sectionId = :sectionId")
    suspend fun clearSection(sectionId: Long)

    /** Returns the entry with the given [id], or null if it does not exist. */
    @Query("SELECT * FROM entries WHERE id = :id")
    fun getEntryById(id: Long): Flow<Entry?>

    /** Returns all entries ordered by manual position (lowest first). */
    @Query("SELECT * FROM entries ORDER BY position ASC")
    fun getAllEntries(): Flow<List<Entry>>

    /** Returns all entries belonging to [sectionId], ordered by manual position. */
    @Query("SELECT * FROM entries WHERE sectionId = :sectionId ORDER BY position ASC")
    fun getEntriesForSection(sectionId: Long): Flow<List<Entry>>

    /** Returns all entries with no section assignment, ordered by manual position. */
    @Query("SELECT * FROM entries WHERE sectionId IS NULL ORDER BY position ASC")
    fun getUnassignedEntries(): Flow<List<Entry>>

    /** Updates the [position] of the entry with [id]. Used for batch reorder commits. */
    @Query("UPDATE entries SET position = :position WHERE id = :id")
    suspend fun updatePosition(id: Long, position: Int)

    /**
     * Returns all entries whose [Entry.title] or [Entry.body] contains [query] (case-insensitive),
     * ordered newest first. Results update reactively as the DB changes.
     */
    @Query("""
        SELECT * FROM entries
        WHERE title LIKE '%' || :query || '%' OR body LIKE '%' || :query || '%'
        ORDER BY createdAt DESC
    """)
    fun searchEntries(query: String): Flow<List<Entry>>
}
