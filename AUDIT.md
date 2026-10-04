# Аудит проекта Cola Tracker

**Дата:** 2026-07-29
**Версия приложения:** 2.1 (versionCode 2)
**Объём проверки:** весь Android-модуль (`app/`), конфигурация сборки, а также `backend.txt` — в части контракта, который потребляет приложение.

> Отчёт составлен статическим анализом. Собрать проект на этой машине не удалось (см. §8), поэтому пункты помечены как **[подтверждено кодом]** или **[требует проверки на устройстве]**.

---

## СТАТУС: исправления применены (версия 2.2)

Всё, что можно было починить на стороне приложения, — исправлено. Список изменений: [CHANGELOG.md](CHANGELOG.md), раздел `[2.2]`.

**Исправлено:** §1 целиком, §2.1–2.5, §3.1 (частично — нужен эндпоинт), §3.2, §3.3, §3.4 (клиентская часть), §3.5–3.7, §4.1–4.11, PERF-1–PERF-4, §7 целиком, §9 целиком.

**НЕ исправлено — требует изменений на сервере:**

| Пункт | Почему |
|---|---|
| SEC-1 (HTTPS) | Нужен сертификат и reverse-proxy на `<SERVER_IP>`. Убрать cleartext из `network_security_config.xml` до появления HTTPS = полностью сломать приложение. |
| SEC-2 (статический токен в APK) | Нужен эндпоинт логина и выдача короткоживущих токенов. |
| SEC-3 (фото детей без авторизации) | Нужно закрыть `GET /photos/{filename}` токеном и генерировать неугадываемые имена файлов. **Самый серьёзный пункт отчёта.** |
| SEC-4 (CORS `*`) | Правится одной строкой в `backend.txt`. |
| §3.1 (`GET /children/{id}`) | Эндпоинта нет; приложение по-прежнему тянет весь список, но теперь корректно показывает ошибку, если ребёнка удалили. |
| §3.5 (сохранение настроек) | Нужен `PUT /children/{id}`. В приложении неработающие поля заменены честной пометкой «скоро». |
| §3.4 (таймзона, серверная часть) | Бэкенду стоит отдавать `datetime.now(timezone.utc).isoformat()`. Клиент уже готов к обеим формам. |
| §8 (окружение сборки) | `sdk.dir` и отсутствие JDK/SDK — это состояние конкретной машины, а не кода. |

**Не делалось сознательно:** вынос всех строк в `strings.xml` (§4.7). Это ~200 строк в четырёх файлах; выполнять такую механическую правку без возможности собрать проект — значит с высокой вероятностью внести опечатку в ресурсную ссылку. Единый язык интерфейса (§4.6) при этом сделан: `SettingsScreen` переведён на русский.

> ⚠️ **Изменения не компилировались** — на этой машине нет JDK, Android SDK и дистрибутива Gradle (§8). Перед использованием соберите проект в Android Studio.

---

## 0. Краткое резюме

| Приоритет | Что | Кол-во |
|---|---|---|
| **P0** | Ломает сценарии пользователя | 5 |
| **P1** | Ошибки логики и данных | 7 |
| **P2** | UI/UX и косметика | 9 |
| **SEC** | Безопасность и приватность | 4 |
| **PERF** | Производительность / ресурсы | 4 |

Три главные вещи, которые стоит сделать первыми:

1. **Разобраться с семантикой `remaining`** — UI называет накопительный баланс «месячной целью». Это и есть то, что вы заметили (§1).
2. **Включить `expectSuccess` в Ktor** — сейчас ошибка 401/404/500 превращается в невнятную ошибку парсинга JSON. Это самый вероятный источник «иногда вижу ошибки» (§2.1).
3. **Обработать системную кнопку «Назад»** — сейчас она закрывает приложение вместо возврата на список (§2.2).

---

## 1. Главное: «цель» — это не цель, это кола-метр

### 1.1 Как на самом деле работает бэкенд

`backend.txt:122-157` — ключевая логика:

```python
if current_date.month != last_reset_date.month or ...:
    for child in data["children"]:
        child["consumed_this_month"] = 0
        child["remaining"] += child["monthly_limit"]   # НАКОПЛЕНИЕ, а не сброс!
```

Значит, поля означают вот что:

| Поле | Реальный смысл | Как называть в UI |
|---|---|---|
| `monthly_limit` | **Ежемесячное начисление**: сколько мл добавляется к балансу 1-го числа | «Начисление в месяц» |
| `consumed_this_month` | Выпито с начала месяца (обнуляется 1-го числа) | «Выпито в этом месяце» |
| `remaining` | **Накопленный баланс. Не сгорает, переносится из месяца в месяц** | «Баланс» / «Кола-метр» |

Из этого следуют два важных факта, которых UI сейчас не учитывает:

