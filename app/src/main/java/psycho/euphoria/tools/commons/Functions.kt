package psycho.euphoria.tools.commons

import android.app.AlertDialog
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.support.annotation.RequiresApi
import android.util.Log
import java.io.File
import java.io.FileDescriptor
import java.util.*
import java.util.concurrent.*
import android.support.v4.graphics.TypefaceCompatUtil.closeQuietly
import android.text.Editable
import android.widget.EditText
import java.io.ByteArrayOutputStream
import java.io.FileInputStream

fun dialog(context: Context, content: String?, title: String?, positiveListener: (Editable?) -> Unit) {

    val editText = EditText(context)
    editText.setText(content)
    AlertDialog.Builder(context)
            .setView(editText)
            .setTitle(title)
            .setNegativeButton("取消") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("确定") { dialog, _ ->
                dialog.dismiss()
                positiveListener(editText.text)
            }.show()
}

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

fun ensureAvailableSpace(context: Context, fd: FileDescriptor, bytes: Long) {
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

fun isStatusError(status: Int): Boolean {
    return status >= 400 && status < 600
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


fun NotificationManager.postNotification(id: Long, notification: Notification) {
    notify(id.toInt(), notification)
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