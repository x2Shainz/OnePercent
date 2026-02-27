package com.onepercent.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user-created collapsible section for grouping [Entry] items on the Index screen.
 *
 * @property name      Display name shown as the section header.
 * @property createdAt Creation timestamp in epoch milliseconds; kept for reference.
 * @property position  Manual sort order among all user sections. Lower values appear first.
 *   Backfilled from [createdAt] order during migration v2→v3.
 */
@Entity(tableName = "sections")
data class Section(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long,
    val position: Int = 0
)
