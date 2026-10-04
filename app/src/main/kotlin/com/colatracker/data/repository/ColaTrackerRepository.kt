package com.colatracker.data.repository

import com.colatracker.AppConfig
import com.colatracker.data.api.ColaTrackerApi
import com.colatracker.data.models.*
import kotlinx.coroutines.CancellationException

/**
 * Интерфейс репозитория для работы с данными
 */
interface ColaTrackerRepository {
    suspend fun getChildren(): Result<List<Child>>
    suspend fun addDrink(childId: Int, amountMl: Int): Result<AddDrinkResponse>
    suspend fun getChildHistory(childId: Int): Result<List<DrinkHistoryItem>>
    suspend fun deleteDrink(drinkId: Int): Result<DeleteDrinkResponse>
    suspend fun uploadPhoto(
        childId: Int,
        imageBytes: ByteArray,
        fileName: String
    ): Result<UploadPhotoResponse>
}

/**
 * Единая точка доступа к репозиторию.
 *
 * Раньше каждый ViewModel создавал собственный `ColaTrackerApi`, а значит и
 * собственный `HttpClient(CIO)` с отдельным пулом потоков. При этом
 * `ChildDetailViewModel` живёт в сторе Activity и не очищается при возврате
 * назад — клиенты копились. Теперь клиент один на процесс.
 */
object ApiProvider {
    private val api: ColaTrackerApi by lazy { ColaTrackerApi() }

    val repository: ColaTrackerRepository by lazy {
        if (AppConfig.USE_MOCK_DATA) MockRepository() else RealRepository(api)
    }
}

/**
 * Реальный репозиторий для работы с API
 */
class RealRepository(private val api: ColaTrackerApi) : ColaTrackerRepository {

    override suspend fun getChildren(): Result<List<Child>> =
        apiResult { api.getChildren() }

    override suspend fun addDrink(childId: Int, amountMl: Int): Result<AddDrinkResponse> =
        apiResult { api.addDrink(childId, amountMl) }

    override suspend fun getChildHistory(childId: Int): Result<List<DrinkHistoryItem>> =
        apiResult { api.getChildHistory(childId) }

    override suspend fun deleteDrink(drinkId: Int): Result<DeleteDrinkResponse> =
        apiResult { api.deleteDrink(drinkId) }

    override suspend fun uploadPhoto(
        childId: Int,
        imageBytes: ByteArray,
        fileName: String
    ): Result<UploadPhotoResponse> = apiResult { api.uploadPhoto(childId, imageBytes, fileName) }
}

/**
 * Оборачивает вызов в [Result], но НЕ проглатывает отмену корутины:
 * иначе уход с экрана во время запроса выглядел бы как сетевая ошибка.
 */
private suspend fun <T> apiResult(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e)
}
