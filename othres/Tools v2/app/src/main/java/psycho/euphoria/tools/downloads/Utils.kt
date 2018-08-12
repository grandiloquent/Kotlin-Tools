package psycho.euphoria.tools.downloads

import psycho.euphoria.tools.commons.randomString
import java.io.File
import java.net.HttpURLConnection
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*


const val HTTP_ACCEPTED = 202
const val HTTP_BAD_GATEWAY = 502
const val HTTP_BAD_METHOD = 405
const val HTTP_BAD_REQUEST = 400
const val HTTP_CLIENT_TIMEOUT = 408
const val HTTP_CONFLICT = 409
const val HTTP_CREATED = 201
const val HTTP_ENTITY_TOO_LARGE = 413
const val HTTP_FORBIDDEN = 403
const val HTTP_GATEWAY_TIMEOUT = 504
const val HTTP_GONE = 410
const val HTTP_INTERNAL_ERROR = 500
const val HTTP_LENGTH_REQUIRED = 411
const val HTTP_MOVED_PERM = 301
const val HTTP_MOVED_TEMP = 302
const val HTTP_MULT_CHOICE = 300
const val HTTP_NO_CONTENT = 204
const val HTTP_NOT_ACCEPTABLE = 406
const val HTTP_NOT_AUTHORITATIVE = 203
const val HTTP_NOT_FOUND = 404
const val HTTP_NOT_IMPLEMENTED = 501
const val HTTP_NOT_MODIFIED = 304
const val HTTP_OK = 200
const val HTTP_PARTIAL = 206
const val HTTP_PAYMENT_REQUIRED = 402
const val HTTP_PRECON_FAILED = 412
const val HTTP_PROXY_AUTH = 407
const val HTTP_REQ_TOO_LONG = 414
const val HTTP_REQUESTED_RANGE_NOT_SATISFIABLE = 416
const val HTTP_RESET = 205
const val HTTP_SEE_OTHER = 303
const val HTTP_SERVER_ERROR = 500
const val HTTP_TEMP_REDIRECT = 307
const val HTTP_UNAUTHORIZED = 401
const val HTTP_UNAVAILABLE = 503
const val HTTP_UNSUPPORTED_TYPE = 415
const val HTTP_USE_PROXY = 305
const val HTTP_VERSION = 505
const val LENGTH_LONG = 10
const val LENGTH_MEDIUM = 20
const val LENGTH_SHORT = 30
const val LENGTH_SHORTER = 40
const val LENGTH_SHORTEST = 50

const val SECOND_IN_MILLIS = 1000
const val MINUTE_IN_MILLIS = SECOND_IN_MILLIS * 60
const val HOUR_IN_MILLIS = MINUTE_IN_MILLIS * 60
const val DAY_IN_MILLIS = HOUR_IN_MILLIS * 24
const val WEEK_IN_MILLIS = DAY_IN_MILLIS * 7

var HttpURLConnection.acceptEncoding: String
    get() = getHeaderField("Accept-Encoding")
    set(value) = addRequestProperty("Accept-Encoding", value)
var HttpURLConnection.connection: String
    get() = getHeaderField("Connection")
    set(value) = addRequestProperty("Connection", value)
var HttpURLConnection.contentLength_: String
    get() = getHeaderField("Content-Length")
    set(value) = addRequestProperty("Content-Length", value)
var HttpURLConnection.eTag: String?
    get() = getHeaderField("ETag")
    set(value) = addRequestProperty("ETag", value)
var HttpURLConnection.ifMatch: String
    get() = getHeaderField("If-Match")
    set(value) = addRequestProperty("If-Match", value)
var HttpURLConnection.location: String
    get() = getHeaderField("Location")
    set(value) = addRequestProperty("Location", value)
var HttpURLConnection.range: String
    get() = getHeaderField("Range")
    set(value) = addRequestProperty("Range", value)
var HttpURLConnection.transferEncoding: String?
    get() = getHeaderField("Transfer-Encoding")
    set(value) = addRequestProperty("Transfer-Encoding", value)


fun Double.getPercent(): String {
    return NumberFormat.getPercentInstance().format(this)
}

fun Int.getFormattedDuration(): String {
    val sb = StringBuilder(8)
    val hours = this / 3600
    val minutes = this % 3600 / 60
    val seconds = this % 60

    if (this >= 3600) {
        sb.append(String.format(Locale.getDefault(), "%02d", hours)).append(":")
    }

    sb.append(String.format(Locale.getDefault(), "%02d", minutes))
    sb.append(":").append(String.format(Locale.getDefault(), "%02d", seconds))
    return sb.toString()
}

fun Long.formatSize(): String {
    if (this <= 0)
        return "0 B"

    val units = arrayOf("B", "kB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(toDouble()) / Math.log10(1024.0)).toInt()
    return "${DecimalFormat("#,##0.#").format(this / Math.pow(1024.0, digitGroups.toDouble()))} ${units[digitGroups]}"
}

fun generateFileNameFromURL(url: String, directory: File): String {
    if (url.isBlank()) {
        var file = File(directory, ('a'..'z').randomString(6))
        while (file.exists()) {
            file = File(directory, ('a'..'z').randomString(6))
        }
        return file.absolutePath
    } else {
        var fileName = url.substringBefore('?')
        var invalidFileNameChars = arrayOf('\"', '<', '>', '|', 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, ':', '*', '?', '\\', '/')
        fileName = fileName.substringAfterLast('/').filter {
            !invalidFileNameChars.contains(it)
        }
        return File(directory, fileName).absolutePath
    }
}
