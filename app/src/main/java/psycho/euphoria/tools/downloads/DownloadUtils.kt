package psycho.euphoria.tools.downloads

import android.content.Context
import android.os.Environment
import psycho.euphoria.tools.commons.randomString
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.text.SimpleDateFormat
import java.util.*



var HttpURLConnection.accept: String
    get() = getHeaderField("Accept")
    set(value) = addRequestProperty("Accept", value)

var HttpURLConnection.acceptCharset: String
    get() = getHeaderField("Accept-Charset")
    set(value) = addRequestProperty("Accept-Charset", value)

var HttpURLConnection.acceptEncoding: String
    get() = getHeaderField("Accept-Encoding")
    set(value) = addRequestProperty("Accept-Encoding", value)

var HttpURLConnection.acceptLanguage: String
    get() = getHeaderField("Accept-Language")
    set(value) = addRequestProperty("Accept-Language", value)

var HttpURLConnection.acceptRanges: String
    get() = getHeaderField("Accept-Ranges")
    set(value) = addRequestProperty("Accept-Ranges", value)

var HttpURLConnection.age: String
    get() = getHeaderField("Age")
    set(value) = addRequestProperty("Age", value)

var HttpURLConnection.allow: String
    get() = getHeaderField("Allow")
    set(value) = addRequestProperty("Allow", value)

var HttpURLConnection.authorization: String
    get() = getHeaderField("Authorization")
    set(value) = addRequestProperty("Authorization", value)

var HttpURLConnection.cacheControl: String
    get() = getHeaderField("Cache-Control")
    set(value) = addRequestProperty("Cache-Control", value)

var HttpURLConnection.connection: String
    get() = getHeaderField("Connection")
    set(value) = addRequestProperty("Connection", value)

var HttpURLConnection.contentDisposition: String
    get() = getHeaderField("Content-Disposition")
    set(value) = addRequestProperty("Content-Disposition", value)

var HttpURLConnection.contentEncoding: String
    get() = getHeaderField("Content-Encoding")
    set(value) = addRequestProperty("Content-Encoding", value)

var HttpURLConnection.contentLanguage: String
    get() = getHeaderField("Content-Language")
    set(value) = addRequestProperty("Content-Language", value)

var HttpURLConnection.contentLength_: String
    get() = getHeaderField("Content-Length")
    set(value) = addRequestProperty("Content-Length", value)

var HttpURLConnection.contentLocation: String
    get() = getHeaderField("Content-Location")
    set(value) = addRequestProperty("Content-Location", value)

var HttpURLConnection.contentMD5: String
    get() = getHeaderField("Content-MD5")
    set(value) = addRequestProperty("Content-MD5", value)

var HttpURLConnection.contentRange: String
    get() = getHeaderField("Content-Range")
    set(value) = addRequestProperty("Content-Range", value)

var HttpURLConnection.contentType: String
    get() = getHeaderField("Content-Type")
    set(value) = addRequestProperty("Content-Type", value)

var HttpURLConnection.cookie: String
    get() = getHeaderField("Cookie")
    set(value) = addRequestProperty("Cookie", value)

var HttpURLConnection.cookie2: String
    get() = getHeaderField("Cookie2")
    set(value) = addRequestProperty("Cookie2", value)

var HttpURLConnection.date: String
    get() = getHeaderField("Date")
    set(value) = addRequestProperty("Date", value)

var HttpURLConnection.eTag: String?
    get() = getHeaderField("ETag")
    set(value) = addRequestProperty("ETag", value)

var HttpURLConnection.expect: String
    get() = getHeaderField("Expect")
    set(value) = addRequestProperty("Expect", value)

var HttpURLConnection.expires: String
    get() = getHeaderField("Expires")
    set(value) = addRequestProperty("Expires", value)

var HttpURLConnection.from: String
    get() = getHeaderField("From")
    set(value) = addRequestProperty("From", value)

var HttpURLConnection.host: String
    get() = getHeaderField("Host")
    set(value) = addRequestProperty("Host", value)

var HttpURLConnection.ifMatch: String
    get() = getHeaderField("If-Match")
    set(value) = addRequestProperty("If-Match", value)

var HttpURLConnection.ifModifiedSince: String
    get() = getHeaderField("If-Modified-Since")
    set(value) = addRequestProperty("If-Modified-Since", value)

var HttpURLConnection.ifNoneMatch: String
    get() = getHeaderField("If-None-Match")
    set(value) = addRequestProperty("If-None-Match", value)

var HttpURLConnection.ifRange: String
    get() = getHeaderField("If-Range")
    set(value) = addRequestProperty("If-Range", value)

var HttpURLConnection.ifUnmodifiedSince: String
    get() = getHeaderField("If-Unmodified-Since")
    set(value) = addRequestProperty("If-Unmodified-Since", value)

var HttpURLConnection.keepAlive: String
    get() = getHeaderField("Keep-Alive")
    set(value) = addRequestProperty("Keep-Alive", value)

