package psycho.euphoria.tools.commons

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.*
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.database.Cursor
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.support.v4.content.FileProvider
import android.support.v4.provider.DocumentFile
import android.text.TextUtils
import android.util.TypedValue
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
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

fun Context.getAdjustedPrimaryColor() = if (isBlackAndWhiteTheme()) Color.WHITE else baseConfig.primaryColor
fun Context.getDoesFilePathExist(path: String) = if (path.startsWith(OTG_PATH)) getOTGFastDocumentFile(path)?.exists()
        ?: false else File(path).exists()

fun Context.getInternalStoragePath() = Environment.getExternalStorageDirectory().absolutePath.trimEnd('/')
fun Context.getSharedPrefs() = getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
fun Context.isBlackAndWhiteTheme() = baseConfig.textColor == Color.WHITE && baseConfig.primaryColor == Color.BLACK && baseConfig.backgroundColor == Color.BLACK
fun Context.isPathOnSD(path: String) = sdCardPath.isNotEmpty() && path.startsWith(sdCardPath)
fun Context.needsStupidWritePermissions(path: String) = (isPathOnSD(path) || path.startsWith(OTG_PATH)) && isLollipopPlus()
val Context.audioManager get() = getSystemService(Context.AUDIO_SERVICE) as AudioManager
val Context.baseConfig: BaseConfig get() = BaseConfig.newInstance(this)
val Context.navigationBarBottom: Boolean get() = usableScreenSize.y < realScreenSize.y
val Context.navigationBarHeight: Int get() = if (navigationBarBottom) navigationBarSize.y else 0
val Context.navigationBarRight: Boolean get() = usableScreenSize.x < realScreenSize.x
val Context.navigationBarWidth: Int get() = if (navigationBarRight) navigationBarSize.x else 0
val Context.portrait get() = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
val Context.sdCardPath: String get() = baseConfig.sdCardPath
val Context.version: Int get() = Build.VERSION.SDK_INT
val Context.windowManager: WindowManager get() = getSystemService(Context.WINDOW_SERVICE) as WindowManager
fun Context.getSomeDocumentFile(path: String) = getFastDocumentFile(path) ?: getDocumentFile(path)
private fun isDownloadsDocument(uri: Uri) = uri.authority == "com.android.providers.downloads.documents"
private fun isMediaDocument(uri: Uri) = uri.authority == "com.android.providers.media.documents"
private fun isExternalStorageDocument(uri: Uri) = uri.authority == "com.android.externalstorage.documents"

