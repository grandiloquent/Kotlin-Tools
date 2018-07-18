package psycho.euphoria.tools

import android.content.Intent
import android.os.Process
import android.os.SystemClock
import android.util.Log
import java.io.InputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL


class Downloader(private val downloadInfo: DownloadInfo) : Thread() {
    private var mSpeedSampleStart = 0L
    private var mSpeedSampleBytes = 0L
    private var mSpeed = 0L
    var notifyDownloadSpeed: ((Long, Long) -> Unit)? = null
    var notifyCompleted: ((Long, String) -> Unit)? = null
    var notifyErrorOccurred: ((Long, String?) -> Unit)? = null

    private fun addRequestHeaders(conn: HttpURLConnection, resuming: Boolean) {
        for (header in downloadInfo.getHeaders()) {
            conn.addRequestProperty(header.first, header.second)
        }
        if (conn.getRequestProperty(UserAgent) == null) {
            conn.addRequestProperty(UserAgent, HEADER_USER_AGENT)
        }
        conn.addRequestProperty(AcceptEncoding, "identity")
        conn.addRequestProperty(Connection, "close")
        if (resuming) {
            if (downloadInfo.etag != null) {
                conn.addRequestProperty(IfMatch, downloadInfo.etag)
            }
            conn.addRequestProperty(Range, "bytes=" + downloadInfo.currentBytes + "-");
        }
    }

    private fun executeDownload() {
        val resuming = downloadInfo.currentBytes != 0L
        var url = URL(downloadInfo.url)
        var redirectCount = 0
        while (redirectCount++ < MAX_REDIRECTS) {
            var con: HttpURLConnection? = null
            try {
                con = ((if (downloadInfo.proxy != null) url.openConnection(downloadInfo.proxy) else url.openConnection()) as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    connectTimeout = DEFAULT_TIMEOUT
                    readTimeout = DEFAULT_TIMEOUT
                }
                addRequestHeaders(con, resuming)
                val responseCode = con.responseCode
                when (responseCode) {
                    HttpURLConnection.HTTP_OK -> {
                        if (resuming) throw Exception("Expected partial, but received OK")
                        parseOkHeaders(con)
                        transferData(con)
                        return
                    }
                    HttpURLConnection.HTTP_PARTIAL -> {
                        if (!resuming) throw  Exception("Expected OK, but received partial")
                        Log.e(TAG, "[executeDownload]:HTTP_PARTIAL")
                        transferData(con)
                        return
                    }
                    HttpURLConnection.HTTP_MOVED_PERM,
                    HttpURLConnection.HTTP_MOVED_TEMP,
                    HttpURLConnection.HTTP_SEE_OTHER,
                    HTTP_TEMP_REDIRECT -> {
                        val location = con.getHeaderField(Location)
                        url = URL(url, location)
                        if (responseCode == HttpURLConnection.HTTP_MOVED_PERM) {
                            downloadInfo.url = url.toString()
                        }
                    }
                    HttpURLConnection.HTTP_PRECON_FAILED,
                    HTTP_REQUESTED_RANGE_NOT_SATISFIABLE,
                    HttpURLConnection.HTTP_UNAVAILABLE,
                    HttpURLConnection.HTTP_INTERNAL_ERROR -> {
                    }
                    else -> {
                    }
                }
            } catch (e: Exception) {
                notifyErrorOccurred?.invoke(0L, e.message)
                Log.e(TAG, "[executeDownload]:${e.message}")
            } finally {
                con?.disconnect()
            }
        }
    }

    private fun parseOkHeaders(conn: HttpURLConnection) {
        val contentDisposition = conn.getHeaderField("Content-Disposition");
        val contentLocation = conn.getHeaderField("Content-Location");
        if (downloadInfo.mimeType == null) {
            downloadInfo.mimeType = Intent.normalizeMimeType(conn.contentType)
        }
        val transferEncoding = conn.getHeaderField(TransferEncoding)
        if (transferEncoding == null) {
            downloadInfo.totalBytes = conn.getHeaderField(ContentLength).toLongOrNull() ?: -1L
        } else {
            downloadInfo.totalBytes = -1L
        }
        downloadInfo.etag = conn.getHeaderField("ETag")
        Log.e(TAG, "$contentDisposition $contentLocation transferEncoding => ${transferEncoding} \nmimeType => ${downloadInfo.mimeType} \ntotalBytes => ${downloadInfo.totalBytes.formatSize()} \netag => ${downloadInfo.etag} \n")
    }

    override fun run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
        executeDownload()
        notifyCompleted?.invoke(0L, downloadInfo.fileName)
    }

    private fun transferData(conn: HttpURLConnection) {
        val hasLength = downloadInfo.totalBytes != -1L
        val isConnectionClose = "close".equals(conn.getHeaderField(Connection), true)
        val isEncodingChunked = "chunked".equals(conn.getHeaderField(TransferEncoding), true)
        val finishKnown = hasLength || isConnectionClose || isEncodingChunked
        if (!finishKnown) {
            throw Exception("can't know size of download, giving up")
        }
        var inputStream: InputStream? = null
        var outputStream: RandomAccessFile? = null
        try {
            inputStream = conn.inputStream
            outputStream = RandomAccessFile(downloadInfo.fileName, "rwd")
            outputStream.seek(downloadInfo.currentBytes)
            inputStream.use { input ->
                outputStream.use { output ->

                    val buffer = ByteArray(8 * 1024)
                    var bytes = input.read(buffer)
                    while (bytes >= 0) {
                        output.write(buffer, 0, bytes)
                        downloadInfo.currentBytes += bytes
                        bytes = input.read(buffer)
                        updateProgress()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "[transferData]:${e.message}")
        }
    }


    fun updateProgress() {
        val now = SystemClock.elapsedRealtime()
        val currentBytes = downloadInfo.currentBytes
        val sampleDelta = now - mSpeedSampleStart
        if (sampleDelta > 500) {
            val sampleSpeed = (currentBytes - mSpeedSampleBytes) * 1000 / sampleDelta
            if (mSpeed == 0L) {
                mSpeed = sampleSpeed
            } else {
                mSpeed = (mSpeed * 3 + sampleSpeed) / 4
            }

            if (mSpeedSampleStart != 0L) {
                notifyDownloadSpeed?.invoke(1L, mSpeed)
            }
            mSpeedSampleStart = now
            mSpeedSampleBytes = currentBytes
        }

    }

    companion object {
        private const val TAG = "Downloader"
        private const val TransferEncoding = "Transfer-Encoding";
        private const val Location = "Location";
        private const val ContentLength = "Content-Length"
        private const val HTTP_TEMP_REDIRECT = 307
        private const val HTTP_REQUESTED_RANGE_NOT_SATISFIABLE = 416
        private const val AcceptEncoding = "Accept-Encoding";
        private const val Connection = "Connection";
        private const val DEFAULT_TIMEOUT = 20000
        private const val HEADER_USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.99 Safari/537.36"
        private const val IfMatch = "If-Match";
        private const val MAX_REDIRECTS = 5
        private const val Range = "Range";
        private const val UserAgent = "User-Agent";
    }
}