var HttpURLConnection.lastModified: String
    get() = getHeaderField("Last-Modified")
    set(value) = addRequestProperty("Last-Modified", value)

var HttpURLConnection.location: String
    get() = getHeaderField("Location")
    set(value) = addRequestProperty("Location", value)

var HttpURLConnection.maxForwards: String
    get() = getHeaderField("Max-Forwards")
    set(value) = addRequestProperty("Max-Forwards", value)

var HttpURLConnection.origin: String
    get() = getHeaderField("Origin")
    set(value) = addRequestProperty("Origin", value)

var HttpURLConnection.p3P: String
    get() = getHeaderField("P3P")
    set(value) = addRequestProperty("P3P", value)

var HttpURLConnection.pragma: String
    get() = getHeaderField("Pragma")
    set(value) = addRequestProperty("Pragma", value)

var HttpURLConnection.proxyAuthenticate: String
    get() = getHeaderField("Proxy-Authenticate")
    set(value) = addRequestProperty("Proxy-Authenticate", value)

var HttpURLConnection.proxyAuthorization: String
    get() = getHeaderField("Proxy-Authorization")
    set(value) = addRequestProperty("Proxy-Authorization", value)

var HttpURLConnection.proxyConnection: String
    get() = getHeaderField("Proxy-Connection")
    set(value) = addRequestProperty("Proxy-Connection", value)

var HttpURLConnection.range: String
    get() = getHeaderField("Range")
    set(value) = addRequestProperty("Range", value)

var HttpURLConnection.referer: String
    get() = getHeaderField("Referer")
    set(value) = addRequestProperty("Referer", value)

var HttpURLConnection.retryAfter: String
    get() = getHeaderField("Retry-After")
    set(value) = addRequestProperty("Retry-After", value)

var HttpURLConnection.secWebSocketAccept: String
    get() = getHeaderField("Sec-WebSocket-Accept")
    set(value) = addRequestProperty("Sec-WebSocket-Accept", value)

var HttpURLConnection.secWebSocketExtensions: String
    get() = getHeaderField("Sec-WebSocket-Extensions")
    set(value) = addRequestProperty("Sec-WebSocket-Extensions", value)

var HttpURLConnection.secWebSocketKey: String
    get() = getHeaderField("Sec-WebSocket-Key")
    set(value) = addRequestProperty("Sec-WebSocket-Key", value)

var HttpURLConnection.secWebSocketProtocol: String
    get() = getHeaderField("Sec-WebSocket-Protocol")
    set(value) = addRequestProperty("Sec-WebSocket-Protocol", value)

var HttpURLConnection.secWebSocketVersion: String
    get() = getHeaderField("Sec-WebSocket-Version")
    set(value) = addRequestProperty("Sec-WebSocket-Version", value)

var HttpURLConnection.server: String
    get() = getHeaderField("Server")
    set(value) = addRequestProperty("Server", value)

var HttpURLConnection.setCookie: String
    get() = getHeaderField("Set-Cookie")
    set(value) = addRequestProperty("Set-Cookie", value)

var HttpURLConnection.setCookie2: String
    get() = getHeaderField("Set-Cookie2")
    set(value) = addRequestProperty("Set-Cookie2", value)

var HttpURLConnection.tE: String
    get() = getHeaderField("TE")
    set(value) = addRequestProperty("TE", value)

var HttpURLConnection.trailer: String
    get() = getHeaderField("Trailer")
    set(value) = addRequestProperty("Trailer", value)

var HttpURLConnection.transferEncoding: String?
    get() = getHeaderField("Transfer-Encoding")
    set(value) = addRequestProperty("Transfer-Encoding", value)

var HttpURLConnection.upgrade: String
    get() = getHeaderField("Upgrade")
    set(value) = addRequestProperty("Upgrade", value)

var HttpURLConnection.userAgent: String
    get() = getHeaderField("User-Agent")
    set(value) = addRequestProperty("User-Agent", value)

var HttpURLConnection.vary: String
    get() = getHeaderField("Vary")
    set(value) = addRequestProperty("Vary", value)

var HttpURLConnection.via: String
    get() = getHeaderField("Via")
    set(value) = addRequestProperty("Via", value)

var HttpURLConnection.warning: String
    get() = getHeaderField("Warning")
    set(value) = addRequestProperty("Warning", value)

var HttpURLConnection.wWWAuthenticate: String
    get() = getHeaderField("WWW-Authenticate")
    set(value) = addRequestProperty("WWW-Authenticate", value)

var HttpURLConnection.xAspNetVersion: String
    get() = getHeaderField("X-AspNet-Version")
    set(value) = addRequestProperty("X-AspNet-Version", value)

var HttpURLConnection.xPoweredBy: String
    get() = getHeaderField("X-Powered-By")
    set(value) = addRequestProperty("X-Powered-By", value)

fun getTimeStamp(): Long {
    return System.currentTimeMillis() / 1000
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

