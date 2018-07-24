package psycho.euphoria.tools.commons

import java.io.File

data class FileItem(val path: String,
                    val name: String,
                    val size: Long,
                    val count: Int,
                    val isDirectory: Boolean)

fun File.createDirectory() {
    if (!exists()) mkdirs()
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