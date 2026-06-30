package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CalculationDao {
    @Query("SELECT * FROM calculation_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<CalculationHistory>>

    @Query("SELECT * FROM calculation_history WHERE type = :type ORDER BY timestamp DESC")
    fun getHistoryByType(type: String): Flow<List<CalculationHistory>>

    @Query("SELECT * FROM calculation_history WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavorites(): Flow<List<CalculationHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: CalculationHistory): Long

    @Query("DELETE FROM calculation_history WHERE id = :id")
    suspend fun deleteHistory(id: Int)

    @Query("UPDATE calculation_history SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Int, isFavorite: Boolean)

    @Query("DELETE FROM calculation_history")
    suspend fun clearAllHistory()

    @Query("DELETE FROM calculation_history WHERE type = :type")
    suspend fun clearHistoryByType(type: String)
}
