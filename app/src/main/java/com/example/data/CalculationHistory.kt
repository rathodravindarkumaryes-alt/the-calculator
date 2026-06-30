package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calculation_history")
data class CalculationHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String,
    val expression: String,
    val result: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
