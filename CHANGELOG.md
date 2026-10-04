# Changelog

## [2.2] - 2026-07-29

### Сборка (2026-10-04) — проект впервые собран после правок 2.2
- Исправлена ошибка компиляции в `ChildCard.kt` (`consumptionProgress` больше нет;
  бейдж предупреждения считается от расхода месяца). Файл по-прежнему не используется
  и подлежит удалению — см. «Удалено» ниже
- `themes.xml`: стиль `Theme.Material3.DayNight.NoActionBar` наследовал сам себя
  (lint `ResourceCycle` ронял release-сборку). Заменён на `Theme.ColaTracker`
  + тёмный вариант в `values-night`
- R8: добавлены `-dontwarn java.lang.management.*` для Ktor — release-сборка проходит
- Перегенерированы сломанные `gradlew` / `gradlew.bat` (Gradle 8.13)
- `processAndUploadPhoto` больше не проглатывает `CancellationException`
- `versionName` 2.0 → 2.2, `versionCode` 2 → 3
- Добавлены unit-тесты `ChildTest` для производных величин `Child`
- `__pycache__/` убран из git и добавлен в `.gitignore`; макеты `stitch_*` перенесены в `design/`

### Главное: «цель» → «кола-метр»
- Поле `remaining` — накопительный баланс, а не остаток месячной цели. UI больше
  не называет его целью:
  - «Месячная цель семьи … осталось» → «Кола-метр семьи … накоплено»
    (+ показывается пополнение 1-го числа и состояние перерасхода)
  - «СЕГОДНЯШНИЙ FIZZ» (считался за месяц) → «ВЫПИТО В ЭТОМ МЕСЯЦЕ»
  - «Доступно» на экране ребёнка → «Кола-метр (накоплено)» + «хватит на N мес.»
- Цвета и предупреждения считаются от расхода месяца (`consumed / monthly_limit`),
  а не от `remaining / monthly_limit` — раньше индикатор был вечно зелёным,
  а бейдж предупреждения не появлялся никогда
- Показывается перерасход: превышение начисления и уход баланса в минус
- Статичная фраза под процентом заменена на текст по фактическому расходу
- Производные величины вынесены в `Child`: `monthUsageRatio`, `monthUsagePercent`,
  `balanceInMonths`, `isOverdrawn`, `overLimitMl`

### Исправлено
- **Ktor `expectSuccess = true`**: ошибки 401/404/500 больше не превращаются
  в ошибку парсинга JSON. Добавлен маппинг в понятные сообщения (`safeCall`)
- **Системная кнопка «Назад»** больше не закрывает приложение — добавлен `BackHandler`
- **Поворот экрана** не сбрасывает навигацию: состояние в `rememberSaveable`
- **Новое фото не обновлялось** из-за кэша Coil (путь на сервере постоянный) —
  добавлен cache-buster `?v=<timestamp>`
- **Дублирующая загрузка**: вход на экран ребёнка стоил 4 запросов вместо 2
  (`init` + `LaunchedEffect`) — остался один источник загрузки
- `CancellationException` больше не проглатывается — уход с экрана во время
  запроса не показывается как сетевая ошибка
- Тёмная тема: главный экран и настройки использовали константы светлой палитры
  и оставались светлыми. Введён `ColaTheme.containers` (в material3 1.1.2 нет
  ролей `surfaceContainer*`), все экраны переведены на `MaterialTheme.colorScheme`
- `SettingsScreen` использовал `OutlinedTextFieldDefaults` — этого API нет
  в material3 1.1.2 из compose-bom 2024.01.00
- Состояние `Loading` стало достижимым (скелетоны реально показываются),
  убраны костыльные `delay(50)/delay(100)` в `ChildDetailViewModel`
- Удаление записи больше не пересчитывает счётчики вручную — цифры берутся с сервера
- Анимация недельного графика не запускалась (`targetValue` совпадал с начальным)
- Подписи дней разъезжались с точками графика — точки ставятся по центрам слотов
- Даты в графике разбираются с учётом смещения таймзоны, если сервер его пришлёт
- Форматирование объёмов через `Locale.ROOT` (стабильные цифры и разделитель)

### Изменено
- Один `HttpClient` на процесс через `ApiProvider` (раньше каждый ViewModel
  создавал свой, а `ChildDetailViewModel` живёт в сторе Activity и не очищался)
- Убрана дублирующая инициализация Coil в `MainActivity`
- Таймауты разделены: connect 10 с, socket 20 с, request 30 с
- FAB действительно добавляет напиток (330 мл) вместо перехода на экран деталей;
  скрывается, когда список пуст
