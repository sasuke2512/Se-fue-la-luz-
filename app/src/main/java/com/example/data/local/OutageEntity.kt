package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "outages")
data class OutageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val startTimeMillis: Long,
    val endTimeMillis: Long? = null,
    val note: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    val isActive: Boolean
        get() = endTimeMillis == null
}
