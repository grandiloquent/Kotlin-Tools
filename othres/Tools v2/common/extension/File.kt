package psycho.euphoria.common.extension

import android.net.Uri
import android.text.TextUtils
import java.io.File
import java.io.FileNotFoundException


fun File.isVideo(): Boolean {
    return arrayOf(".mp4", ".flv").any { name.endsWith(it, true) }
}

fun File.listVideoFiles(): List<File>? {
    return listFiles()?.filter { it.isFile && it.isVideo() }
}

fun File.toUri(): Uri {
    return Uri.fromFile(this)
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

    return buildUniqueFileWithExtension( name, ext)
}


fun File.buildUniqueFileWithExtension(name: String, ext: String?): File {
    var file = buildFile(name, ext)

    // If conflicting file, try adding counter suffix
    var n = 0
    while (file.exists()) {
        if (n++ >= 32) {
            throw FileNotFoundException("Failed to create unique file")
        }
        file = buildFile("$name ($n)", ext)
    }

    return file
}



fun File.buildFile(name: String, ext: String?): File {
    return if (TextUtils.isEmpty(ext)) {
        File(this, name)
    } else {
        File(this, "$name.$ext")
    }
}

