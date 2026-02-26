package com.onepercent.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user-created collapsible section for grouping [Entry] items on the Index screen.
 *
 * @property name      Display name shown as the section header.
 * @property createdAt Creation timestamp in epoch milliseconds; used for ascending ordering.
 */
@Entity(tableName = "sections")
data class Section(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long
)
