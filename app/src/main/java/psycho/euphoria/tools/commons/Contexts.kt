package psycho.euphoria.tools.commons

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.database.Cursor
import android.graphics.Point
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.content.FileProvider
import android.text.TextUtils
import android.util.DisplayMetrics
import android.view.WindowManager
import android.widget.Toast
import com.simplemobiletools.commons.extensions.getIntValue
import psycho.euphoria.tools.*
import java.io.File
import java.util.*
import java.util.regex.Pattern

// avoid these being set as SD card paths
private val physicalPaths = arrayListOf(
        "/storage/sdcard1", // Motorola Xoom
        "/storage/extsdcard", // Samsung SGS3
        "/storage/sdcard0/external_sdcard", // User request
        "/mnt/extsdcard", "/mnt/sdcard/external_sd", // Samsung galaxy family
        "/mnt/external_sd", "/mnt/media_rw/sdcard1", // 4.4.2 on CyanogenMod S3
        "/removable/microsd", // Asus transformer prime
        "/mnt/emmc", "/storage/external_SD", // LG
        "/storage/ext_sd", // HTC One Max
        "/storage/removable/sdcard1", // Sony Xperia Z1
        "/data/sdext", "/data/sdext2", "/data/sdext3", "/data/sdext4", "/sdcard1", // Sony Xperia Z
        "/sdcard2", // HTC One M8s
        "/storage/usbdisk0",
        "/storage/usbdisk1",
        "/storage/usbdisk2"
)

fun Context.getInternalStoragePath() = Environment.getExternalStorageDirectory().absolutePath.trimEnd('/')
fun Context.getSharedPrefs() = getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
val Context.audioManager get() = getSystemService(Context.AUDIO_SERVICE) as AudioManager
val Context.config: Config get() = Config.newInstance(applicationContext)
val Context.navigationBarBottom: Boolean get() = usableScreenSize.y < realScreenSize.y
val Context.navigationBarHeight: Int get() = if (navigationBarBottom) navigationBarSize.y else 0
val Context.navigationBarRight: Boolean get() = usableScreenSize.x < realScreenSize.x
val Context.navigationBarWidth: Int get() = if (navigationBarRight) navigationBarSize.x else 0
val Context.portrait get() = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
val Context.version: Int get() = Build.VERSION.SDK_INT
val Context.windowManager: WindowManager get() = getSystemService(Context.WINDOW_SERVICE) as WindowManager


internal val Context.navigationBarSize: Point
    get() = when {
        navigationBarRight -> Point(realScreenSize.x - usableScreenSize.x, usableScreenSize.y)
        navigationBarBottom -> Point(usableScreenSize.x, realScreenSize.y - usableScreenSize.y)
        else -> Point()
    }


val Context.alarmManager: AlarmManager
    get() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            return getSystemService(AlarmManager::class.java)
        else
            return getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }
val Context.clipboardManager: ClipboardManager
    get() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            return getSystemService(ClipboardManager::class.java)
        else
            return getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

val Context.notificationManager: NotificationManager
    get() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            return getSystemService(NotificationManager::class.java)
        else
            return getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
val Context.realScreenSize: Point
    get() {
        val size = Point()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            windowManager.defaultDisplay.getRealSize(size)
        return size
    }
val Context.usableScreenSize: Point
    get() {
        val size = Point()
        windowManager.defaultDisplay.getSize(size)
        return size
    }


fun Context.humanizePath(path: String): String {
    return ""
}

