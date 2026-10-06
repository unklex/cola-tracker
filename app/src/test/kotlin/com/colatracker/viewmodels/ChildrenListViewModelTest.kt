package com.colatracker.viewmodels

import com.colatracker.data.api.ApiException
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChildrenListViewModelTest {

    private val kid = Child(1, "Маша", null, monthlyLimit = 1000, consumedThisMonth = 0, remaining = 1000)

    private class FakeRepository(var children: List<Child>) : ColaTrackerRepository {
        var failChildren: Throwable? = null
        var failAdd: Throwable? = null
        var gate: CompletableDeferred<Unit>? = null
        var getCalls = 0
        val requestIds = mutableListOf<String?>()

        override suspend fun getChildren(): Result<List<Child>> {
            getCalls++
            gate?.await()
            return failChildren?.let { Result.failure(it) } ?: Result.success(children)
        }

        override suspend fun addDrink(childId: Int, amountMl: Int, requestId: String?): Result<AddDrinkResponse> {
            requestIds.add(requestId)
            failAdd?.let { failAdd = null; return Result.failure(it) }
            val updated = children.first { it.id == childId }
                .let { it.copy(consumedThisMonth = it.consumedThisMonth + amountMl, remaining = it.remaining - amountMl) }
            children = children.map { if (it.id == childId) updated else it }
            return Result.success(AddDrinkResponse("ok", updated, DrinkHistoryItem(1, childId, amountMl, "2026-10-06T12:00:00")))
        }

        override suspend fun getChildHistory(childId: Int) = Result.success(emptyList<DrinkHistoryItem>())
        override suspend fun deleteDrink(drinkId: Int): Result<DeleteDrinkResponse> = error("не используется")
        override suspend fun uploadPhoto(childId: Int, imageBytes: ByteArray, fileName: String) =
            Result.success(UploadPhotoResponse("ok"))
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `успешная загрузка запоминает время обновления`() {
        var now = 1_000L
        val vm = ChildrenListViewModel(FakeRepository(listOf(kid)), clock = { now })

        assertEquals(1_000L, vm.lastUpdatedMillis.value)
        assertTrue(vm.uiState.value is ChildrenUiState.Success)

        now = 5_000L
        vm.refresh()
        assertEquals(5_000L, vm.lastUpdatedMillis.value)
        assertEquals(false, vm.isRefreshing.value)
    }

    @Test
    fun `неудачное обновление оставляет список и прежнее время, показывает сообщение`() {
        var now = 1_000L
        val repo = FakeRepository(listOf(kid))
        val vm = ChildrenListViewModel(repo, clock = { now })

        repo.failChildren = ApiException("Нет связи", retryable = true)
        now = 9_000L
        vm.refresh()

        assertEquals(1_000L, vm.lastUpdatedMillis.value) // подпись не врёт про свежесть
        assertEquals(listOf(kid), (vm.uiState.value as ChildrenUiState.Success).children)
        assertEquals("Нет связи", vm.message.value?.text)
    }

    @Test
    fun `ошибка первой загрузки даёт состояние ошибки, времени обновления нет`() {
        val repo = FakeRepository(listOf(kid)).apply { failChildren = ApiException("Сервер не ответил") }
        val vm = ChildrenListViewModel(repo)

        assertEquals("Сервер не ответил", (vm.uiState.value as ChildrenUiState.Error).message)
        assertNull(vm.lastUpdatedMillis.value)
    }

    @Test
    fun `повторное обновление во время обновления игнорируется`() {
        val repo = FakeRepository(listOf(kid))
        val vm = ChildrenListViewModel(repo)
        val callsAfterInit = repo.getCalls

        repo.gate = CompletableDeferred()
        vm.refresh()
        assertTrue(vm.isRefreshing.value)
        vm.refresh() // кнопка + жест одновременно
        vm.refresh()

        repo.gate!!.complete(Unit)
        assertEquals(callsAfterInit + 1, repo.getCalls)
        assertEquals(false, vm.isRefreshing.value)
    }

    @Test
    fun `быстрое добавление — сбой сети даёт повтор с тем же ключом`() {
        val repo = FakeRepository(listOf(kid)).apply { failAdd = ApiException("Нет связи", retryable = true) }
        val vm = ChildrenListViewModel(repo)

        vm.quickAddDrink(1, 330)
        val retry = vm.message.value?.retry
        assertNotNull(retry)
        assertEquals(330, retry!!.amountMl)

        vm.clearMessage()
        vm.quickAddDrink(retry.childId, retry.amountMl, retry.requestId)

        assertEquals(listOf(retry.requestId, retry.requestId), repo.requestIds)
        assertEquals(670, (vm.uiState.value as ChildrenUiState.Success).children.first().remaining)
        assertEquals("Маша: добавлено 330 мл", vm.message.value?.text)
    }
}
