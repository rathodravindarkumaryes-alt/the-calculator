package com.example.data

import kotlinx.coroutines.flow.Flow

class CalculationRepository(private val calculationDao: CalculationDao) {
    val allHistory: Flow<List<CalculationHistory>> = calculationDao.getAllHistory()
    val favorites: Flow<List<CalculationHistory>> = calculationDao.getFavorites()

    fun getHistoryByType(type: String): Flow<List<CalculationHistory>> {
        return calculationDao.getHistoryByType(type)
    }

    suspend fun insertHistory(item: CalculationHistory): Long {
        return calculationDao.insertHistory(item)
    }

    suspend fun deleteHistory(id: Int) {
        calculationDao.deleteHistory(id)
    }

    suspend fun updateFavoriteStatus(id: Int, isFavorite: Boolean) {
        calculationDao.updateFavoriteStatus(id, isFavorite)
    }

    suspend fun clearAllHistory() {
        calculationDao.clearAllHistory()
    }

    suspend fun clearHistoryByType(type: String) {
        calculationDao.clearHistoryByType(type)
    }
}