- Сообщения показываются `Snackbar`'ом вместо `Toast`
- Ошибка обновления не стирает уже показанный список
- `SettingsScreen` переведён на русский и показывает только реальные данные;
  нереализованные настройки честно помечены как «скоро»
- Добавлены отступы под системную навигацию (`navigationBarsPadding`)
- Убрано разрешение `READ_MEDIA_IMAGES` — системный пикер его не требует

### Удалено (мёртвый код)
- `ChildCard.kt` целиком, `ColaTopAppBar`, `ChildDetailHeader`, `CompactStatsRow`,
  `LabeledGradientProgressBar`, `getProgressColor`, `StatisticsCardSkeleton`,
  `healthCheck`, `formatTotalConsumption`, `Child.getFullPhotoUrl`,
  приватный `uploadPhoto` в `ChildDetailViewModel`
- Неиспользуемые зависимости `navigation-compose` и `ktor-client-auth`

---

## [2.1] - 2026-03-24

### Добавлено (Settings Screen)
- Новый экран "Settings" по дизайну Effervescent Archive
  - Карточка Daily Family Goal (tertiary-акцент, фиолетовый)
  - Карточки участников с настройками лимитов и переключателем уведомлений
  - Секция Privacy & Archives (видимость архива, хранение данных, анонимизация)
  - Leave Family Group (danger zone)
- Навигация: профильный аватар на главном экране → Settings
- Маршрут `Screen.Settings` в `MainActivity`

### Безопасность
- AUTH_TOKEN вынесен из исходного кода в `local.properties` → `BuildConfig` (не коммитится в git)
- Заменён `usesCleartextTraffic=true` на `network_security_config.xml` (cleartext только для API-сервера)
- Убрана debug-подпись из release build type
- Ktor логирование отключено в release (`LogLevel.NONE`)
- Удалены `android.util.Log` вызовы из `ChildCard` (утечка URL в logcat)
- Добавлено ProGuard правило для удаления `Log.d/v/i` из release APK

### Исправлено
- HttpClient (CIO) утечка: добавлен `close()` в `onCleared()` обоих ViewModels
- StateFlow мутации из `Dispatchers.IO` в `processAndUploadPhoto` → теперь на Main через `withContext(IO)`
- Тренд в `StatisticsCard` всегда показывал UP (фейковый baseline) → теперь сравнивает первую и вторую половину недели
- `Modifier.padding` на `Canvas` не работал → обёрнут в `Box` с padding

### Добавлено
- FAB открывает bottom sheet для выбора ребёнка (быстрое добавление)
- Линейный индикатор обновления при refresh
- `isRefreshing` состояние в `ChildrenListViewModel`
- `close()` метод в интерфейсе `ColaTrackerRepository`

---

## [2.0] - 2026-03-23

### Добавлено
- Новый дизайн главного экрана "Effervescent Archive"
  - Hero-секция с анимированным круговым прогрессом и % потребления семьи
  - Карточка "Месячная цель семьи" (фиолетовый tertiary-акцент)
  - Карточки участников семьи с аватарами и статистикой
  - Секция "Наблюдения" с динамическими инсайтами
  - FAB (Floating Action Button) с gradient-фоном
- Полупрозрачный TopAppBar с лого и профильным аватаром
- Расширенная цветовая палитра Effervescent Archive
  - Tertiary purple (#7842A5, #D199FF)
  - Иерархия surface-контейнеров (5 уровней)
  - Cola CTA gradient (145°)
- Агрегированная статистика семьи (% использования, остаток, лидер)

### Изменено
- Theme.kt: полная замена палитры на Effervescent Archive
- ChildrenListScreen.kt: полный редизайн (было: список карточек, стало: editorial layout)
- Кнопки используют rounded-full (9999px) стиль

### Сохранено без изменений
- Детальный экран ребёнка (ChildDetailScreen)
- API-клиент и модели данных
- ViewModel логика
- Навигация (Home → Detail)

---

## [1.0] - 2026-03-23

### Первоначальный релиз
- Список детей с прогресс-барами потребления
- Детальный экран: история напитков, добавление, удаление
- Круговой прогресс-аватар с фото
- Загрузка и обрезка фото (камера/галерея)
- Canvas-иконки (стакан, бутылка, банка)
- Градиентный TopAppBar
- Шиммер-анимации загрузки
- Недельный график потребления
- Mock/Real режим через AppConfig
- Поддержка светлой и тёмной темы
- Ktor HTTP-клиент с авторизацией
- ProGuard конфигурация для release
