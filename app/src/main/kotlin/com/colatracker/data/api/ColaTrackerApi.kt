package com.colatracker.data.api

import com.colatracker.AppConfig
import com.colatracker.data.models.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * API клиент для работы с Cola Tracker API
 */
class ColaTrackerApi {
    
    private val client = HttpClient(CIO) {
        // JSON сериализация
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        
        // Логирование запросов (для отладки)
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }
        
        // Таймауты
        install(HttpTimeout) {
            requestTimeoutMillis = AppConfig.REQUEST_TIMEOUT_MS
            connectTimeoutMillis = AppConfig.REQUEST_TIMEOUT_MS
            socketTimeoutMillis = AppConfig.REQUEST_TIMEOUT_MS
        }
        
        // Дефолтные настройки запросов
        defaultRequest {
            url(AppConfig.BASE_URL)
            header("Authorization", "Bearer ${AppConfig.AUTH_TOKEN}")
            // НЕ устанавливаем contentType глобально - это ломает multipart/form-data загрузки
            // ContentNegotiation автоматически обработает JSON для GET/POST с JSON телом
        }
    }
    
    /**
     * Получить список всех детей
     */
    suspend fun getChildren(): List<Child> {
        return try {
            client.get("/children").body()
        } catch (e: Exception) {
            throw ApiException("Ошибка загрузки списка детей: ${e.message}", e)
        }
    }
    
    /**
     * Добавить запись о выпитом
     */
    suspend fun addDrink(childId: Int, amountMl: Int): AddDrinkResponse {
        return try {
            client.post("/children/$childId/drink") {
                contentType(ContentType.Application.Json)
                setBody(DrinkRequest(amountMl))
            }.body()
        } catch (e: Exception) {
            throw ApiException("Ошибка добавления записи: ${e.message}", e)
        }
    }
    
    /**
     * Получить историю потребления для ребёнка
     */
    suspend fun getChildHistory(childId: Int): List<DrinkHistoryItem> {
        return try {
            client.get("/children/$childId/history").body()
        } catch (e: Exception) {
            throw ApiException("Ошибка загрузки истории: ${e.message}", e)
        }
    }
    
    /**
     * Удалить запись о выпитом
     */
    suspend fun deleteDrink(drinkId: Int): DeleteDrinkResponse {
        return try {
            client.delete("/drinks/$drinkId").body()
        } catch (e: Exception) {
            throw ApiException("Ошибка удаления записи: ${e.message}", e)
        }
    }
    
    /**
     * Загрузить фото ребёнка
     */
    suspend fun uploadPhoto(childId: Int, imageBytes: ByteArray, fileName: String): UploadPhotoResponse {
        return try {
            val response = client.post("/children/$childId/photo") {
                // Авторизация уже установлена в defaultRequest, не дублируем
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("file", imageBytes, Headers.build {
                                append(HttpHeaders.ContentType, "image/jpeg")
                                append(HttpHeaders.ContentDisposition, "form-data; name=\"file\"; filename=\"$fileName\"")
                            })
                        }
                    )
                )
            }
            
            if (response.status.value in 200..299) {
                response.body()
            } else {
                val errorBody = try {
                    response.body<String>()
                } catch (_: Exception) {
                    "Не удалось прочитать ответ"
                }
                throw ApiException("Ошибка загрузки фото: ${response.status} - $errorBody", null)
            }
        } catch (e: Exception) {
            if (e is ApiException) throw e
            throw ApiException("Ошибка загрузки фото: ${e.message}", e)
        }
    }

    /**
     * Проверить работу API
     */
    suspend fun healthCheck(): ApiResponse {
        return try {
            client.get("/").body()
        } catch (e: Exception) {
            throw ApiException("API недоступен: ${e.message}", e)
        }
    }
    
    /**
     * Закрыть клиент
     */
    fun close() {
        client.close()
    }
}

/**
 * Кастомное исключение для API ошибок
 */
class ApiException(message: String, cause: Throwable? = null) : Exception(message, cause)
