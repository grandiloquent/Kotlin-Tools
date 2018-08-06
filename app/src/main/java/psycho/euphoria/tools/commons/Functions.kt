package psycho.euphoria.tools.commons

import android.app.AlertDialog
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.provider.DocumentsContract
import android.support.annotation.RequiresApi
import android.text.Editable
import android.util.Log
import android.view.WindowManager
import android.widget.EditText
import java.io.File
import java.io.FileFilter
import java.util.*
import java.util.concurrent.*


fun append(array: LongArray, currentSize: Int, element: Long): LongArray {
    var array = array
    assert(currentSize <= array.size)
    if (currentSize + 1 > array.size) {
        val newArray = LongArray(growSize(currentSize))
        System.arraycopy(array, 0, newArray, 0, currentSize)
        array = newArray
    }
    array[currentSize] = element
    return array
}

fun binarySearch(array: LongArray, size: Int, value: Long): Int {
    var lo = 0
    var hi = size - 1
    while (lo <= hi) {
        val mid = (lo + hi).ushr(1)
        val midVal = array[mid]
        if (midVal < value) {
            lo = mid + 1
        } else if (midVal > value) {
            hi = mid - 1
        } else {
            return mid  // value found
        }
    }
    return lo.inv()  // value not present
}

fun binarySearch(array: IntArray, size: Int, value: Int): Int {
    var lo = 0
    var hi = size - 1
    while (lo <= hi) {
        val mid = (lo + hi).ushr(1)
        val midVal = array[mid]
        if (midVal < value) {
            lo = mid + 1
        } else if (midVal > value) {
            hi = mid - 1
        } else {
            return mid  // value found
        }
    }
    return lo.inv()  // value not present
}

@RequiresApi(Build.VERSION_CODES.KITKAT)
fun closeQuietly(closeable: AutoCloseable?) {
    if (closeable != null) {
        try {
            closeable.close()
        } catch (rethrown: RuntimeException) {
            throw rethrown
        } catch (ignored: Exception) {
        }
    }
}

fun dialog(context: Context, content: String?, title: String?, isForFileName: Boolean = false, positiveListener: (Editable?) -> Unit) {
    val editText = EditText(context)
    editText.maxLines = 1
    editText.setText(content)
    if (isForFileName) {
        content?.let {
            val pos = it.lastIndexOf('.')
            if (pos > -1) {
                editText.setSelection(0, pos)

            }
        }
    }
    val dialog = AlertDialog.Builder(context)
            .setView(editText)
            .setTitle(title)
            .setNegativeButton("取消") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("确定") { dialog, _ ->
                dialog.dismiss()
                positiveListener(editText.text)
            }.create()

    //  Show the input keyboard for user
    dialog.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    dialog.show()


}

fun serializeFileName(path: String, context: Context, startValue: Int = 1) {
    val dir = File(path)
    if (!dir.isDirectory) return
    val files = dir.listFiles(FileFilter {
        it.isFile
    })
    val chinese = Regex("[\\u4E00-\\u9FA5]+")
    val map = HashMap<String, Int>()
    for (file in files) {
        val matchValue = chinese.find(file.name)
        if (matchValue != null && !map.containsKey(matchValue.value)) {
            map.put(matchValue.value, startValue)
        }
    }
    if (map.isNotEmpty()) {
        for (file in files) {
            val matchValue = chinese.find(file.name)
            if (matchValue != null && map.containsKey(matchValue.value)) {

                val ext = if (file.extension.isBlank()) "mp4" else file.extension
                val targetFile = File(file.parentFile, matchValue.value + map[matchValue.value] + "." + ext)
                if (context.needsStupidWritePermissions(targetFile.absolutePath)) {
                    val document = context.getDocumentFile(file.absolutePath)
                    if (document != null)
                        DocumentsContract.renameDocument(context.applicationContext.contentResolver, document.uri, targetFile.absolutePath.getFilenameFromPath())
                } else {
                    file.renameTo(targetFile)
                }

                map.set(matchValue.value, map[matchValue.value]?.plus(1) ?: 1)
            }
        }
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

fun growSize(currentSize: Int): Int {
    return if (currentSize <= 4) 8 else currentSize * 2
}

fun insert(array: LongArray, currentSize: Int, index: Int, element: Long): LongArray {
    assert(currentSize <= array.size)
    if (currentSize + 1 <= array.size) {
        System.arraycopy(array, index, array, index + 1, currentSize - index)
        array[index] = element
        return array
    }
    val newArray = LongArray(growSize(currentSize))
    System.arraycopy(array, 0, newArray, 0, index)
    newArray[index] = element
    System.arraycopy(array, index, newArray, index + 1, array.size - index)
    return newArray
}

fun isDrmConvertNeeded(mimetype: String?): Boolean {
    return MIMETYPE_DRM_MESSAGE.equals(mimetype)
}

fun isStatusCompleted(status: Int): Boolean {
    return status >= 200 && status < 300 || status >= 400 && status < 600
}

fun isStatusError(status: Int): Boolean {
    return status >= 400 && status < 600
}

fun listFilesRecursive(startDir: File, exclude: String): List<File> {
    val files = ArrayList<File>()
    val dirs = LinkedList<File>();
    dirs.add(startDir);
    while (!dirs.isEmpty()) {
        val dir = dirs.removeFirst();
        if (Objects.equals(dir.getName(), exclude)) continue;
        val children = dir.listFiles();
        if (children == null) continue;
        for (child in children) {
            if (child.isDirectory()) {
                dirs.add(child);
            } else if (child.isFile()) {
                try {
                    files.add(child);
                } catch (ignored: Exception) {
                }
            }
        }
    }
    return files;
}

fun NotificationManager.postNotification(id: Long, notification: Notification) {
    notify(id.toInt(), notification)
}

fun sanitizeMimeType(mimeType: String?): String? {
    if (mimeType != null) {
        var m = mimeType.trim().toLowerCase(Locale.ENGLISH)
        val semicolonIndex = m.indexOf(';')
        if (semicolonIndex != -1)
            m = m.substring(0, semicolonIndex)
        return m
    }
    return null
}

inline fun then(a: () -> Boolean, b: () -> Unit, c: () -> Unit) {
    if (a()) b() else c()
}

inline fun thenOr(funtions: Array<Pair<() -> Boolean, () -> Unit>>, final: () -> Unit) {
    for ((a, b) in funtions) {
        if (a()) {
            b()
            return
        }
    }
    final()
}