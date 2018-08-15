package psycho.euphoria.tools.commons
import android.content.Context
import android.text.TextUtils
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileNotFoundException
import java.util.*
fun File.getDirectChildrenCount(countHiddenItems: Boolean) = listFiles()?.filter { if (countHiddenItems) true else !it.isHidden }?.size
        ?: 0
fun File.buildUniqueFile(): File {
    val parent = parentFile
    val ext = extension
    val name = nameWithoutExtension
    return buildUniqueFileWithExtension(parent, name, ext)
}
private fun buildUniqueFileWithExtension(parent: File, name: String, ext: String): File {
    var file = buildFile(parent, name, ext)
    // If conflicting file, try adding counter suffix
    var n = 0
    while (file.exists()) {
        if (n++ >= 32) {
            throw FileNotFoundException("Failed to create unique file")
        }
        file = buildFile(parent, "$name ($n)", ext)
    }
    return file
}
fun splitFileName(mimeType: String, displayName: String): Array<String> {
    var name: String
    var ext: String?
    if (android.provider.DocumentsContract.Document.MIME_TYPE_DIR.equals(mimeType)) {
        name = displayName
        ext = null
    } else {
        var mimeTypeFromExt: String?
        // Extract requested extension from display name
        val lastDot = displayName.lastIndexOf('.')
        if (lastDot >= 0) {
            name = displayName.substring(0, lastDot)
            ext = displayName.substring(lastDot + 1)
            mimeTypeFromExt = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    ext.toLowerCase())
        } else {
            name = displayName
            ext = null
            mimeTypeFromExt = null
        }
        if (mimeTypeFromExt == null) {
            mimeTypeFromExt = "application/octet-stream"
        }
        val extFromMimeType = MimeTypeMap.getSingleton().getExtensionFromMimeType(
                mimeType)
        if (Objects.equals(mimeType, mimeTypeFromExt) || Objects.equals(ext, extFromMimeType)) {
            // Extension maps back to requested MIME type; allow it
        } else {
            // No match; insist that create file matches requested MIME
            name = displayName
            ext = extFromMimeType
        }
    }
    if (ext == null) {
        ext = ""
    }
    return arrayOf(name, ext)
}
fun buildUniqueFile(parent: File, mimeType: String, displayName: String): File {
    val parts = splitFileName(mimeType, displayName)
    return buildUniqueFileWithExtension(parent, parts[0], parts[1])
}
fun roundStorageSize(size: Long): Long {
    var `val`: Long = 1
    var pow: Long = 1
    while (`val` * pow < size) {
        `val` = `val` shl 1
        if (`val` > 512) {
            `val` = 1
            pow *= 1000
        }
    }
    return `val` * pow
}
private fun buildFile(parent: File, name: String, ext: String): File {
    return if (TextUtils.isEmpty(ext)) {
        File(parent, name)
    } else {
        File(parent, "$name.$ext")
    }
}
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
                    // If it is a directory, recursively accumulate
                    size += getDirectorySize(files[i], countHiddenItems)
                } else if (!files[i].isHidden && !dir.isHidden || countHiddenItems) {
                    size += files[i].length()
                }
            }
        }
    }
    return size
}