- **`remaining` может быть больше `monthly_limit`** — если ребёнок пил мало, за 3 месяца баланс станет 3 × лимита.
- **`remaining` может быть отрицательным** — бэкенд разрешает уход в минус (`backend.txt:246-248` только пишет warning в лог).
- **`remaining ≠ monthly_limit − consumed_this_month`.** Эти две величины вообще не связаны напрямую.

### 1.2 Где UI врёт [подтверждено кодом]

**a) Карточка «Месячная цель семьи»** — [ChildrenListScreen.kt:481](app/src/main/kotlin/com/colatracker/ui/screens/ChildrenListScreen.kt:481)

```kotlin
Text(text = "Месячная цель семьи", ...)
Text(text = "Лимит на $childrenCount чел.", ...)
Text(text = formatConsumption(totalRemaining), ...)  // ← сумма накопленных балансов
Text(text = "осталось", ...)
```

Показывается `sum(remaining)` — накопленный за всё время баланс семьи — под заголовком «Месячная **цель**» и подписью «осталось». Пользователь читает это как «осталось до конца месяца», а на самом деле это «накоплено всего». Через несколько месяцев экономии число вырастет выше месячного лимита, и карточка станет откровенно бессмысленной.

**b) Hero-секция «СЕГОДНЯШНИЙ FIZZ»** — [ChildrenListScreen.kt:377-421](app/src/main/kotlin/com/colatracker/ui/screens/ChildrenListScreen.kt:377)

```kotlin
val familyFizzPercent = (totalConsumed.toFloat() / totalLimit * 100f).toInt()
...
Text(text = "СЕГОДНЯШНИЙ FIZZ", ...)
```

`totalConsumed` — это `consumed_this_month`, то есть **за месяц**, а не за сегодня. Слово «СЕГОДНЯШНИЙ» неверно. Данных за сегодня в этом расчёте нет вообще.

**c) Статический текст под процентом** — [ChildrenListScreen.kt:416](app/src/main/kotlin/com/colatracker/ui/screens/ChildrenListScreen.kt:416)

```kotlin
Text(text = "Отлично! Держите баланс сегодня.", ...)
```

Всегда одна и та же фраза — и при 5%, и при 200%. Рядом (в «Наблюдениях», строка 695) уже есть готовая логика с четырьмя вариантами текста — стоит переиспользовать её здесь.

**d) Цвет и бейдж «Мало» считаются от неправильной базы**

- [ChildCard.kt:325-332](app/src/main/kotlin/com/colatracker/ui/components/ChildCard.kt:325) — `getRemainingColor(remaining, limit)` делит `remaining / monthly_limit`. При накопленном балансе это почти всегда > 0.5 → **всегда зелёный**, независимо от того, сколько выпито в этом месяце.
- [ChildCard.kt:66](app/src/main/kotlin/com/colatracker/ui/components/ChildCard.kt:66) — `showWarningBadge = child.remaining < child.monthlyLimit * 0.2f` — по той же причине бейдж «Мало» практически никогда не появится.
- Та же формула в [CircularProgressAvatar.kt:230-234](app/src/main/kotlin/com/colatracker/ui/components/CircularProgressAvatar.kt:230).

**e) На экране ребёнка два разных смысла рядом**

[ChildDetailScreen.kt:284-296](app/src/main/kotlin/com/colatracker/ui/screens/ChildDetailScreen.kt:284): счётчик «Доступно: N мл» показывает `remaining` (баланс), а прямо под ним «Месячный лимит: consumed / limit» показывает месячное потребление. Две разные системы координат без пояснения.

### 1.3 Что предлагаю сделать

**Вариант А (минимальный, только тексты).** Переименовать так, чтобы слова совпадали со смыслом:

| Сейчас | Предлагаю |
|---|---|
| «Месячная цель семьи» | «Кола-метр семьи» |
| «осталось» (под суммой балансов) | «накоплено» |
| «Лимит на N чел.» | «+{totalLimit} мл 1-го числа» |
| «СЕГОДНЯШНИЙ FIZZ» | «ВЫПИТО В ЭТОМ МЕСЯЦЕ» |
| «Доступно» (детальный экран) | «Баланс» |
| «Осталось: X» (карточка ребёнка) | «Баланс: X» |

**Вариант Б (рекомендую).** Дополнительно развести две метрики визуально и починить формулы:

1. **Кольцо/полоса прогресса** = `consumed_this_month / monthly_limit` — это и есть «месячный расход». Оставить как есть, но подписать «расход месяца».
2. **Кола-метр** = отдельный блок с `remaining` и понятной производной метрикой — на сколько месяцев хватит:

