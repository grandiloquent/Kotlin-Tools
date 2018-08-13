package psycho.euphoria.common.extension

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.support.v4.content.FileProvider
import android.support.v4.provider.DocumentFile
import android.text.TextUtils
import android.util.TypedValue
import android.widget.Toast
import psycho.euphoria.common.*
import java.io.File
import java.util.*
import java.util.regex.Pattern


fun Context.getDoesFilePathExist(path: String) = if (path.startsWith(OTG_PATH)) getOTGFastDocumentFile(path)?.exists()
        ?: false else File(path).exists()
val Context.widthPixels get() = resources.displayMetrics.widthPixels
val Context.heightPixels get() = resources.displayMetrics.heightPixels
val Context.sdCardPath: String get() = Services.sdCardPath
fun Context.needsStupidWritePermissions(path: String) = (isPathOnSD(path) || path.startsWith(OTG_PATH)) && isLollipopPlus
fun Context.isPathOnSD(path: String) = sdCardPath.isNotEmpty() && path.startsWith(sdCardPath)
fun Context.dp2px(dp: Float): Float {
    return dp * Services.density;
}

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



fun Context.createNotificationChannel(channelId: String, channelName: String, channelImportance: Int = NotificationManager.IMPORTANCE_MIN) {
    /**
     * Creates a notification channel that notifications can be posted to.
     *
     * This can also be used to restore a deleted channel and to update an existing channel's
     * name, description, and/or importance.
     *
     * <p>The name and description should only be changed if the locale changes
     * or in response to the user renaming this channel. For example, if a user has a channel
     * named 'John Doe' that represents messages from a 'John Doe', and 'John Doe' changes his name
     * to 'John Smith,' the channel can be renamed to match.
     *
     * <p>The importance of an existing channel will only be changed if the new importance is lower
     * than the current value and the user has not altered any settings on this channel.
     *
     * All other fields are ignored for channels that already exist.
     *
     * @param channel  the channel to create.  Note that the created channel may differ from this
     *                 value. If the provided channel is malformed, a RemoteException will be
     *                 thrown.
     */
    /**
     * Creates a notification channel.
     *
     * @param id The id of the channel. Must be unique per package. The value may be truncated if
     *           it is too long.
     * @param name The user visible name of the channel. You can rename this channel when the system
     *             locale changes by listening for the {@link Intent#ACTION_LOCALE_CHANGED}
     *             broadcast. The recommended maximum length is 40 characters; the value may be
     *             truncated if it is too long.
     * @param importance The importance of the channel. This controls how interruptive notifications
     *                   posted to this channel are.
     */
    /**
     * Min notification importance: only shows in the shade, below the fold.  This should
     * not be used with {@link Service#startForeground(int, Notification) Service.startForeground}
     * since a foreground service is supposed to be something the user cares about so it does
     * not make semantic sense to mark its notification as minimum importance.  If you do this
     * as of Android version {@link android.os.Build.VERSION_CODES#O}, the system will show
     * a higher-priority notification about your app running in the background.
     *    public static final int IMPORTANCE_MIN = 1;
     */
    if (isOPlus) {
        Services.notificationManager.createNotificationChannel(NotificationChannel(channelId, channelName, channelImportance))
    }
}
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
fun Context.ensurePublicUri(uri: Uri, applicationId: String): Uri {
    return if (uri.scheme == "content") {
        uri
    } else {
        val file = File(uri.path)
        getFilePublicUri(file, applicationId)
    }
}
fun Context.ensurePublicUri(path: String, applicationId: String): Uri? {
    return if (path.startsWith(OTG_PATH)) {
        getDocumentFile(path)?.uri
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
fun Context.getDocumentFile(path: String): DocumentFile? {
    if (!isLollipopPlus) {
        return null
    }
    val isOTG = path.startsWith(OTG_PATH)
    var relativePath = path.substring(if (isOTG) OTG_PATH.length else sdCardPath.length)
    if (relativePath.startsWith(File.separator)) {
        relativePath = relativePath.substring(1)
    }
    var document = DocumentFile.fromTreeUri(applicationContext, Uri.parse(if (isOTG) Services.OTGTreeUri else Services.treeUri))
    val parts = relativePath.split("/").filter { it.isNotEmpty() }
    for (part in parts) {
        document = document?.findFile(part)
    }
    return document
}
fun Context.getFastDocumentFile(path: String): DocumentFile? {
    if (!isLollipopPlus) {
        return null
    }
    if (path.startsWith(OTG_PATH)) {
        return getOTGFastDocumentFile(path)
    }
    val sdCardPath = Services.sdCardPath
    if (sdCardPath.isEmpty()) {
        return null
    }
    val relativePath = Uri.encode(path.substring(sdCardPath.length).trim('/'))
    val externalPathPart = sdCardPath.split("/").lastOrNull(String::isNotEmpty)?.trim('/')
            ?: return null
    val fullUri = "${Services.treeUri}/document/$externalPathPart%3A$relativePath"
    return DocumentFile.fromSingleUri(this, Uri.parse(fullUri))
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
    if (Services.OTGTreeUri.isEmpty()) {
        return null
    }
    if (Services.OTGPartition.isEmpty()) {
        Services.OTGPartition = Services.OTGTreeUri.removeSuffix("%3A").substringAfterLast('/')
    }
    val relativePath = Uri.encode(path.substring(OTG_PATH.length).trim('/'))
    val fullUri = "${Services.OTGTreeUri}/document/${Services.OTGPartition}%3A$relativePath"
    return DocumentFile.fromSingleUri(this, Uri.parse(fullUri))
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
fun Context.getUriMimeType(path: String, newUri: Uri): String {
    var mimeType = path.getMimeType()
    if (mimeType.isEmpty()) {
        mimeType = getMimeTypeFromUri(newUri)
    }
    return mimeType
}
fun Context.hasProperStoredTreeUri(): Boolean {
    if (isKitkatPlus) {
        val hasProperUri = contentResolver.persistedUriPermissions.any { it.uri.toString() == Services.treeUri }
        if (!hasProperUri) {
            Services.treeUri = ""
        }
        return hasProperUri
    }
    return false
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
    MediaScannerConnection.scanFile(applicationContext, paths.toTypedArray(), null) { s, uri ->
        if (--cnt == 0) {
            callback?.invoke()
        }
    }
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
fun Context.startForegroundServiceCompat(intent: Intent): ComponentName {
    return if (Build.VERSION.SDK_INT >= 26) {
        startForegroundService(intent)
    } else {
        startService(intent)
    }
}
fun Context.toast(id: Int, length: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, id, length).show()
}
fun Context.toast(message: String, length: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, length).show()
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
fun Context.trySAFFileDelete(file: File, allowDeleteFolder: Boolean = false, callback: ((wasSuccess: Boolean) -> Unit)? = null) {
    var fileDeleted = tryFastDocumentDelete(file.path, allowDeleteFolder)
    if (!fileDeleted) {
        val document = getDocumentFile(file.path)
        if (document != null && (file.isDirectory == document.isDirectory)) {
            try {
                fileDeleted = (document.isFile == true || allowDeleteFolder) && DocumentsContract.deleteDocument(applicationContext.contentResolver, document.uri)
            } catch (ignored: Exception) {
            }
        }
    }
    if (fileDeleted) {
        rescanDeletedPath(file.path) {
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
fun getPaths(file: File): ArrayList<String> {
    val paths = arrayListOf<String>(file.absolutePath)
    if (file.isDirectory) {
        val files = file.listFiles() ?: return paths
        for (curFile in files) {
            paths.addAll(getPaths(curFile))
        }
    }
    return paths
}