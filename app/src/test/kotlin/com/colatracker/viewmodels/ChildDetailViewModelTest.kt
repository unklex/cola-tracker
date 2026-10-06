package com.colatracker.viewmodels

import com.colatracker.data.models.AddDrinkResponse
import com.colatracker.data.models.Child
import com.colatracker.data.models.DeleteDrinkResponse
import com.colatracker.data.models.DrinkHistoryItem
import com.colatracker.data.models.UploadPhotoResponse
import com.colatracker.data.repository.ColaTrackerRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Тесты ChildDetailViewModel на фейковом репозитории (сеть не нужна).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChildDetailViewModelTest {

    private val start = Child(1, "Тест", null, monthlyLimit = 1000, consumedThisMonth = 0, remaining = 1000)

    private fun drink(id: Int, ml: Int) = DrinkHistoryItem(id, 1, ml, "2026-10-06T12:00:00")

    /** Репозиторий, поведением которого управляет тест. */
    private class FakeRepository(var child: Child) : ColaTrackerRepository {
        var history = mutableListOf<DrinkHistoryItem>()
        var addCalls = 0
        var failNext: Throwable? = null
        var gate: CompletableDeferred<Unit>? = null
        private var nextId = 1

        override suspend fun getChildren() = failNext?.let { Result.failure<List<Child>>(it) }
            ?: Result.success(listOf(child))

        override suspend fun addDrink(childId: Int, amountMl: Int): Result<AddDrinkResponse> {
            addCalls++
            gate?.await()
            failNext?.let { failNext = null; return Result.failure(it) }
            child = child.copy(
                consumedThisMonth = child.consumedThisMonth + amountMl,
                remaining = child.remaining - amountMl
            )
            val d = DrinkHistoryItem(nextId++, childId, amountMl, "2026-10-06T12:00:00")
            history.add(0, d)
            return Result.success(AddDrinkResponse("ok", child, d))
        }

        override suspend fun getChildHistory(childId: Int) = Result.success(history.toList())

        override suspend fun deleteDrink(drinkId: Int): Result<DeleteDrinkResponse> {
            val d = history.first { it.id == drinkId }
            history.remove(d)
            child = child.copy(
                consumedThisMonth = child.consumedThisMonth - d.amountMl,
                remaining = child.remaining + d.amountMl
            )
            return Result.success(DeleteDrinkResponse("ok", d))
        }

        override suspend fun uploadPhoto(childId: Int, imageBytes: ByteArray, fileName: String) =
            Result.success(UploadPhotoResponse("ok"))
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun success(vm: ChildDetailViewModel) = vm.uiState.value as ChildDetailUiState.Success

    @Test
    fun `добавление напитка обновляет ребёнка, историю и показывает сообщение`() {
        val repo = FakeRepository(start)
        val vm = ChildDetailViewModel(1, start, repo)

        vm.addDrink(250)

        val state = success(vm)
        assertEquals(750, state.child.remaining)
        assertEquals(250, state.child.consumedThisMonth)
        assertEquals(listOf(250), state.history.map { it.amountMl })
        assertTrue(vm.hasChanges.value)
        assertEquals("Добавлено 250 мл", vm.message.value)
        assertFalse(vm.isAddingDrink.value)
    }

    @Test
    fun `ошибка добавления показывает сообщение, не меняет данные и снимает индикатор`() {
        val repo = FakeRepository(start).apply { failNext = ApiError("Нет связи") }
        val vm = ChildDetailViewModel(1, start, repo)

        vm.addDrink(250)

        assertEquals("Нет связи", vm.message.value)
        assertEquals(start, success(vm).child)
        assertTrue(success(vm).history.isEmpty())
        assertFalse(vm.hasChanges.value)
        assertFalse(vm.isAddingDrink.value) // иначе кнопки остались бы заблокированными навсегда
    }

    @Test
    fun `повторное нажатие во время запроса игнорируется`() {
        val repo = FakeRepository(start).apply { gate = CompletableDeferred() }
        val vm = ChildDetailViewModel(1, start, repo)

        vm.addDrink(250)
        assertTrue(vm.isAddingDrink.value)
        vm.addDrink(250)
        vm.addDrink(330)

        repo.gate!!.complete(Unit)
        assertEquals(1, repo.addCalls)
        assertEquals(1, success(vm).history.size)
        assertFalse(vm.isAddingDrink.value)
    }

    @Test
    fun `удаление убирает запись и подтягивает счётчики с сервера`() {
        val repo = FakeRepository(start)
        val vm = ChildDetailViewModel(1, start, repo)
        vm.addDrink(250)
        vm.addDrink(330)
        val firstId = success(vm).history.last().id

        vm.deleteDrink(firstId)

        val state = success(vm)
        assertEquals(listOf(330), state.history.map { it.amountMl })
        assertEquals(330, state.child.consumedThisMonth)
        assertEquals(670, state.child.remaining)
        assertEquals("Запись удалена", vm.message.value)
    }

    @Test
    fun `загрузка берёт свежие данные ребёнка и историю`() {
        val repo = FakeRepository(start.copy(consumedThisMonth = 500, remaining = 500))
        repo.history = mutableListOf(drink(7, 500))
        val vm = ChildDetailViewModel(1, start, repo)

        vm.load()

        assertEquals(500, success(vm).child.consumedThisMonth)
        assertEquals(listOf(7), success(vm).history.map { it.id })
        assertFalse(vm.isLoadingHistory.value)
    }

    @Test
    fun `ошибка загрузки списка даёт состояние ошибки с понятным текстом`() {
        val repo = FakeRepository(start).apply { failNext = ApiError("Сервер не ответил вовремя") }
        val vm = ChildDetailViewModel(1, start, repo)

        vm.load()

        val state = vm.uiState.value as ChildDetailUiState.Error
        assertEquals("Сервер не ответил вовремя", state.message)
        assertFalse(vm.isLoadingHistory.value)
    }

    @Test
    fun `ребёнок пропал с сервера — состояние ошибки`() {
        val repo = FakeRepository(start.copy(id = 99)) // в ответе нет ребёнка с id=1
        val vm = ChildDetailViewModel(1, start, repo)

        vm.load()

        assertTrue(vm.uiState.value is ChildDetailUiState.Error)
    }

    private class ApiError(message: String) : Exception(message)
}
