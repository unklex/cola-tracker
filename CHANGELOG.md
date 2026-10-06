# Changelog

## [2.3] - 2026-10-06

### Главное: приложение больше не падает при добавлении напитка
- **Падение при нажатии «250 мл» / «330 мл»** (и при обновлении списка, быстром добавлении,
  загрузке фото): `NoSuchMethodError KeyframesSpecConfig.at` в неопределённом
  `LinearProgressIndicator`. Причина — `compose-bom 2024.01.00` тянул `material3 1.1.2` вместе
  с `compose-animation 1.6.0`, эта пара бинарно несовместима. BOM обновлён до `2024.06.00`
  (material3 1.2.1). Регрессию ловит UI-тест `QuickAmountSelectorTest`

### Добавлено
- История на экране ребёнка сгруппирована по дням: «Сегодня · 580 мл», «Вчера», дата;
  внутри группы показывается только время (`groupHistoryByDay`, `dayLabel`)
- Тесты: `ChildDetailViewModelTest` (7, на фейковом репозитории), `DrinkHistoryItemTest` (5),
  UI-тест `QuickAmountSelectorTest` (3, эмулятор). Всего unit-тестов 17

### Изменено
- **Только HTTPS**: приложение ходит на `https://cola.st77.ru`. Cleartext HTTP разрешён
  только в debug-сборке (`app/src/debug/res/xml/network_security_config.xml`)
- `network_security_config.xml` без `<domain-config>`: Ktor CIO вызывает `checkServerTrusted`
  без имени хоста, и Android бросает «Domain specific configurations require that hostname
  aware checkServerTrusted…» — любой HTTPS-запрос падал
- Адрес сервера и токен не хранятся в репозитории: дефолт `API_BASE_URL` — `10.0.2.2`,
  реальные значения только в `local.properties`
- `ChildDetailViewModel` принимает репозиторий параметром (по умолчанию `ApiProvider.repository`)
- `MockRepository`: баланс может уйти в минус, как на реальном бэкенде
- `versionName` 2.2 → 2.3, `versionCode` 3 → 4

### Бэкенд v3 (SQLite, Docker) — `backend/`
- Хранение в SQLite вместо `data.json`; API совместим с v2, приложение менять не нужно
- Транзакции (`BEGIN IMMEDIATE`): параллельные запросы больше не теряют записи
- ID записей не переиспользуются (в v2 `len()+1` давал дубликаты: в боевых данных были
  дубли id 2 и 51)
- Расход месяца считается по истории, а не отдельным счётчиком: удаление записи за прошлый
  месяц больше не портит текущий расход
- Месячное пополнение начисляется за все пропущенные месяцы, а не за один
- Валидация суммы (1–5000 мл), сравнение токена за постоянное время, защита `/photos`
  от path traversal, лимит фото 10 МБ, токен не зашит в код
- `manage.py`: `import-json` (перенос из `data.json`, дубликаты id чинятся без каскада),
  `add-child`, `list`. 15 тестов на временной базе
- Dockerfile (не root, healthcheck, `TZ=Europe/Moscow`), `docker-compose.yml`, `backup.sh`
  (ночной бэкап базы и фото, 14 дней), `nginx-cola.conf`, README с переездом и восстановлением

### Безопасность и инфраструктура
- API-токен заменён; старый удалён из кода, документов и истории git
- Репозиторий опубликован (https://github.com/unklex/cola-tracker): история очищена от токена,
  IP сервера, локальных путей и `.idea/`, авторство — `unklex`
- Сервер: переезд на Docker с проверкой «до/после», HTTPS (nginx + Let's Encrypt),
  файрвол ufw, порт 8000 закрыт снаружи (контейнер слушает только `127.0.0.1`),
  ночные бэкапы с проверенным восстановлением

## [2.2] - 2026-07-29

### Сборка (2026-10-04) — проект впервые собран после правок 2.2
- `ChildCard.kt` (не использовался, считал цвета от `remaining / monthly_limit`) удалён
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
