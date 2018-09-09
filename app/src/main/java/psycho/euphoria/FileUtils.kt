package psycho.euphoria

import java.io.File
import java.io.FileFilter
import java.text.Collator
import java.util.*
import kotlin.Comparator

fun File.listAudioFiles(): Array<out File>? {
    if (this.isDirectory) {
        val ext = arrayOf(".mp3", ".wav", ".wma", ".ogg", ".m4a", ".opus", ".flac", ".aac")
        return this.listFiles(FileFilter { it -> it.isFile && ext.any { it.endsWith(it, true) } })?.let {
            val c = Collator.getInstance(Locale.CHINA)
            it.sortWith(Comparator<File> { a, b -> c.compare(a.name, b.name) })
            it
        } ?: run { null }
    }
    return null
}