package psycho.euphoria.common.extension

import android.net.Uri
import java.io.File

fun File.isVideo(): Boolean {
    return arrayOf(".mp4", ".flv").any { name.endsWith(it, true) }
}

fun File.listVideoFiles(): List<File>? {
    return listFiles()?.filter { it.isFile && it.isVideo() }
}

fun File.toUri(): Uri {
    return Uri.fromFile(this)
}