```kotlin
// Child.kt — добавить
/** Накопленный баланс в «месяцах начисления»: 2.4 = хватит примерно на 2.4 месяца */
val balanceInMonths: Float
    get() = if (monthlyLimit > 0) remaining.toFloat() / monthlyLimit else 0f

/** Баланс ушёл в минус (бэкенд это допускает) */
val isOverdrawn: Boolean get() = remaining < 0
```

3. **Цвет и предупреждения считать от месячного расхода, а не от баланса:**

```kotlin
// ChildCard.kt — заменить getRemainingColor(child.remaining, child.monthlyLimit)
@Composable
fun getMonthUsageColor(consumed: Int, limit: Int): Color {
    val p = if (limit > 0) consumed.toFloat() / limit else 0f
    return when {
        p <= 0.5f -> ProgressGreen
        p <= 0.8f -> ProgressYellow
        else -> ProgressRed
    }
}

// бейдж «Мало» — по остатку месячного начисления, либо по уходу баланса в минус
val showWarningBadge = child.isOverdrawn ||
        child.consumedThisMonth > child.monthlyLimit * 0.8f
```

4. **Показывать перерасход.** Сейчас `consumptionProgress` жёстко зажат в 0..100 ([Child.kt:32-37](app/src/main/kotlin/com/colatracker/data/models/Child.kt:32)), поэтому 150% выглядят так же, как ровно 100%. Стоит выводить «150% (перерасход 1500 мл)» отдельной строкой.

---

## 2. P0 — ломает пользовательские сценарии

### 2.1 Ошибки HTTP превращаются в ошибки парсинга JSON [подтверждено кодом]

[ColaTrackerApi.kt:24-54](app/src/main/kotlin/com/colatracker/data/api/ColaTrackerApi.kt:24) — в конфигурации клиента **не задан `expectSuccess`**, а по умолчанию в Ktor 2.x он `false`. То есть при ответе 401 (неверный токен), 404 или 500 исключение **не** бросается. Вместо этого тело ошибки

```json
{"detail": "Неверный токен авторизации"}
```

пытается десериализоваться в `List<Child>` — и падает с `JsonConvertException`. Пользователь на экране видит что-то вроде:

> Ошибка загрузки списка детей: Expected start of the array '[', but had '{' instead...

**Это самый вероятный кандидат на «иногда вижу ошибки».** Настоящая причина (протух токен / сервер лежит / ребёнка удалили) полностью скрыта.

**Фикс:**

```kotlin
private val client = HttpClient(CIO) {
    expectSuccess = true          // ← добавить
    ...
}

// и человекопонятные сообщения:
private suspend fun <T> safeCall(what: String, block: suspend () -> T): T = try {
    block()
} catch (e: ClientRequestException) {
    val detail = runCatching {
        Json.decodeFromString<ErrorResponse>(e.response.bodyAsText()).detail
    }.getOrNull()
    throw ApiException(
        when (e.response.status) {
            HttpStatusCode.Unauthorized -> "Неверный токен авторизации. Проверьте API_AUTH_TOKEN."
            HttpStatusCode.NotFound     -> detail ?: "Запись не найдена"
            else                        -> detail ?: "$what: ${e.response.status}"
        }, e
    )
} catch (e: ServerResponseException) {
    throw ApiException("Сервер недоступен (${e.response.status.value}). Попробуйте позже.", e)
} catch (e: HttpRequestTimeoutException) {
    throw ApiException("Превышено время ожидания. Проверьте соединение.", e)
} catch (e: IOException) {
    throw ApiException("Нет связи с сервером. Проверьте интернет.", e)
}
```

Класс `ErrorResponse` уже есть в [ApiModels.kt:58](app/src/main/kotlin/com/colatracker/data/models/ApiModels.kt:58), но **нигде не используется** — как раз для этого он и нужен.

### 2.2 Системная кнопка «Назад» закрывает приложение [подтверждено кодом]

