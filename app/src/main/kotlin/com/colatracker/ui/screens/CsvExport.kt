package com.colatracker.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.colatracker.data.models.DrinkHistoryItem
import com.colatracker.data.models.csvFileName
import com.colatracker.data.models.historyToCsv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

/**
 * Записывает историю в CSV во временную папку кэша и возвращает content-URI для шаринга.
 * FileProvider настроен на весь cache-path (res/xml/file_paths.xml), как и для камеры.
 * Прежние экспорты удаляются — в кэше не копится мусор.
 */
suspend fun writeHistoryCsv(
    context: Context,
    childName: String,
    history: List<DrinkHistoryItem>
): Uri = withContext(Dispatchers.IO) {
    val dir = File(context.cacheDir, "exports").apply { mkdirs() }
    dir.listFiles()?.forEach { it.delete() }

    val file = File(dir, csvFileName(childName, LocalDate.now().toString()))
    file.writeText(historyToCsv(childName, history), Charsets.UTF_8)

    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

/** Открывает системное окно «Поделиться» (почта, мессенджеры, «Сохранить на диск»…). */
fun shareCsv(context: Context, uri: Uri, childName: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "История Cola Tracker: $childName")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, "Экспорт истории"))
}
