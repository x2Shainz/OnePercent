package com.onepercent.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    /** Due date stored as epoch milliseconds (UTC midnight of the chosen day). */
    val dueDate: Long
)
