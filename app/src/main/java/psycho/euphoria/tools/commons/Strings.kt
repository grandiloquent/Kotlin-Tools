package psycho.euphoria.tools.commons

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Point
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Patterns
import psycho.euphoria.common.extension.getMimeType
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


fun String.areDigitsOnly() = matches(Regex("[0-9]+"))
fun String.getFilenameExtension() = substring(lastIndexOf(".") + 1)
fun String.isAudioFast() = audioExtensions.any { endsWith(it, true) }
fun String.isAudioSlow() = isAudioFast() || getMimeType().startsWith("audio")
fun String.isGif() = endsWith(".gif", true)
fun String.isImageFast() = photoExtensions.any { endsWith(it, true) }
fun String.isImageSlow() = isImageFast() || getMimeType().startsWith("image")
fun String.isImageVideoGif() = isImageFast() || isVideoFast() || isGif() || isRawFast()
fun String.isJpg() = endsWith(".jpg", true) or endsWith(".jpeg")
fun String.isPng() = endsWith(".png", true)
fun String.isRawFast() = rawExtensions.any { endsWith(it, true) }
fun String.isVideoFast() = videoExtensions.any { endsWith(it, true) }
fun String.isVideoSlow() = isVideoFast() || getMimeType().startsWith("video")
fun String.isArchiveFast() = archiveExtensions.any { endsWith(it, true) }
fun String.getDuration() = getFileDurationSeconds()?.getFormattedDuration()
fun String.getFilenameFromPath() = substring(lastIndexOf("/") + 1)

/*
Functions
 */

fun String.getExifCameraModel(exif: ExifInterface): String {
    exif.getAttribute(ExifInterface.TAG_MAKE).let {
        if (it?.isNotEmpty() == true) {
            val model = exif.getAttribute(ExifInterface.TAG_MODEL)
            return "$it $model".trim()
        }
    }
    return ""
}

fun String.getParentPath(): String {
    var parent = removeSuffix("/${getFilenameFromPath()}")
    if (parent == "otg:") {
        parent = OTG_PATH
    }
    return parent
}

fun String.getExifDateTaken(exif: ExifInterface): String {
    exif.getAttribute(ExifInterface.TAG_DATETIME).let {
        if (it?.isNotEmpty() == true) {
            try {
                val simpleDateFormat = SimpleDateFormat("yyyy:MM:dd kk:mm:ss", Locale.ENGLISH)
                return simpleDateFormat.parse(it).time.formatDate().trim()
            } catch (ignored: Exception) {
            }
        }
    }
    return ""
}

fun String.getExifProperties(exif: ExifInterface): String {
    var exifString = ""
    exif.getAttribute(ExifInterface.TAG_F_NUMBER).let {
        if (it?.isNotEmpty() == true) {
            val number = it.trimEnd('0').trimEnd('.')
            exifString += "F/$number  "
        }
    }
    exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH).let {
        if (it?.isNotEmpty() == true) {
            val values = it.split('/')
            val focalLength = "${values[0].toDouble() / values[1].toDouble()}mm"
            exifString += "$focalLength  "
        }
    }
    exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME).let {
        if (it?.isNotEmpty() == true) {
            val exposureSec = Math.round(1 / it.toFloat())
            exifString += "1/${exposureSec}s  "
        }
    }
    exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS).let {
        if (it?.isNotEmpty() == true) {
            exifString += "ISO-$it"
        }
    }
    return exifString.trim()
}

fun String.getImageResolution(): Point? {
    val options = BitmapFactory.Options()
    options.inJustDecodeBounds = true
    BitmapFactory.decodeFile(this, options)
    val width = options.outWidth
    val height = options.outHeight
    return if (width > 0 && height > 0) {
        Point(options.outWidth, options.outHeight)
    } else {
        null
    }
}


fun String.getResolution(): Point? {
    return if (isImageFast() || isImageSlow()) {
        getImageResolution()
    } else if (isVideoFast() || isVideoSlow()) {
        getVideoResolution()
    } else {
        null
    }
}

fun String.getFileAlbum(): String? {
    return try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(this)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
    } catch (ignored: Exception) {
        null
    }
}

fun String.getFileArtist(): String? {
    return try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(this)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
    } catch (ignored: Exception) {
        null
    }
}

fun String.getFileDurationSeconds(): Int? {
    return try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(this)
        val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val timeInMs = java.lang.Long.parseLong(time)
        (timeInMs / 1000).toInt()
    } catch (e: Exception) {
        null
    }
}

fun String.getVideoResolution(): Point? {
    return try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(this)
        val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH).toInt()
        val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT).toInt()
        Point(width, height)
    } catch (ignored: Exception) {
        null
    }
}

fun String.isValidURL(): Boolean {
    return Patterns.WEB_URL.matcher(this).matches() //URLUtil.isValidUrl(this)
}

fun String.getFileSongTitle(): String? {
    return try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(this)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
    } catch (ignored: Exception) {
        null
    }
}

fun String.triggerScanFile(context: Context = App.instance) {
    val file = File(this)
    if (!file.exists()) return

    val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
    val uri = Uri.fromFile(file)
    mediaScanIntent.data = uri
    context.sendBroadcast(mediaScanIntent)
}

fun String.getGenericMimeType(): String {
    if (!contains("/"))
        return this

    val type = substring(0, indexOf("/"))
    return "$type/*"
}

fun String.convertToSeconds(): Int {

    val strings = split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

    return if (strings.size > 1) {
        Integer.parseInt(strings[0]) * 60 + Integer.parseInt(strings[1])
    } else 0
}