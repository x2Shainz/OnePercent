package com.onepercent.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user-created bullet-journal entry.
 *
 * @property title Short headline shown on the Index screen.
 * @property body  Full editable content shown on the Entry screen.
 * @property sectionId Foreign key to [Section.id], or null if the entry is free-floating
 *   (not grouped under any user-created section).
 * @property createdAt Creation timestamp in epoch milliseconds; kept for reference.
 * @property position  Manual sort order within [sectionId] (or among free-floating entries).
 *   Lower values appear first. Backfilled from [createdAt] order during migration v2→v3.
 */
@Entity(tableName = "entries")
data class Entry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val body: String,
    val sectionId: Long?,
    val createdAt: Long,
    val position: Int = 0
)