fun Context.toast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun Context.getHumanizedFilename(path: String): String {
    val humanized = humanizePath(path)
    return humanized.substring(humanized.lastIndexOf("/") + 1)
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

fun Context.getPermissionString(id: Int) = when (id) {
    PERMISSION_READ_STORAGE -> Manifest.permission.READ_EXTERNAL_STORAGE
    PERMISSION_WRITE_STORAGE -> Manifest.permission.WRITE_EXTERNAL_STORAGE
    PERMISSION_CAMERA -> Manifest.permission.CAMERA
    PERMISSION_RECORD_AUDIO -> Manifest.permission.RECORD_AUDIO
    PERMISSION_READ_CONTACTS -> Manifest.permission.READ_CONTACTS
    PERMISSION_WRITE_CONTACTS -> Manifest.permission.WRITE_CONTACTS
    PERMISSION_READ_CALENDAR -> Manifest.permission.READ_CALENDAR
    PERMISSION_WRITE_CALENDAR -> Manifest.permission.WRITE_CALENDAR
    PERMISSION_CALL_PHONE -> Manifest.permission.CALL_PHONE
    else -> ""
}

fun Context.ensurePublicUri(path: String, applicationId: String): Uri? {
    return if (path.startsWith(OTG_PATH)) {
        null
        // getDocumentFile(path)?.uri
    } else {
        val uri = Uri.parse(path)
        if (uri.scheme == "content") {
            uri
        } else {
            val newPath = if (uri.toString().startsWith("/")) uri.toString() else uri.path
            val file = File(newPath)
            getFilePublicUri(file, applicationId)
        }
    }
}

fun Context.getFilePublicUri(file: File, applicationId: String): Uri {
    // for images/videos/gifs try getting a media content uri first, like content://media/external/images/media/438
    // if media content uri is null, get our custom uri like content://com.simplemobiletools.gallery.provider/external_files/emulated/0/DCIM/IMG_20171104_233915.jpg
    return if (file.isImageVideoGif()) {
        getMediaContentUri(file.absolutePath)
                ?: FileProvider.getUriForFile(this, "$applicationId.provider", file)
    } else {
        FileProvider.getUriForFile(this, "$applicationId.provider", file)
    }
}
fun Context.toast(id: Int, length: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, id, length).show()
}

fun Context.getMediaContentUri(path: String): Uri? {
    val uri = when {
        path.isImageFast() -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        path.isVideoFast() -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        else -> MediaStore.Files.getContentUri("external")
    }

    return getMediaContent(path, uri)
}

fun Context.getMediaContent(path: String, uri: Uri): Uri? {
    val projection = arrayOf(MediaStore.Images.Media._ID)
    val selection = MediaStore.Images.Media.DATA + "= ?"
    val selectionArgs = arrayOf(path)
    var cursor: Cursor? = null
    try {
        cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
        if (cursor?.moveToFirst() == true) {
            val id = cursor.getIntValue(MediaStore.Images.Media._ID).toString()
            return Uri.withAppendedPath(uri, id)
        }
    } catch (e: Exception) {
    } finally {
        cursor?.close()
    }
    return null
}

fun Context.ensurePublicUri(uri: Uri, applicationId: String): Uri {
    return if (uri.scheme == "content") {
        uri
    } else {
        val file = File(uri.path)
        getFilePublicUri(file, applicationId)
    }
}

fun Context.getUriMimeType(path: String, newUri: Uri): String {
    var mimeType = path.getMimeType()
    if (mimeType.isEmpty()) {
        mimeType = getMimeTypeFromUri(newUri)
    }
    return mimeType
}

fun Context.getMimeTypeFromUri(uri: Uri): String {
    var mimetype = uri.path.getMimeType()
    if (mimetype.isEmpty()) {
        try {
            mimetype = contentResolver.getType(uri)
        } catch (e: IllegalStateException) {
        }
    }
    return mimetype
}

// http://stackoverflow.com/a/40582634/1967672
fun Context.getSDCardPath(): String {
    val directories = getStorageDirectories().filter { it.trimEnd('/') != getInternalStoragePath() }
    var sdCardPath = directories.firstOrNull { !physicalPaths.contains(it.toLowerCase().trimEnd('/')) }
            ?: ""
    // on some devices no method retrieved any SD card path, so test if its not sdcard1 by any chance. It happened on an Android 5.1
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