package psycho.euphoria.tools

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileFilter
import java.text.Collator
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.ArrayList


private val physicalPaths = arrayListOf(
        "/storage/sdcard1",
        "/storage/extsdcard",
        "/storage/sdcard0/external_sdcard",
        "/mnt/extsdcard", "/mnt/sdcard/external_sd",
        "/mnt/external_sd", "/mnt/media_rw/sdcard1",
        "/removable/microsd",
        "/mnt/emmc", "/storage/external_SD",
        "/storage/ext_sd",
        "/storage/removable/sdcard1",
        "/data/sdext", "/data/sdext2", "/data/sdext3", "/data/sdext4", "/sdcard1",
        "/sdcard2",
        "/storage/usbdisk0",
        "/storage/usbdisk1",
        "/storage/usbdisk2"
)


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
const val OTG_PATH = "otg:/"
fun getInternalStoragePath() = Environment.getExternalStorageDirectory().absolutePath.trimEnd('/')

fun isLollipopPlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
fun isMarshmallowPlus() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
private const val REGEX_FILE_AUDIO = "\\.(?:3gp|8svx|aa|aac|aax|act|aiff|amr|ape|au|awb|dct|dss|dvf|flac|gsm|iklax|ivs|m4a|m4b|m4p|mmf|mp3|mpc|msv|nsf|ogg|oga|mogg|opus|ra|rm|raw|sln|tta|vox|wav|webm|wma|wv)$"
private const val REGEX_FILE_IMAGE = "\\.(?:jpeg|jpg|bmp|png|gif|tiff)$"
private const val TAG = "Extension"
val Context.sdCardPath: String get() = getSDCardPath()

fun Context.dp2px(dp: Float): Float {
    return dp * resources.displayMetrics.density;
}

fun Context.getDrawableCompat(resId: Int): Drawable {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        return resources.getDrawable(resId, theme);
    } else {
        return resources.getDrawable(resId);
    }
}

fun Context.getSDCardPath(): String {
    val directories = getStorageDirectories().filter { it.trimEnd('/') != getInternalStoragePath() }
    var sdCardPath = directories.firstOrNull { !physicalPaths.contains(it.toLowerCase().trimEnd('/')) }
            ?: ""

    if (sdCardPath.trimEnd('/').isEmpty()) {
        val file = File("/storage/sdcard1")
        if (file.exists()) {
            return file.absolutePath
        }
        sdCardPath = directories.firstOrNull() ?: ""
    }
    if (sdCardPath.isEmpty()) {
        val SDpattern = Pattern.compile("^[A-Za-z0-9]{4}-[A-Za-z0-9]{4}$")
        try {
            File("/storage").listFiles()?.forEach {
                if (SDpattern.matcher(it.name).matches()) {
                    sdCardPath = "/storage/${it.name}"
                }
            }
        } catch (e: Exception) {
        }
    }
    val finalPath = sdCardPath.trimEnd('/')


    return finalPath
}

fun Context.getStorageDirectories(): Array<String> {
    val paths = HashSet<String>()
    val rawExternalStorage = System.getenv("EXTERNAL_STORAGE")
    val rawSecondaryStoragesStr = System.getenv("SECONDARY_STORAGE")
    val rawEmulatedStorageTarget = System.getenv("EMULATED_STORAGE_TARGET")
    if (TextUtils.isEmpty(rawEmulatedStorageTarget)) {
        if (isMarshmallowPlus()) {
            getExternalFilesDirs(null).filterNotNull().map { it.absolutePath }
                    .mapTo(paths) { it.substring(0, it.indexOf("Android/data")) }
        } else {
            if (TextUtils.isEmpty(rawExternalStorage)) {
                paths.addAll(physicalPaths)
            } else {
                paths.add(rawExternalStorage)
            }
        }
    } else {
        val path = Environment.getExternalStorageDirectory().absolutePath
        val folders = Pattern.compile("/").split(path)
        val lastFolder = folders[folders.size - 1]
        var isDigit = false
        try {
            Integer.valueOf(lastFolder)
            isDigit = true
        } catch (ignored: NumberFormatException) {
        }
        val rawUserId = if (isDigit) lastFolder else ""
        if (TextUtils.isEmpty(rawUserId)) {
            paths.add(rawEmulatedStorageTarget)
        } else {
            paths.add(rawEmulatedStorageTarget + File.separator + rawUserId)
        }
    }
    if (!TextUtils.isEmpty(rawSecondaryStoragesStr)) {
        val rawSecondaryStorages = rawSecondaryStoragesStr.split(File.pathSeparator.toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
        Collections.addAll(paths, *rawSecondaryStorages)
    }
    return paths.toTypedArray()
}



fun Context.px2dp(px: Float): Float {
    return px / resources.displayMetrics.density;
}


fun Context.px2sp(px: Float): Float {
    return px / resources.displayMetrics.scaledDensity
}

fun Context.sp2px(sp: Float): Float {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics);
}

fun File.isImage(): Boolean {
    return isFile && Regex("\\.(?:jpeg|jpg|bmp|png|gif|tiff)$", RegexOption.IGNORE_CASE).containsMatchIn(name)
}

fun File.listAudio(): List<File> {
    val audioRegex = Regex(REGEX_FILE_AUDIO, RegexOption.IGNORE_CASE)
    val accept = fun(f: File): Boolean {
        return f.isFile && audioRegex.containsMatchIn(f.name)
    }
    return listFiles().filter { accept(it) }
}


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

fun String.e(t: String, messag: String) {
    Log.e(t, "[$this]: $messag")
}



fun Context.getSharedPrefs() = getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
//
//open class BaseConfig(val context: Context) {
//    protected val prefs = context.getSharedPrefs()
//
//    var accessedDirectory: String
//        get() = prefs.getString(PREFS_ACCESSED_DIRECTORY, getInternalStoragePath())
//        set(accessdDirectory) = prefs.edit().putString(PREFS_ACCESSED_DIRECTORY, accessdDirectory).apply()
//
//    companion object {
//        fun newInstance(context: Context) = BaseConfig(context)
//    }
//}

abstract class BaseActivity() : Activity() {

}