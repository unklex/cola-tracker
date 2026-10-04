package com.colatracker

/**
 * Конфигурация приложения
 */
object AppConfig {
    /**
     * Режим работы:
     * true = использовать фейковые данные (для тестирования UI)
     * false = работать с реальным API сервером
     */
    const val USE_MOCK_DATA = false

    /**
     * URL сервера (из local.properties → BuildConfig).
     * Хвостовой слэш убираем: URL фотографий склеивается как "$BASE_URL/$photoUrl",
     * и двойной слэш ломает путь.
     */
    val BASE_URL: String = BuildConfig.API_BASE_URL.trimEnd('/')

    /**
     * Токен аутентификации (из local.properties → BuildConfig)
     */
    val AUTH_TOKEN: String = BuildConfig.API_AUTH_TOKEN

    /**
     * Полный таймаут запроса (включая загрузку фото).
     */
    const val REQUEST_TIMEOUT_MS = 30_000L

    /**
     * Таймаут установки соединения. Держим коротким: 30 секунд ожидания
     * подключения — это вечность для мобильного интерфейса.
     */
    const val CONNECT_TIMEOUT_MS = 10_000L

    /**
     * Таймаут молчания сокета между пакетами.
     */
    const val SOCKET_TIMEOUT_MS = 20_000L
}
