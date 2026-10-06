package com.colatracker.data.api

import com.colatracker.AppConfig
import com.colatracker.BuildConfig
import com.colatracker.data.models.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.network.sockets.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException

/**
 * API клиент для работы с Cola Tracker API.
 *
 * Клиент тяжёлый (собственный пул потоков CIO), поэтому создаётся один раз
 * на процесс — см. `ApiProvider`.
 */
class ColaTrackerApi {

    private val client = HttpClient(CIO) {
        // Без этого Ktor НЕ бросает исключение на 4xx/5xx, и тело ошибки
        // {"detail": "..."} пытается десериализоваться в ожидаемую модель,
        // превращая понятную «401 Unauthorized» в невнятную ошибку парсинга JSON.
        expectSuccess = true

        // JSON сериализация
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }

        // Логирование запросов (только в debug)
        install(Logging) {
            logger = Logger.DEFAULT
            level = if (BuildConfig.DEBUG) LogLevel.INFO else LogLevel.NONE
        }

        // Таймауты
        install(HttpTimeout) {
            requestTimeoutMillis = AppConfig.REQUEST_TIMEOUT_MS
            connectTimeoutMillis = AppConfig.CONNECT_TIMEOUT_MS
            socketTimeoutMillis = AppConfig.SOCKET_TIMEOUT_MS
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
    suspend fun getChildren(): List<Child> = safeCall("Не удалось загрузить список детей") {
        client.get("/children").body()
    }

    /**
     * Добавить запись о выпитом
     */
    suspend fun addDrink(childId: Int, amountMl: Int, requestId: String? = null): AddDrinkResponse =
        safeCall("Не удалось добавить запись") {
            client.post("/children/$childId/drink") {
                contentType(ContentType.Application.Json)
                setBody(DrinkRequest(amountMl, requestId))
            }.body()
        }

    /**
     * Получить историю потребления для ребёнка
     */
    suspend fun getChildHistory(childId: Int): List<DrinkHistoryItem> =
        safeCall("Не удалось загрузить историю") {
            client.get("/children/$childId/history").body()
        }

    /**
     * Удалить запись о выпитом
     */
    suspend fun deleteDrink(drinkId: Int): DeleteDrinkResponse =
        safeCall("Не удалось удалить запись") {
            client.delete("/drinks/$drinkId").body()
        }

    /**
     * Загрузить фото ребёнка
     */
    suspend fun uploadPhoto(
        childId: Int,
        imageBytes: ByteArray,
        fileName: String
    ): UploadPhotoResponse = safeCall("Не удалось загрузить фото") {
        client.post("/children/$childId/photo") {
            // Авторизация уже установлена в defaultRequest, не дублируем
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("file", imageBytes, Headers.build {
                            append(HttpHeaders.ContentType, "image/jpeg")
                            append(
                                HttpHeaders.ContentDisposition,
                                "form-data; name=\"file\"; filename=\"$fileName\""
                            )
                        })
                    }
                )
            )
        }.body()
    }

    /**
     * Закрыть клиент. Вызывается только при завершении процесса —
     * ViewModel'и общий клиент не закрывают.
     */
    fun close() {
        client.close()
    }

    // ===== Обработка ошибок =====

    /**
     * Превращает технические исключения Ktor в понятные пользователю сообщения.
     *
     * `CancellationException` пробрасывается как есть — иначе отмена корутины
     * (уход с экрана) выглядела бы как сетевая ошибка.
     */
    private suspend fun <T> safeCall(what: String, block: suspend () -> T): T {
        return try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: ClientRequestException) {
            throw ApiException(clientErrorMessage(what, e), e)
        } catch (e: ServerResponseException) {
            throw ApiException(
                "Сервер вернул ошибку ${e.response.status.value}. Попробуйте позже.", e,
                retryable = true
            )
        } catch (e: HttpRequestTimeoutException) {
            throw ApiException("Сервер не ответил вовремя. Попробуйте ещё раз.", e, retryable = true)
        } catch (e: ConnectTimeoutException) {
            throw ApiException("Не удалось подключиться к серверу. Проверьте интернет.", e, retryable = true)
        } catch (e: SocketTimeoutException) {
            throw ApiException("Соединение с сервером прервалось. Попробуйте ещё раз.", e, retryable = true)
        } catch (e: IOException) {
            throw ApiException("Нет связи с сервером. Проверьте интернет-соединение.", e, retryable = true)
        } catch (e: SerializationException) {
            throw ApiException("Сервер вернул данные в неожиданном формате.", e)
        } catch (e: Exception) {
            throw ApiException("$what: ${e.message ?: "неизвестная ошибка"}", e)
        }
    }

    private suspend fun clientErrorMessage(what: String, e: ClientRequestException): String {
        // FastAPI отдаёт ошибки как {"detail": "..."} — показываем текст с сервера,
        // если он есть.
        val detail = try {
            errorJson.decodeFromString<ErrorResponse>(e.response.bodyAsText()).detail
        } catch (_: Exception) {
            null
        }

        return when (e.response.status) {
            HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden ->
                "Неверный токен авторизации. Проверьте API_AUTH_TOKEN в local.properties."

            HttpStatusCode.NotFound -> detail ?: "$what: запись не найдена на сервере."

            HttpStatusCode.BadRequest -> detail ?: "$what: сервер отклонил запрос."

            else -> detail ?: "$what: сервер вернул ${e.response.status.value}."
        }
    }

    private companion object {
        val errorJson = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }
}

/**
 * Кастомное исключение для API ошибок.
 * `message` уже пригоден для показа пользователю.
 */
class ApiException(
    message: String,
    cause: Throwable? = null,
    /** Сбой транспорта или сервера (сеть, таймаут, 5xx): повтор запроса может помочь. */
    val retryable: Boolean = false
) : Exception(message, cause)
