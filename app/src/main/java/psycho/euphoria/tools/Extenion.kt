package psycho.euphoria.tools

import android.content.Context
import android.content.Intent
import android.graphics.PointF
import android.net.Uri
import android.os.Build
import android.support.v4.content.MimeTypeFilter
import android.util.FloatMath
import android.util.Log
import kotlin.math.atan2
import kotlin.math.sqrt
import android.view.MotionEvent
import android.view.View
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileFilter
import java.text.Collator
import java.util.*
import java.util.regex.Pattern


private const val TAG = "Extension"
private const val SIXTY_FPS_INTERVAL = 1000 / 60L
private const val REGEX_FILE_IMAGE="\\.(?:jpeg|jpg|bmp|png|gif|tiff)$"
private const val REGEX_FILE_AUDIO="\\.(?:3gp|8svx|aa|aac|aax|act|aiff|amr|ape|au|awb|dct|dss|dvf|flac|gsm|iklax|ivs|m4a|m4b|m4p|mmf|mp3|mpc|msv|nsf|ogg|oga|mogg|opus|ra|rm|raw|sln|tta|vox|wav|webm|wma|wv)$"

fun File.isImage(): Boolean {
    return isFile && Regex("\\.(?:jpeg|jpg|bmp|png|gif|tiff)$", RegexOption.IGNORE_CASE).containsMatchIn(name)
}

fun File.listFilesOrderly(): List<File> {
    return listFiles().asList().let {
        it.sortedWith(compareBy<File> { it.isFile }.thenBy { it.name.toLowerCase() })
    }
}

fun File.listImagesRecursively(): List<File> {
    var dir = this
    if (dir.isFile) {
        dir = dir.parentFile
    }
    val imageRegex = Regex(REGEX_FILE_IMAGE, RegexOption.IGNORE_CASE)
    return dir.walkTopDown().filter { imageRegex.containsMatchIn(it.name) }.toList()


}

fun File.listAudio(): List<File> {
    val audioRegex = Regex(REGEX_FILE_AUDIO, RegexOption.IGNORE_CASE)

    val accept = fun(f: File): Boolean {
        return f.isFile && audioRegex.containsMatchIn(f.name)
    }
    return listFiles().filter { accept(it) }
}

fun File.share(context: Context) {

    val intent = Intent(Intent.ACTION_SEND)
    val uri = Uri.fromFile(this)
    val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
    if (extension != null) {
        intent.setType(MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension))
    }
    intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(this))
    context.startActivity(Intent.createChooser(intent, "分享"))

}

fun getFileName(filePath: String?): String? {
    if (filePath != null) {
        var where = filePath.lastIndexOf('/')
        if (where != -1) {
            val fileName = filePath.substring(where + 1)
            where = fileName.lastIndexOf('.')
            return if (where != -1) {
                fileName.substring(0, where)
            } else fileName
        } else {
            where = filePath.lastIndexOf('\\')
            if (where != -1) {
                return filePath.substring(where + 1)
            }
        }
        return filePath
    } else return null
}

fun getPointerIndex(action: Int): Int {
    return action and MotionEvent.ACTION_POINTER_INDEX_MASK shr MotionEvent.ACTION_POINTER_INDEX_SHIFT
}

fun listAudioFiles(directoryFile: File, containsDirectory: Boolean): List<String> {
    val files = ArrayList<String>()
    val pattern = Pattern.compile("\\.(?:mp3|ogg|wav|flac)$", Pattern.CASE_INSENSITIVE)

    val rawFiles = directoryFile.listFiles(object : FileFilter {
        override fun accept(file: File): Boolean {
            if (containsDirectory) {
                if (file.isDirectory || file.isFile && pattern.matcher(file.name).find()) {
                    return true
                }
            } else {
                if (file.isFile && pattern.matcher(file.name).find()) {
                    return true
                }
            }

            return false
        }
    })
    if (rawFiles != null) {
        val collator = Collator.getInstance(Locale.CHINA)

        Arrays.sort(rawFiles, object : Comparator<File> {
            override fun compare(file: File, t1: File): Int {
                if (containsDirectory) {
                    if (file.isDirectory && t1.isDirectory || file.isFile && t1.isFile) {
                        return collator.compare(file.name, t1.name)
                    }
                    if (file.isDirectory && t1.isFile) return -1
                    if (file.isFile && t1.isDirectory) return 1
                } else {
                    return collator.compare(file.name, t1.name)
                }
                return 0
            }
        })

    }

    for (file in rawFiles!!) {
        files.add(file.getName())
    }
    return files
}

fun View.postOnAnimationCompat(runnable: Runnable) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
        postOnAnimation(runnable)
    else postDelayed(runnable, SIXTY_FPS_INTERVAL)
}