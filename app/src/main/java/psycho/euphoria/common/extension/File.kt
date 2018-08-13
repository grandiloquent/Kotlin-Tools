package psycho.euphoria.common.extension

import android.net.Uri
import android.text.TextUtils
import psycho.euphoria.common.SORT_BY_DATE_MODIFIED
import psycho.euphoria.common.SORT_BY_NAME
import psycho.euphoria.common.SORT_BY_SIZE
import java.io.File
import java.io.FileNotFoundException
import java.util.ArrayList


fun File.isVideo(): Boolean {
    return arrayOf(".mp4", ".flv").any { name.endsWith(it, true) }
}

fun File.listVideoFiles(): List<File>? {
    return listFiles()?.filter { it.isFile && it.isVideo() }
}

fun File.changeExtension(ext: String): File {
    var e = ""
    if (ext[0] != '.') {
        e = "."
    }
    return File("${absolutePath.substringBeforeLast('.')}${e}${ext}")
}

fun File.toUri(): Uri {
    return Uri.fromFile(this)
}

fun File.buildUniqueFile(): File {
    val parent = parentFile
    val ext = extension
    val name = nameWithoutExtension
    return parent.buildUniqueFileWithExtension(name, ext)
}

fun File.buildUniqueFile(displayName: String): File {
    val name: String
    val ext: String?

    // Extract requested extension from display name
    val lastDot = displayName.lastIndexOf('.')
    if (lastDot >= 0) {
        name = displayName.substring(0, lastDot)
        ext = displayName.substring(lastDot + 1)
    } else {
        name = displayName
        ext = null
    }

    return buildUniqueFileWithExtension(name, ext)
}

fun File.isImageVideoGif(): Boolean {
    return absolutePath.isImageFast() || absolutePath.isVideoFast() || absolutePath.isGif() || absolutePath.isRawFast()
}

fun File.listFileItems(sort: Int = SORT_BY_NAME): ArrayList<FileItem>? {
    if (!isDirectory) return null
    val files = listFiles()
    if (files == null) return null
    when (sort) {

        SORT_BY_DATE_MODIFIED -> files.sortWith(compareBy<File> { it.isFile }.thenByDescending { it.lastModified() })
        // Sort files in descending order, so the larger the file size, the more front
        SORT_BY_SIZE -> files.sortWith(compareBy<File> { it.isFile }.thenByDescending { it.length() })
        else -> files.sortWith(compareBy<File> { it.isFile }.thenBy { it.name })
    }
    val ls = ArrayList<FileItem>()
    for (file in files) {
        ls.add(FileItem(
                file.absolutePath,
                file.name,
                if (file.isDirectory) 0L else file.length(),
                if (file.isDirectory) file.listFiles()?.size ?: 0 else 0,
                file.isDirectory
        ))
    }
    return ls
}

fun File.buildUniqueFileWithExtension(name: String, ext: String?): File {
    var file = buildFile(this, name, ext)

    // If conflicting file, try adding counter suffix
    var n = 0
    while (file.exists()) {
        if (n++ >= 32) {
            throw FileNotFoundException("Failed to create unique file")
        }
        file = buildFile(this, "$name ($n)", ext)
    }

    return file
}


private fun buildFile(parent: File, name: String, ext: String?): File {
    return if (TextUtils.isEmpty(ext)) {
        File(parent, name)
    } else {
        File(parent, "$name.$ext")
    }
}

data class FileItem(val path: String,
                    val name: String,
                    val size: Long,
                    val count: Int,
                    val isDirectory: Boolean)
