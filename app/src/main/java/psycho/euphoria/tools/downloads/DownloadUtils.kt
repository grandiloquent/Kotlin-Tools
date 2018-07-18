package psycho.euphoria.tools.downloads
import android.os.Environment
import android.util.Log
import java.io.File
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.*


fun getTimeStamp(): Long {
    return System.currentTimeMillis() / 1000
}

fun buildDownloadInfo(url: String): DownloadInfo {
    val fileName = generateFileNameFromURL(url, Environment.getExternalStorageDirectory())
    val file = File(fileName)
    return DownloadInfo(url, fileName, if (file.exists() && file.isFile) file.length() else -1L)
}

fun ClosedRange<Char>.randomString(lenght: Int) =
        (1..lenght)
                .map { (Random().nextInt(endInclusive.toInt() - start.toInt()) + start.toInt()).toChar() }
                .joinToString("")

fun ClosedRange<Int>.random() =
        Random().nextInt((endInclusive + 1) - start) + start

fun Long.formatSize(): String {
    if (this <= 0)
        return "0 B"
    val units = arrayOf("B", "kB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(toDouble()) / Math.log10(1024.0)).toInt()
    return "${DecimalFormat("#,##0.#").format(this / Math.pow(1024.0, digitGroups.toDouble()))} ${units[digitGroups]}"
}

fun generateFileNameFromURL(url: String, directory: File): String {
    if (url.isBlank()) {
        var file = File(directory, ('a'..'z').randomString(6))
        while (file.exists()) {
            file = File(directory, ('a'..'z').randomString(6))
        }
        return file.absolutePath
    } else {
        var fileName = url.substringBefore('?')
        var invalidFileNameChars = arrayOf('\"', '<', '>', '|', 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, ':', '*', '?', '\\', '/')
        fileName = fileName.substringAfterLast('/').filter {
            !invalidFileNameChars.contains(it)
        }
        return File(directory, fileName).absolutePath
    }
}

fun getExecutor(): ExecutorService {
    val maxConcurrent = 3
    val executor = object : ThreadPoolExecutor(
            maxConcurrent, maxConcurrent, 10, TimeUnit.SECONDS,
            LinkedBlockingQueue<Runnable>()) {
        override fun afterExecute(r: Runnable, t: Throwable?) {
            var t = t
            super.afterExecute(r, t)
            if (t == null && r is Future<*>) {
                try {
                    (r as Future<*>).get()
                } catch (ce: CancellationException) {
                    t = ce
                } catch (ee: ExecutionException) {
                    t = ee.cause
                } catch (ie: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }
            if (t != null) {
                Log.w("ExecutorService", "Uncaught exception", t)
            }
        }
    }
    executor.allowCoreThreadTimeOut(true)
    return executor
}