/*
Properties
 */
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
internal val Context.navigationBarSize: Point
    get() = when {
        navigationBarRight -> Point(realScreenSize.x - usableScreenSize.x, usableScreenSize.y)
        navigationBarBottom -> Point(usableScreenSize.x, realScreenSize.y - usableScreenSize.y)
        else -> Point()
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


/*
Functions
 */


fun Context.deleteFromMediaStore(path: String): Boolean {
    if (getDoesFilePathExist(path) || getIsPathDirectory(path)) {
        return false
    }
    return try {
        val where = "${MediaStore.MediaColumns.DATA} = ?"
        val args = arrayOf(path)
        contentResolver.delete(getFileUri(path), where, args) == 1
    } catch (e: Exception) {
        false
    }
}
fun Context.dp2px(dp: Float): Float {
    return dp * resources.displayMetrics.density;
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
fun Context.ensurePublicUri(uri: Uri, applicationId: String): Uri {
    return if (uri.scheme == "content") {
        uri
    } else {
        val file = File(uri.path)
        getFilePublicUri(file, applicationId)
    }
}
fun Context.getDataColumn(uri: Uri, selection: String? = null, selectionArgs: Array<String>? = null): String? {
    var cursor: Cursor? = null
    try {
        val projection = arrayOf(MediaStore.Files.FileColumns.DATA)
        cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
        if (cursor?.moveToFirst() == true) {
            return cursor.getStringValue(MediaStore.Files.FileColumns.DATA)
        }
    } catch (e: Exception) {
    } finally {
        cursor?.close()
    }
    return null
}
fun Context.getDocumentFile(path: String): DocumentFile? {
    if (!isLollipopPlus()) {
        return null
    }
    val isOTG = path.startsWith(OTG_PATH)
    var relativePath = path.substring(if (isOTG) OTG_PATH.length else sdCardPath.length)
    if (relativePath.startsWith(File.separator)) {
        relativePath = relativePath.substring(1)
    }
    var document = DocumentFile.fromTreeUri(applicationContext, Uri.parse(if (isOTG) baseConfig.OTGTreeUri else baseConfig.treeUri))
    val parts = relativePath.split("/").filter { it.isNotEmpty() }
    for (part in parts) {
        document = document?.findFile(part)
    }
    return document
}
fun Context.getDrawableCompat(resId: Int): Drawable {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        return resources.getDrawable(resId, theme);
    } else {
        return resources.getDrawable(resId);
    }
}
fun Context.getFastDocumentFile(path: String): DocumentFile? {
    if (!isLollipopPlus()) {
        return null
    }
    if (path.startsWith(OTG_PATH)) {
        return getOTGFastDocumentFile(path)
    }
    val sdCardPath = baseConfig.sdCardPath
    if (sdCardPath.isEmpty()) {
        return null
    }
    val relativePath = Uri.encode(path.substring(sdCardPath.length).trim('/'))
    val externalPathPart = sdCardPath.split("/").lastOrNull(String::isNotEmpty)?.trim('/')
            ?: return null
    val fullUri = "${baseConfig.treeUri}/document/$externalPathPart%3A$relativePath"
    return DocumentFile.fromSingleUri(this, Uri.parse(fullUri))
}
fun Context.getFilenameFromContentUri(uri: Uri): String? {
    var cursor: Cursor? = null
    try {
        cursor = contentResolver.query(uri, null, null, null, null)
        if (cursor?.moveToFirst() == true) {
            return cursor.getStringValue(OpenableColumns.DISPLAY_NAME)
        }
    } catch (e: Exception) {
    } finally {
        cursor?.close()
    }
    return ""
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
fun Context.getFileUri(path: String) = when {
    path.isImageSlow() -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    path.isVideoSlow() -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    else -> MediaStore.Files.getContentUri("external")
}
fun Context.getHumanizedFilename(path: String): String {
    val humanized = humanizePath(path)
    return humanized.substring(humanized.lastIndexOf("/") + 1)
}
fun Context.getIsPathDirectory(path: String): Boolean {
    return if (path.startsWith(OTG_PATH)) {
        getOTGFastDocumentFile(path)?.isDirectory ?: false
    } else {
        File(path).isDirectory
    }
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
fun Context.getMediaContentUri(path: String): Uri? {
    val uri = when {
        path.isImageFast() -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        path.isVideoFast() -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        else -> MediaStore.Files.getContentUri("external")
    }
    return getMediaContent(path, uri)
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
fun Context.getOTGFastDocumentFile(path: String): DocumentFile? {
    if (baseConfig.OTGTreeUri.isEmpty()) {
        return null
    }
    if (baseConfig.OTGPartition.isEmpty()) {
        baseConfig.OTGPartition = baseConfig.OTGTreeUri.removeSuffix("%3A").substringAfterLast('/')
    }
    val relativePath = Uri.encode(path.substring(OTG_PATH.length).trim('/'))
    val fullUri = "${baseConfig.OTGTreeUri}/document/${baseConfig.OTGPartition}%3A$relativePath"
    return DocumentFile.fromSingleUri(this, Uri.parse(fullUri))
}
fun Context.getRealPathFromURI(uri: Uri): String? {
    if (uri.scheme == "file") {
        return uri.path
    }
    if (isKitkatPlus()) {
        if (isDownloadsDocument(uri)) {
            val id = DocumentsContract.getDocumentId(uri)
            if (id.areDigitsOnly()) {
                val newUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), id.toLong())
                val path = getDataColumn(newUri)
                if (path != null) {
                    return path
                }
            }
        } else if (isExternalStorageDocument(uri)) {
            val documentId = DocumentsContract.getDocumentId(uri)
            val parts = documentId.split(":")
            if (parts[0].equals("primary", true)) {
                return "${Environment.getExternalStorageDirectory().absolutePath}/${parts[1]}"
            }
        } else if (isMediaDocument(uri)) {
            val documentId = DocumentsContract.getDocumentId(uri)
            val split = documentId.split(":").dropLastWhile { it.isEmpty() }.toTypedArray()
            val type = split[0]
            val contentUri = when (type) {
                "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                else -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            val selection = "_id=?"
            val selectionArgs = arrayOf(split[1])
            val path = getDataColumn(contentUri, selection, selectionArgs)
            if (path != null) {
                return path
            }
        }
    }
    return getDataColumn(uri)
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
fun Context.getUriMimeType(path: String, newUri: Uri): String {
    var mimeType = path.getMimeType()
    if (mimeType.isEmpty()) {
        mimeType = getMimeTypeFromUri(newUri)
    }
    return mimeType
}
fun Context.hasProperStoredTreeUri(): Boolean {
    val hasProperUri = contentResolver.persistedUriPermissions.any { it.uri.toString() == baseConfig.treeUri }
    if (!hasProperUri) {
        baseConfig.treeUri = ""
    }
    return hasProperUri
}
fun Context.humanizePath(path: String): String {
    return ""
}
fun Context.px2dp(px: Float): Float {
    return px / resources.displayMetrics.density;
}
fun Context.px2sp(px: Float): Float {
    return px / resources.displayMetrics.scaledDensity
}
fun Context.rescanDeletedPath(path: String, callback: (() -> Unit)? = null) {
    if (path.startsWith(filesDir.toString())) {
        callback?.invoke()
        return
    }
    if (deleteFromMediaStore(path)) {
        callback?.invoke()
    } else {
        if (getIsPathDirectory(path)) {
            callback?.invoke()
            return
        }
        MediaScannerConnection.scanFile(applicationContext, arrayOf(path), null) { s, uri ->
            try {
                applicationContext.contentResolver.delete(uri, null, null)
            } catch (e: Exception) {
            }
            callback?.invoke()
        }
    }
}
fun Context.rescanPaths(paths: ArrayList<String>, callback: (() -> Unit)? = null) {
    var cnt = paths.size
    MediaScannerConnection.scanFile(applicationContext, paths.toTypedArray(), null, { s, uri ->
        if (--cnt == 0) {
            callback?.invoke()
        }
    })
}
fun Context.scanFileRecursively(file: File, callback: (() -> Unit)? = null) {
    scanFilesRecursively(arrayListOf(file), callback)
}
fun Context.scanFilesRecursively(files: ArrayList<File>, callback: (() -> Unit)? = null) {
    val allPaths = ArrayList<String>()
    for (file in files) {
        allPaths.addAll(getPaths(file))
    }
    rescanPaths(allPaths, callback)
}
fun Context.scanPathRecursively(path: String, callback: (() -> Unit)? = null) {
    scanPathsRecursively(arrayListOf(path), callback)
}
fun Context.scanPathsRecursively(paths: ArrayList<String>, callback: (() -> Unit)? = null) {
    val allPaths = ArrayList<String>()
    for (path in paths) {
        allPaths.addAll(getPaths(File(path)))
    }
    rescanPaths(allPaths, callback)
}
fun Context.sp2px(sp: Float): Float {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics);
}
fun Context.toast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}
fun Context.toast(id: Int, length: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, id, length).show()
}
fun Context.tryFastDocumentDelete(path: String, allowDeleteFolder: Boolean): Boolean {
    val document = getFastDocumentFile(path)
    return if (document?.isFile == true || allowDeleteFolder) {
        try {
            DocumentsContract.deleteDocument(contentResolver, document?.uri)
        } catch (e: Exception) {
            false
        }
    } else {
        false
    }
}
fun Context.trySAFFileDelete(fileDirItem: FileDirItem, allowDeleteFolder: Boolean = false, callback: ((wasSuccess: Boolean) -> Unit)? = null) {
    var fileDeleted = tryFastDocumentDelete(fileDirItem.path, allowDeleteFolder)
    if (!fileDeleted) {
        val document = getDocumentFile(fileDirItem.path)
        if (document != null && (fileDirItem.isDirectory == document.isDirectory)) {
            try {
                fileDeleted = (document.isFile == true || allowDeleteFolder) && DocumentsContract.deleteDocument(applicationContext.contentResolver, document.uri)
            } catch (ignored: Exception) {
            }
        }
    }
    if (fileDeleted) {
        rescanDeletedPath(fileDirItem.path) {
            callback?.invoke(true)
        }
    }
}
fun Context.updateInMediaStore(oldPath: String, newPath: String) {
    Thread {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DATA, newPath)
            put(MediaStore.MediaColumns.DISPLAY_NAME, newPath.getFilenameFromPath())
            put(MediaStore.MediaColumns.TITLE, newPath.getFilenameFromPath())
        }
        val uri = getFileUri(oldPath)
        val selection = "${MediaStore.MediaColumns.DATA} = ?"
        val selectionArgs = arrayOf(oldPath)
        try {
            contentResolver.update(uri, values, selection, selectionArgs)
        } catch (ignored: Exception) {
        }
    }.start()
}
fun Context.updateLastModified(path: String, lastModified: Long) {
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DATE_MODIFIED, lastModified / 1000)
    }
    File(path).setLastModified(lastModified)
    val uri = getFileUri(path)
    val selection = "${MediaStore.MediaColumns.DATA} = ?"
    val selectionArgs = arrayOf(path)
    try {
        contentResolver.update(uri, values, selection, selectionArgs)
    } catch (ignored: Exception) {
    }
}
fun Context.updateTextColors(viewGroup: ViewGroup, tmpTextColor: Int = 0, tmpAccentColor: Int = 0) {
}
fun Context.showKeyboard(et: EditText) {
    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT)
}
//fun Context.toggleAppIconColor(appId: String, colorIndex: Int, color: Int, enable: Boolean) {
//    val className = "${appId.removeSuffix(".debug")}.activities.SplashActivity${appIconColorStrings[colorIndex]}"
//    val state = if (enable) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
//    try {
//        packageManager.setComponentEnabledSetting(ComponentName(appId, className), state, PackageManager.DONT_KILL_APP)
//        if (enable) {
//            baseConfig.lastIconColor = color
//        }
//    } catch (e: Exception) {
//    }
//}
