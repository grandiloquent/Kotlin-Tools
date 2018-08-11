package psycho.euphoria.common.extension

import android.content.Context
import android.os.Environment
import android.text.TextUtils
import java.io.File
import java.util.*
import java.util.regex.Pattern
import android.content.Intent
import android.content.ComponentName
import android.os.Build


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

fun Context.startForegroundServiceCompat(intent: Intent): ComponentName{
    return if (Build.VERSION.SDK_INT >= 26) {
        startForegroundService(intent)
    } else {
        startService(intent)
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