Навигация в [MainActivity.kt:67-120](app/src/main/kotlin/com/colatracker/MainActivity.kt:67) сделана на `var currentScreen by remember { mutableStateOf(...) }` без `NavHost`. `BackHandler` не используется нигде в проекте (проверено grep'ом). Значит, на экране «Детали ребёнка» или «Настройки» системная кнопка «Назад» / свайп сразу **выходит из приложения**.

**Фикс:**

```kotlin
// внутри ColaTrackerApp(), после объявления currentScreen
BackHandler(enabled = currentScreen !is Screen.ChildrenList) {
    if (currentScreen is Screen.ChildDetail) shouldRefresh = true
    currentScreen = Screen.ChildrenList
}
```

### 2.3 Поворот экрана сбрасывает навигацию [подтверждено кодом]

Там же: `remember`, а не `rememberSaveable`. При повороте / смене темы / освобождении памяти пользователя выкидывает с экрана ребёнка на список. Данные не теряются (ViewModel живёт), но место — да.

**Фикс:** хранить id ребёнка в `rememberSaveable`, а сам объект `Child` доставать из списка ViewModel:

```kotlin
var currentChildId by rememberSaveable { mutableStateOf<Int?>(null) }
var showSettings by rememberSaveable { mutableStateOf(false) }
```

### 2.4 Новое фото не появляется — показывается старое из кэша [подтверждено кодом]

Бэкенд сохраняет файл под **постоянным** именем ([backend.txt:382](backend.txt)):

```python
filename = f"child_{child_id}{file_extension}"
child_found["photo_url"] = f"{PHOTOS_DIR}/{filename}"    # photos/child_1.jpg — всегда одинаково
```

Приложение строит URL как `"${AppConfig.BASE_URL}/${child.photoUrl}"` (в четырёх местах: [ChildCard.kt:298](app/src/main/kotlin/com/colatracker/ui/components/ChildCard.kt:298), [CircularProgressAvatar.kt:118](app/src/main/kotlin/com/colatracker/ui/components/CircularProgressAvatar.kt:118), [ChildrenListScreen.kt:182](app/src/main/kotlin/com/colatracker/ui/screens/ChildrenListScreen.kt:182), [SettingsScreen.kt:270](app/src/main/kotlin/com/colatracker/ui/screens/SettingsScreen.kt:270)). URL после перезагрузки фото не меняется → Coil отдаёт картинку из памяти/диска → **пользователь загрузил новое фото, а видит старое**. Выглядит как «загрузка не сработала».

**Фикс на стороне приложения** — cache-buster:

```kotlin
// один общий хелпер вместо четырёх копий конкатенации
fun Child.photoRequestUrl(version: Long = 0L): String? =
    photoUrl?.let { "${AppConfig.BASE_URL}/$it" + if (version > 0) "?v=$version" else "" }
```

и в `ChildDetailViewModel.processAndUploadPhoto` после успеха класть `System.currentTimeMillis()` в `StateFlow`, который прокидывается в `photoRequestUrl`. Правильнее — попросить бэкенд отдавать имя с хэшем/меткой времени.

### 2.5 На каждый вход в экран ребёнка уходит 4 запроса вместо 2 [подтверждено кодом]

- [ChildDetailViewModel.kt:66-68](app/src/main/kotlin/com/colatracker/viewmodels/ChildDetailViewModel.kt:66): `init { loadChildData() }`
- [ChildDetailScreen.kt:77-79](app/src/main/kotlin/com/colatracker/ui/screens/ChildDetailScreen.kt:77): `LaunchedEffect(Unit) { viewModel.refresh() }` — а `refresh()` это тот же `loadChildData()`

При первом открытии оба срабатывают → `GET /children` и `GET /children/{id}/history` уходят по два раза. Удвоенная нагрузка и удвоенный шанс словить сетевую ошибку.

**Фикс:** убрать `LaunchedEffect(Unit)` из Composable, оставить только `init`. Если нужно обновление при возврате на экран — вешать на `Lifecycle.Event.ON_RESUME`.

---

## 3. P1 — ошибки логики и данных

### 3.1 `ChildDetailViewModel` тянет весь список ради одного ребёнка

[ChildDetailViewModel.kt:84-89](app/src/main/kotlin/com/colatracker/viewmodels/ChildDetailViewModel.kt:84):

```kotlin
val childResult = repository.getChildren()          // грузим ВСЕХ
val freshChild = children.find { it.id == childId } ?: initialChild
```

В API нет `GET /children/{id}` — стоит добавить на бэкенде. Плюс: если ребёнка удалили на сервере, приложение молча покажет устаревшие данные из `initialChild` вместо сообщения «ребёнок не найден».

### 3.2 Локальный пересчёт после удаления записи может разойтись с сервером

[ChildDetailViewModel.kt:162-165](app/src/main/kotlin/com/colatracker/viewmodels/ChildDetailViewModel.kt:162) пересчитывает `consumedThisMonth`/`remaining` руками. Сейчас формула совпадает с бэкендом ([backend.txt:325-330](backend.txt)), но это дублирование бизнес-логики в двух местах — при любом изменении на сервере цифры разъедутся. Эндпоинт `DELETE /drinks/{id}` уже возвращает достаточно данных; правильнее вернуть обновлённого ребёнка целиком (изменение на бэкенде) либо просто перезапросить данные.

### 3.3 Костыльные `delay()` вместо решения проблемы

[ChildDetailViewModel.kt:133, 138, 143](app/src/main/kotlin/com/colatracker/viewmodels/ChildDetailViewModel.kt:133):

```kotlin
kotlinx.coroutines.delay(50)   // «чтобы избежать state change во время measure»
...
kotlinx.coroutines.delay(100)  // «чтобы избежать remeasure во время draw»
```

Плюс комментарии в [ChildrenListViewModel.kt:37-39](app/src/main/kotlin/com/colatracker/viewmodels/ChildrenListViewModel.kt:37) и [ChildDetailViewModel.kt:80-81](app/src/main/kotlin/com/colatracker/viewmodels/ChildDetailViewModel.kt:80): «не переключаем в Loading, чтобы избежать layout crash». Это следы борьбы с каким-то реальным падением, но лечение симптома, а не причины. Побочный эффект: **состояние `Loading` в обоих ViewModel фактически недостижимо** — оно объявлено, ветки `is Loading -> LoadingContent()` в UI написаны, скелетоны нарисованы, но никогда не показываются. Пользователь при первой загрузке видит пустой экран «Пока никого нет», а потом внезапно список.

Рекомендую воспроизвести исходный краш (какой именно был?) и починить его, после чего вернуть нормальный `Loading` и убрать `delay`. Скорее всего, дело было в `Modifier.verticalScroll` внутри `LazyColumn` или в изменении состояния прямо во время композиции.

### 3.4 Часовые пояса в недельном графике

[StatisticsCard.kt:67-87](app/src/main/kotlin/com/colatracker/ui/components/StatisticsCard.kt:67) сравнивает `LocalDate.parse(timestamp)` (время **сервера**, без таймзоны — `datetime.now().isoformat()`) с `LocalDate.now()` (время **устройства**). Если сервер в UTC, а пользователь в UTC+4, записи после 20:00 попадут в «вчера». Стоит отдавать с бэкенда время в ISO с таймзоной и парсить через `OffsetDateTime`.

### 3.5 Настройки ничего не сохраняют

Весь [SettingsScreen.kt](app/src/main/kotlin/com/colatracker/ui/screens/SettingsScreen.kt) — локальный `remember`-стейт:

- `familyGoal = "12"` — хардкод, не приходит с сервера (строка 51);
- кнопка «Update Global Limit» — `onClick = { /* TODO */ }` (строка 213);
- «Leave Family Group» — `onClick = { /* TODO */ }` (строка 559);
- всё сбрасывается при уходе с экрана, потому что `remember` умирает вместе с композицией.

Для пользователя это выглядит как сломанные настройки. Либо реализовать (нужен `PUT /children/{id}` на бэкенде), либо явно пометить как «скоро».

### 3.6 «DAILY LIMIT» считается из месячных миллилитров

[SettingsScreen.kt:59](app/src/main/kotlin/com/colatracker/ui/screens/SettingsScreen.kt:59):

```kotlin
childLimits[child.id] = ((child.monthlyLimit ?: 5000) / 1000).toString()
```

и подписывается как `"$dailyLimit servings"` (строка 323). То есть месячный лимит 5000 мл превращается в «5 servings **в день**». Цифра не значит ничего.

Заодно: `child.monthlyLimit` имеет тип `Int` (не `Int?`), поэтому `?: 5000` — мёртвый код, компилятор выдаёт предупреждение *«Elvis operator always returns the left operand»*.

### 3.7 Ответ `UploadPhotoResponse.photoUrl` не используется

[ChildDetailViewModel.kt:236-240](app/src/main/kotlin/com/colatracker/viewmodels/ChildDetailViewModel.kt:236) — сервер уже вернул новый `photo_url`, но вместо того чтобы применить его к состоянию, делается полный `loadChildData()` (ещё два сетевых запроса). См. также §2.4.

---

## 4. P2 — UI/UX

| # | Проблема | Файл |
|---|---|---|
| 4.1 | **Тёмная тема работает наполовину.** Главный экран и Настройки жёстко используют константы светлой палитры (`ColaSurface`, `ColaOnSurface`, `ColaSurfaceContainerLow`…), а экран ребёнка — `MaterialTheme.colorScheme`. В тёмной теме первые два остаются светлыми, третий темнеет. Нужно везде перейти на `MaterialTheme.colorScheme.*` | [ChildrenListScreen.kt:66](app/src/main/kotlin/com/colatracker/ui/screens/ChildrenListScreen.kt:66), [SettingsScreen.kt:70](app/src/main/kotlin/com/colatracker/ui/screens/SettingsScreen.kt:70) |
| 4.2 | **FAB «Добавить» ничего не добавляет** — открывает bottom sheet «Добавить напиток», но выбор ребёнка просто переводит на экран деталей. Либо переименовать в «Выбрать ребёнка», либо сделать реальное быстрое добавление 330 мл прямо из шторки | [ChildrenListScreen.kt:106](app/src/main/kotlin/com/colatracker/ui/screens/ChildrenListScreen.kt:106) |
| 4.3 | **FAB молча не реагирует**, когда список пуст (`if (children.isNotEmpty())`). Лучше скрывать кнопку или показывать подсказку | [ChildrenListScreen.kt:107](app/src/main/kotlin/com/colatracker/ui/screens/ChildrenListScreen.kt:107) |
| 4.4 | **Анимация графика мертва**: `animateFloatAsState(targetValue = 1f)` без изменения значения не анимируется никогда — начальное состояние равно целевому | [StatisticsCard.kt:186](app/src/main/kotlin/com/colatracker/ui/components/StatisticsCard.kt:186) |
| 4.5 | **Подписи дней не совпадают с точками графика**: точки рисуются по `stepX = width/(n-1)`, а подписи раскладываются `Arrangement.SpaceBetween` с учётом ширины текста | [StatisticsCard.kt:281](app/src/main/kotlin/com/colatracker/ui/components/StatisticsCard.kt:281) |
| 4.6 | **Смесь языков**: главный экран и детали — по-русски, Настройки — по-английски («Settings», «Member», «DAILY LIMIT», «Leave Family Group») | [SettingsScreen.kt](app/src/main/kotlin/com/colatracker/ui/screens/SettingsScreen.kt) |
| 4.7 | **Все строки захардкожены в Kotlin**, `strings.xml` содержит только `app_name`. Локализация и правки текстов невозможны без пересборки | [strings.xml](app/src/main/res/values/strings.xml) |
| 4.8 | **Ошибки показываются `Toast`'ом, который тут же исчезает** и дублируется в `_message`. Для сетевых ошибок лучше `Snackbar` с кнопкой «Повторить» | [ChildDetailScreen.kt:127-136](app/src/main/kotlin/com/colatracker/ui/screens/ChildDetailScreen.kt:127) |
| 4.9 | **Нет отступа под навбар** на экране ребёнка: `verticalScroll` без `navigationBarsPadding()`, последняя карточка истории может уходить под системную панель | [ChildDetailScreen.kt:202](app/src/main/kotlin/com/colatracker/ui/screens/ChildDetailScreen.kt:202) |
| 4.10 | `String.format("%.1f л", ...)` без `Locale` — в локалях с другой цифровой системой даст неожиданный результат; lint ругается | [ChildrenListScreen.kt:780](app/src/main/kotlin/com/colatracker/ui/screens/ChildrenListScreen.kt:780), [StatisticsCard.kt:387](app/src/main/kotlin/com/colatracker/ui/components/StatisticsCard.kt:387), [ColaTopAppBar.kt:169](app/src/main/kotlin/com/colatracker/ui/components/ColaTopAppBar.kt:169) |
| 4.11 | **Ошибка компиляции (не просто стиль):** `SettingsScreen` использовал `OutlinedTextFieldDefaults.colors(...)` — этот API появился только в material3 **1.2.0**, а `compose-bom:2024.01.00` приносит **1.1.2**. Кастомные цвета полей убраны вместе с самими полями. По той же причине в проекте нельзя использовать `HorizontalDivider` (только `Divider`) и роли `surfaceContainer*` в `ColorScheme` — для них заведён `ColaTheme.containers`. *Поправка к первой редакции отчёта: `Divider` и `menuAnchor()` в 1.1.2 не устаревшие, они корректны.* | `SettingsScreen.kt` |

---

## 5. Безопасность и приватность

### SEC-1 — Токен уходит по открытому HTTP на публичный IP **(критично)**

[network_security_config.xml](app/src/main/res/xml/network_security_config.xml) разрешает cleartext для `<SERVER_IP>` — это **публичный адрес в интернете**, а не локальная разработка. Значит, `Authorization: Bearer <token>`, имена детей и фотографии передаются открытым текстом и читаются любым, кто находится в той же Wi-Fi-сети.

Правильно: поднять HTTPS (Let's Encrypt / Caddy / nginx), оставить cleartext только для `10.0.2.2` и `localhost`.

### SEC-2 — Единый статический токен зашит в APK

`BuildConfig.API_AUTH_TOKEN` ([build.gradle.kts:33](app/build.gradle.kts:33)) — это строковая константа в DEX. Вытаскивается из APK за минуту (`apktool`, `strings`). Токен один на всех и не имеет срока годности — значит, полный доступ к данным всех детей. Вынос в `local.properties` (сделан в 2.1) защищает только от утечки через git, но не от анализа APK.

Правильно: логин/пароль → короткоживущий токен → хранение в `EncryptedSharedPreferences`.

### SEC-3 — Фотографии детей доступны без авторизации

[backend.txt:416-421](backend.txt) — `GET /photos/{filename}` намеренно без токена, а имена файлов предсказуемы: `child_1.jpg`, `child_2.jpg`, … На публичном IP это значит, что любой желающий перебором получает фотографии детей. Для детского приложения это самое серьёзное из всего списка.

Правильно: закрыть эндпоинт токеном и генерировать неугадываемые имена файлов (uuid4).

### SEC-4 — CORS `allow_origins=["*"]`

[backend.txt:27](backend.txt) — для мобильного клиента CORS не нужен вовсе; в паре с bearer-токеном это лишняя поверхность атаки.

---

## 6. Производительность и ресурсы

### PERF-1 — Несколько HTTP-клиентов Ktor одновременно

Каждый ViewModel создаёт **свой** `ColaTrackerApi()` → свой `HttpClient(CIO)` со своим пулом потоков и селектором:

- [ChildrenListViewModel.kt:31-35](app/src/main/kotlin/com/colatracker/viewmodels/ChildrenListViewModel.kt:31)
- [ChildDetailViewModel.kt:38-42](app/src/main/kotlin/com/colatracker/viewmodels/ChildDetailViewModel.kt:38)

Хуже: `ChildDetailViewModel` создаётся через `viewModel(key = "child_${child.id}")` ([ChildDetailScreen.kt:67](app/src/main/kotlin/com/colatracker/ui/screens/ChildDetailScreen.kt:67)), а владелец стора — Activity. Поэтому при переходе назад ViewModel **не очищается**, `onCleared()` не вызывается, `close()` не срабатывает. Открыли пятерых детей — пять живых HttpClient до конца жизни Activity.

**Фикс:** один общий клиент на процесс.

```kotlin
// data/api/ColaTrackerApi.kt
object ApiProvider {
    val api: ColaTrackerApi by lazy { ColaTrackerApi() }
    val repository: ColaTrackerRepository by lazy {
        if (AppConfig.USE_MOCK_DATA) MockRepository() else RealRepository(api)
    }
}
```

и убрать `close()` из `onCleared()` обоих ViewModel.

### PERF-2 — Дублирующая инициализация Coil

[ColaTrackerApp.kt:8-19](app/src/main/kotlin/com/colatracker/ColaTrackerApp.kt:8) реализует `ImageLoaderFactory`, и одновременно [MainActivity.kt:35-40](app/src/main/kotlin/com/colatracker/MainActivity.kt:35) в `onCreate` собирает **ещё один** `ImageLoader` и ставит его глобально. При каждом пересоздании Activity создаётся новый загрузчик и теряется кэш. Достаточно `Application`-варианта, код из `MainActivity` удалить.

### PERF-3 — Таймауты по 30 секунд

[AppConfig.kt:27](app/src/main/kotlin/com/colatracker/AppConfig.kt:27) — один и тот же `REQUEST_TIMEOUT_MS = 30_000` на connect / socket / request. 30 секунд ожидания подключения — очень долго для мобильного UX. Разумнее: connect 10 с, request 30 с.

### PERF-4 — Полная перезагрузка вместо точечного обновления

`processAndUploadPhoto` → `loadChildData()` (§3.7); возврат с экрана деталей → `refresh()` всего списка ([MainActivity.kt:79-84](app/src/main/kotlin/com/colatracker/MainActivity.kt:79)). Для 2-3 детей терпимо, но лишнего трафика хватает.

---

## 7. Мёртвый код

Объявлено, но нигде не вызывается (проверено grep'ом по всему `app/src`):

| Символ | Файл |
|---|---|
| `ChildCard()` — целиком, ~330 строк | [ChildCard.kt:43](app/src/main/kotlin/com/colatracker/ui/components/ChildCard.kt:43) |
| `ColaTopAppBar()` | [ColaTopAppBar.kt:37](app/src/main/kotlin/com/colatracker/ui/components/ColaTopAppBar.kt:37) |
| `formatTotalConsumption()` | [ColaTopAppBar.kt:167](app/src/main/kotlin/com/colatracker/ui/components/ColaTopAppBar.kt:167) |
| `ChildDetailHeader()` | [CircularProgressAvatar.kt:219](app/src/main/kotlin/com/colatracker/ui/components/CircularProgressAvatar.kt:219) |
| `CompactStatsRow()` | [StatisticsCard.kt:397](app/src/main/kotlin/com/colatracker/ui/components/StatisticsCard.kt:397) |
| `LabeledGradientProgressBar()`, `getProgressColor()` | [GradientProgressBar.kt:207](app/src/main/kotlin/com/colatracker/ui/components/GradientProgressBar.kt:207) |
| `StatisticsCardSkeleton()` | [ShimmerEffect.kt:242](app/src/main/kotlin/com/colatracker/ui/components/ShimmerEffect.kt:242) |
| `ColaTrackerApi.healthCheck()` | [ColaTrackerApi.kt:141](app/src/main/kotlin/com/colatracker/data/api/ColaTrackerApi.kt:141) |
| `ChildDetailViewModel.uploadPhoto()` (private, не используется) | [ChildDetailViewModel.kt:271](app/src/main/kotlin/com/colatracker/viewmodels/ChildDetailViewModel.kt:271) |
| `Child.getFullPhotoUrl()` — вместо него везде ручная конкатенация | [Child.kt:42](app/src/main/kotlin/com/colatracker/data/models/Child.kt:42) |
| `ErrorResponse` | [ApiModels.kt:58](app/src/main/kotlin/com/colatracker/data/models/ApiModels.kt:58) |

Плюс неиспользуемые импорты в [ChildDetailScreen.kt:5-7](app/src/main/kotlin/com/colatracker/ui/screens/ChildDetailScreen.kt:5) (`Bitmap`, `BitmapFactory`, `ByteArrayOutputStream`) и в шапках нескольких файлов. Также в зависимостях подключены `navigation-compose` и `ktor-client-auth`, которые в коде не используются ([build.gradle.kts:85, 97](app/build.gradle.kts:85)).

Отдельно: три папки `stitch_add_intake/`, `stitch_add_intake (1)/`, `stitch_add_intake_2/` с макетами лежат в корне проекта — стоит убрать в `design/` или удалить.

---

## 8. Окружение сборки — на этой машине проект не соберётся

Проверено фактически:

1. **JDK нет в `PATH`.** `gradlew.bat` падает с `'java.exe' is not recognized`. Единственная найденная JVM — `C:\Users\<user>\AppData\Local\Programs\PyCharm 2026.2.0.1\jbr\bin\java.exe` (JBR 25).
2. **`sdk.dir` в `local.properties` указывает на чужой путь:**
   ```
   sdk.dir=C:\Users\<user>\AppData\Local\Android\Sdk
   ```
   Текущий пользователь — `<user>`. Путь не существует. Gradle упадёт с *«SDK location not found»*.
3. **Android SDK на машине не установлен** — `%LOCALAPPDATA%\Android\Sdk` отсутствует.
4. **Дистрибутив Gradle 8.13 не скачан** — `~/.gradle/wrapper/dists` пуст.

Если сборка ведётся на другой машине — всё в порядке, просто поправьте `sdk.dir` здесь. Если на этой — нужны JDK 17+, Android SDK (API 34) и первая онлайн-сборка для скачивания Gradle.

Отдельно: **JBR 25 не подойдёт** для AGP 8.13 — нужен JDK 17 или 21.

---

## 9. Расхождения в документации

- [CLAUDE.md](CLAUDE.md) в разделе «API Endpoints» указывает `DELETE /drink/{id}`. Реально и в коде, и на бэкенде — `DELETE /drinks/{id}` (множественное число).
- [CLAUDE.md](CLAUDE.md) описывает навигацию как «NavHost с маршрутами `children_list` → `child_detail/{childJson}`». В коде NavHost **нет** — навигация ручная через `sealed class Screen` ([MainActivity.kt:58](app/src/main/kotlin/com/colatracker/MainActivity.kt:58)). Зависимость `navigation-compose` осталась неиспользованной.
- [CLAUDE.md](CLAUDE.md) не упоминает `SettingsScreen`, хотя он есть с версии 2.1.
- В структуре проекта не упомянут `ColaTrackerApp.kt` (класс `Application`).
- Нигде в документации не зафиксировано ключевое свойство домена — **накопительный характер `remaining`**. Именно поэтому оно и потерялось в UI (§1).

---

## 10. Предлагаемый план работ

**Спринт 1 — «перестать врать пользователю и падать» (0.5–1 день)**

1. `expectSuccess = true` + человекопонятные ошибки (§2.1)
2. `BackHandler` (§2.2)
3. Убрать дублирующий `LaunchedEffect(Unit)` (§2.5)
4. Переименовать «цель» → «кола-метр», починить формулы цвета и бейджа (§1.3, вариант А + пункт 3)
5. Cache-buster для фото (§2.4)

**Спринт 2 — консистентность (1–2 дня)**

6. Единый `HttpClient` через `ApiProvider` (PERF-1)
7. `rememberSaveable` для навигации (§2.3)
8. Тёмная тема: везде `MaterialTheme.colorScheme` (§4.1)
9. Вернуть состояние `Loading`, убрать `delay()`-костыли (§3.3)
10. Вычистить мёртвый код (§7)

**Спринт 3 — функциональность и безопасность**

11. HTTPS на бэкенде + закрыть `/photos` токеном (SEC-1, SEC-3)
12. `GET /children/{id}` и `PUT /children/{id}` на бэкенде; оживить Настройки (§3.1, §3.5)
13. Реальное быстрое добавление из FAB (§4.2)
14. Строки в `strings.xml`, один язык интерфейса (§4.6, §4.7)
15. Timestamp с таймзоной (§3.4)

---

*Отчёт сгенерирован при статическом анализе исходников. Пункты §2.4, §3.3 и §4.4 желательно дополнительно подтвердить на реальном устройстве.*
