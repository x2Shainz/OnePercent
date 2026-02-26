package com.onepercent.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.onepercent.app.data.model.Section
import kotlinx.coroutines.flow.Flow

@Dao
interface SectionDao {

    /** Inserts a new section and returns its auto-generated row ID. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSection(section: Section): Long

    /** Permanently removes the given section row. */
    @Delete
    suspend fun deleteSection(section: Section)

    /** Returns all sections ordered by creation time (oldest first). */
    @Query("SELECT * FROM sections ORDER BY createdAt ASC")
    fun getAllSections(): Flow<List<Section>>
}
