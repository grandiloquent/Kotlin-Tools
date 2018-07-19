package psycho.euphoria.tools.downloads
import psycho.euphoria.tools.commons.randomString
import java.io.File


fun getTimeStamp(): Long {
    return System.currentTimeMillis() / 1000
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

