package psycho.euphoria.tools.commons

import android.content.Context
import java.io.File

data class FileItem(val path: String,
                    val name: String,
                    val size: Long,
                    val count: Int,
                    val isDirectory: Boolean)

fun File.isImageVideoGif() = absolutePath.isImageFast() || absolutePath.isVideoFast() || absolutePath.isGif() || absolutePath.isRawFast()
fun File.getDirectChildrenCount(countHiddenItems: Boolean) = listFiles()?.filter { if (countHiddenItems) true else !it.isHidden }?.size
        ?: 0

fun File.toFileDirItem(context: Context) = FileDirItem(absolutePath, name, context.getIsPathDirectory(absolutePath), 0, length())



fun File.createDirectory() {
    if (!exists()) mkdirs()
}
fun File.deletes() {
    if (isFile) {
        delete()
    } else if (isDirectory) {
        /*
        public fun File.deleteRecursively(): Boolean = walkBottomUp().fold(true, { res, it -> (it.delete() || !it.exists()) && res })
         */
        deleteRecursively()
    }
}
fun File.getFileCount(countHiddenItems: Boolean): Int {
    return if (isDirectory) {
        getDirectoryFileCount(this, countHiddenItems)
    } else {
        1
    }
}
fun File.getProperSize(countHiddenItems: Boolean): Long {
    return if (isDirectory) {
        getDirectorySize(this, countHiddenItems)
    } else {
        length()
    }
}
fun File.listFileItems(sort: Int = SORT_BY_NAME): ArrayList<FileItem>? {
    if (!isDirectory) return null
    val files = listFiles()
    if (files == null) return null
    when (sort) {
        SORT_BY_DATE_MODIFIED -> files.sortWith(compareBy<File> { it.isFile }.thenByDescending { it.lastModified() })
        SORT_BY_SIZE -> files.sortWith(compareBy<File> { it.isFile }.thenBy { it.length() })
        else -> files.sortWith(compareBy<File> { it.isFile }.thenBy { it.name })
    }
    val ls = ArrayList<FileItem>()
    for (file in files) {
        ls.add(FileItem(
                file.absolutePath,
                file.name,
                if (file.isDirectory) 0L else file.length(),
                if (file.isDirectory) file.listFiles().size else 0,
                file.isDirectory
        ))
    }
    return ls
}
private fun getDirectoryFileCount(dir: File, countHiddenItems: Boolean): Int {
    var count = 0
    if (dir.exists()) {
        val files = dir.listFiles()
        if (files != null) {
            for (i in files.indices) {
                val file = files[i]
                if (file.isDirectory) {
                    count++
                    count += getDirectoryFileCount(file, countHiddenItems)
                } else if (!file.isHidden || countHiddenItems) {
                    count++
                }
            }
        }
    }
    return count
}
private fun getDirectorySize(dir: File, countHiddenItems: Boolean): Long {
    var size = 0L
    if (dir.exists()) {
        val files = dir.listFiles()
        if (files != null) {
            for (i in files.indices) {
                if (files[i].isDirectory) {
                    size += getDirectorySize(files[i], countHiddenItems)
                } else if (!files[i].isHidden && !dir.isHidden || countHiddenItems) {
                    size += files[i].length()
                }
            }
        }
    }
    return size
}