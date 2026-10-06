package com.colatracker.data.repository

import com.colatracker.data.models.*
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Mock репозиторий с фейковыми данными
 * Используется для тестирования UI без реального сервера
 */
class MockRepository : ColaTrackerRepository {
    
    // Фейковые данные детей
    private val mockChildren = mutableListOf(
        Child(
            id = 1,
            name = "Ребёнок 1",
            photoUrl = null,
            monthlyLimit = 1000,
            consumedThisMonth = 330,
            remaining = 670
        ),
        Child(
            id = 2,
            name = "Ребёнок 2",
            photoUrl = null,
            monthlyLimit = 1000,
            consumedThisMonth = 0,
            remaining = 1000
        )
    )
    
    // Фейковая история
    private val mockHistory = mutableListOf(
        DrinkHistoryItem(
            id = 1,
            childId = 1,
            amountMl = 330,
            timestamp = "2025-01-13T14:30:00.000000"
        ),
        DrinkHistoryItem(
            id = 2,
            childId = 1,
            amountMl = 250,
            timestamp = "2025-01-13T10:15:00.000000"
        ),
        DrinkHistoryItem(
            id = 3,
            childId = 2,
            amountMl = 330,
            timestamp = "2025-01-12T18:45:00.000000"
        )
    )
    
    private var nextDrinkId = 4
    
    override suspend fun getChildren(): Result<List<Child>> {
        // Имитируем задержку сети
        delay(500)
        return Result.success(mockChildren.toList())
    }
    
    override suspend fun addDrink(childId: Int, amountMl: Int): Result<AddDrinkResponse> {
        delay(300)
        
        // Находим ребёнка
        val childIndex = mockChildren.indexOfFirst { it.id == childId }
        if (childIndex == -1) {
            return Result.failure(Exception("Ребёнок не найден"))
        }
        
        val child = mockChildren[childIndex]
        
        // Обновляем счётчики
        val updatedChild = child.copy(
            consumedThisMonth = child.consumedThisMonth + amountMl,
            // Как на бэкенде: баланс может уйти в минус
            remaining = child.remaining - amountMl
        )
        mockChildren[childIndex] = updatedChild
        
        // Создаём новую запись
        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
        val newDrink = DrinkHistoryItem(
            id = nextDrinkId++,
            childId = childId,
            amountMl = amountMl,
            timestamp = now.format(formatter)
        )
        
        mockHistory.add(0, newDrink)
        
        return Result.success(
            AddDrinkResponse(
                message = "Запись добавлена",
                child = updatedChild,
                drink = newDrink
            )
        )
    }
    
    override suspend fun getChildHistory(childId: Int): Result<List<DrinkHistoryItem>> {
        delay(300)
        
        val history = mockHistory
            .filter { it.childId == childId }
            .sortedByDescending { it.timestamp }
        
        return Result.success(history)
    }
    
    override suspend fun deleteDrink(drinkId: Int): Result<DeleteDrinkResponse> {
        delay(300)
        
        // Находим запись
        val drink = mockHistory.find { it.id == drinkId }
            ?: return Result.failure(Exception("Запись не найдена"))
        
        // Находим ребёнка
        val childIndex = mockChildren.indexOfFirst { it.id == drink.childId }
        if (childIndex != -1) {
            val child = mockChildren[childIndex]
            
            // Откатываем счётчики
            val updatedChild = child.copy(
                consumedThisMonth = (child.consumedThisMonth - drink.amountMl).coerceAtLeast(0),
                remaining = child.remaining + drink.amountMl
            )
            mockChildren[childIndex] = updatedChild
        }
        
        // Удаляем запись
        mockHistory.removeAll { it.id == drinkId }

        return Result.success(
            DeleteDrinkResponse(
                message = "Запись удалена",
                deletedDrink = drink
            )
        )
    }

    override suspend fun uploadPhoto(childId: Int, imageBytes: ByteArray, fileName: String): Result<UploadPhotoResponse> {
        delay(500)

        // Находим ребёнка
        val childIndex = mockChildren.indexOfFirst { it.id == childId }
        if (childIndex == -1) {
            return Result.failure(Exception("Ребёнок не найден"))
        }

        val child = mockChildren[childIndex]

        // Имитируем сохранение фото
        val updatedChild = child.copy(
            photoUrl = "photos/child_$childId.jpg"
        )
        mockChildren[childIndex] = updatedChild

        return Result.success(
            UploadPhotoResponse(
                message = "Фото загружено",
                photoUrl = updatedChild.photoUrl,
                childId = childId
            )
        )
    }
}
