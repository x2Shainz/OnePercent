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
 * @property createdAt Creation timestamp in epoch milliseconds; used for ascending ordering.
 */
@Entity(tableName = "entries")
data class Entry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val body: String,
    val sectionId: Long?,
    val createdAt: Long
)
