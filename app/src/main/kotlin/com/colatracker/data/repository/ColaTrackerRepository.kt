package com.colatracker.data.repository

import com.colatracker.data.api.ColaTrackerApi
import com.colatracker.data.models.*

/**
 * Интерфейс репозитория для работы с данными
 */
interface ColaTrackerRepository {
    suspend fun getChildren(): Result<List<Child>>
    suspend fun addDrink(childId: Int, amountMl: Int): Result<AddDrinkResponse>
    suspend fun getChildHistory(childId: Int): Result<List<DrinkHistoryItem>>
    suspend fun deleteDrink(drinkId: Int): Result<DeleteDrinkResponse>
    suspend fun uploadPhoto(childId: Int, imageBytes: ByteArray, fileName: String): Result<UploadPhotoResponse>
}

/**
 * Реальный репозиторий для работы с API
 */
class RealRepository(private val api: ColaTrackerApi) : ColaTrackerRepository {
    
    override suspend fun getChildren(): Result<List<Child>> {
        return try {
            val children = api.getChildren()
            Result.success(children)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun addDrink(childId: Int, amountMl: Int): Result<AddDrinkResponse> {
        return try {
            val response = api.addDrink(childId, amountMl)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getChildHistory(childId: Int): Result<List<DrinkHistoryItem>> {
        return try {
            val history = api.getChildHistory(childId)
            Result.success(history)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteDrink(drinkId: Int): Result<DeleteDrinkResponse> {
        return try {
            val response = api.deleteDrink(drinkId)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadPhoto(childId: Int, imageBytes: ByteArray, fileName: String): Result<UploadPhotoResponse> {
        return try {
            val response = api.uploadPhoto(childId, imageBytes, fileName)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
