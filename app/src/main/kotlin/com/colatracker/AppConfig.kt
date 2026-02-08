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
     * URL сервера
     */
    const val BASE_URL = "http://<SERVER_IP>:8000"

    /**
     * Токен аутентификации
     */
    const val AUTH_TOKEN = "REDACTED_OLD_TOKEN"

    /**
     * Таймаут запросов в миллисекундах
     */
    const val REQUEST_TIMEOUT_MS = 30_000L
}
