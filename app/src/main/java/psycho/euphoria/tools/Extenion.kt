package psycho.euphoria.tools
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.text.TextUtils
import android.util.Log
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileFilter
import java.text.Collator
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.ArrayList
const val PREFS_KEY = "Prefs"
const val PREFS_ACCESSED_DIRECTORY = "accessed_directory"
const val PERMISSION_READ_STORAGE = 1
const val PERMISSION_WRITE_STORAGE = 2
const val PERMISSION_CAMERA = 3
const val PERMISSION_RECORD_AUDIO = 4
const val PERMISSION_READ_CONTACTS = 5
const val PERMISSION_WRITE_CONTACTS = 6
const val PERMISSION_READ_CALENDAR = 7
const val PERMISSION_WRITE_CALENDAR = 8
const val PERMISSION_CALL_PHONE = 9
const val GENERIC_PERM_HANDLER = 100
fun isLollipopPlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
private const val REGEX_FILE_AUDIO = "\\.(?:3gp|8svx|aa|aac|aax|act|aiff|amr|ape|au|awb|dct|dss|dvf|flac|gsm|iklax|ivs|m4a|m4b|m4p|mmf|mp3|mpc|msv|nsf|ogg|oga|mogg|opus|ra|rm|raw|sln|tta|vox|wav|webm|wma|wv)$"
private const val REGEX_FILE_IMAGE = "\\.(?:jpeg|jpg|bmp|png|gif|tiff)$"
private const val TAG = "Extension"
fun File.listImagesRecursively(): List<File> {
    var dir = this
    if (dir.isFile) {
        dir = dir.parentFile
    }
    val imageRegex = Regex(REGEX_FILE_IMAGE, RegexOption.IGNORE_CASE)
    return dir.walkTopDown().filter { imageRegex.containsMatchIn(it.name) }.toList()
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
//    if (rawFiles != null) {
//        val collator = Collator.getInstance(Locale.CHINA)
//        Arrays.sort(rawFiles, object : Comparator<File> {
//            override fun compare(file: File, t1: File): Int {
//                if (containsDirectory) {
//                    if (file.isDirectory && t1.isDirectory || file.isFile && t1.isFile) {
//                        return collator.compare(file.name, t1.name)
//                    }
//                    if (file.isDirectory && t1.isFile) return -1
//                    if (file.isFile && t1.isDirectory) return 1
//                } else {
//                    return collator.compare(file.name, t1.name)
//                }
//                return 0
//            }
//        })
//    }
    for (file in rawFiles!!) {
        files.add(file.getName())
    }
    return files
}
fun String.e(t: String, messag: String) {
    Log.e(t, "[$this]: $messag")
}