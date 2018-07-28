package psycho.euphoria.tools.downloads

import android.content.Intent
import android.os.Process
import android.os.SystemClock
import android.util.Log
import psycho.euphoria.tools.commons.*
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile
import java.math.RoundingMode
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.round
import kotlin.math.roundToInt
import java.net.ProtocolException


class StopRequestException(val finalStatus: Int, message: String?, throwable: Throwable?) : Exception(message, throwable) {
    constructor(finalStatus: Int, throwable: Throwable) : this(finalStatus, null, throwable)
    constructor(finalStatus: Int, message: String) : this(finalStatus, message, null)

    companion object {

        @JvmStatic
        fun throwUnhandledHttpError(code: Int, message: String) {
            val error = "Unhandled HTTP response: $code $message"
            if (code in 400..599) {
                throw StopRequestException(code, error)
            } else if (code >= 300 && code < 400) {
                throw StopRequestException(STATUS_UNHANDLED_REDIRECT, error)
            } else {
                throw StopRequestException(STATUS_UNHANDLED_HTTP_CODE, error)
            }
        }
    }
}

class Downloader(private val downloadInfo: DownloadInfo) : Runnable {
    private var mSpeedSampleStart = 0L
    private var mSpeedSampleBytes = 0L
    private var mSpeed = 0L
    private val mId: Long;
    private var mLastUpdateBytes = 0L
    private var mLastUpdateTime = 0L
    var notifyDownloadSpeed: ((Long, Long) -> Unit)? = null
    var notifyCompleted: ((Long) -> Unit)? = null
    var notifyErrorOccurred: ((Long, String?) -> Unit)? = null
    var notifyStart: ((Long) -> Unit)? = null

    init {
        mId = downloadInfo.id
    }

    private fun addRequestHeaders(conn: HttpURLConnection, resuming: Boolean) {
        for (header in downloadInfo.getHeaders()) {
            conn.addRequestProperty(header.first, header.second)
        }

        if (conn.getRequestProperty("User-Agent") == null) {
            conn.userAgent = DEFAULT_USER_AGENT
        }
        conn.acceptEncoding = "identity"
        conn.connection = "close"
        if (resuming) {

            downloadInfo.etag?.let { conn.ifMatch = it }
            conn.range = "bytes=" + downloadInfo.currentBytes + "-";
        }
    }

    private fun executeDownload() {
        val resuming = downloadInfo.currentBytes != 0L
        var url = URL(downloadInfo.url)
        var redirectCount = 0
        outer@ while (redirectCount++ < MAX_REDIRECTS) {
            Tracker.e("executeDownload", "redirectCount => $redirectCount")
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
                    HTTP_OK -> {
                        if (resuming) throw   StopRequestException(
                                STATUS_CANNOT_RESUME, "Expected partial, but received OK");
                        parseOkHeaders(con)
                        transferData(con)


                        return
                    }
                    HTTP_PARTIAL -> {
                        if (!resuming) throw  StopRequestException(
                                STATUS_CANNOT_RESUME, "Expected OK, but received partial");
                        transferData(con)

                        return
                    }
                    HTTP_MOVED_PERM,
                    HTTP_MOVED_TEMP,
                    HTTP_SEE_OTHER,
                    HTTP_TEMP_REDIRECT -> {
                        url = URL(url, con.location)
                        if (responseCode == HttpURLConnection.HTTP_MOVED_PERM) {
                            downloadInfo.url = url.toString()
                        }
                        return@outer
                    }
                    HTTP_PRECON_FAILED -> throw StopRequestException(
                            STATUS_CANNOT_RESUME, "Precondition failed");

                    HTTP_REQUESTED_RANGE_NOT_SATISFIABLE -> throw  StopRequestException(
                            STATUS_CANNOT_RESUME, "Requested range not satisfiable");
                    HTTP_UNAVAILABLE -> throw  StopRequestException(
                            HTTP_UNAVAILABLE, con.responseMessage);
                    HTTP_INTERNAL_ERROR -> throw  StopRequestException(
                            HTTP_INTERNAL_ERROR, con.responseMessage);
                    else -> {

                        StopRequestException.throwUnhandledHttpError(
                                responseCode, con.responseMessage);
                    }
                }
            } catch (e: IOException) {
                if (e is ProtocolException && e.message!!.startsWith("Unexpected status line")) {
                    throw StopRequestException(STATUS_UNHANDLED_HTTP_CODE, e)
                } else {
                    Tracker.e("executeDownload", "throw IOException")
                    // Trouble with low-level sockets
                    throw StopRequestException(STATUS_HTTP_DATA_ERROR, e)
                }
            } finally {
                con?.disconnect()
            }
        }
        throw StopRequestException(STATUS_TOO_MANY_REDIRECTS, "Too many redirects")
    }

    private fun parseOkHeaders(conn: HttpURLConnection) {
        if (conn.transferEncoding == null) {
            downloadInfo.totalBytes = conn.contentLength_.toLongOrNull() ?: -1L
        } else {
            downloadInfo.totalBytes = -1L
        }
        downloadInfo.etag = conn.eTag
        downloadInfo.writeToDatabase()
        // Tracker.e("parseOkHeaders", "totalBytes => ${downloadInfo.totalBytes}")

        // Log.e(TAG, "$contentDisposition $contentLocation transferEncoding => ${transferEncoding} \nmimeType => ${downloadInfo.mimeType} \ntotalBytes => ${downloadInfo.totalBytes.formatSize()} \netag => ${downloadInfo.etag} \n")
    }

    override fun run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
        try {
            notifyStart?.invoke(mId)
            Logger.getInstance().d("Downloader called the method: run, fileName => ${downloadInfo.fileName}")
            executeDownload()
            if (downloadInfo.totalBytes != -1L && downloadInfo.currentBytes >= downloadInfo.totalBytes) {
                Logger.getInstance().d("Downloader. Because the totalBytes is -1L,set the finish to true.")
                downloadInfo.finish = true
            }
            notifyCompleted?.invoke(mId)
            Logger.getInstance().d("Downloader.run.notifyCompleted")
        } catch (e: StopRequestException) {
            // 489 Requested range not satisfiable
            if (e.finalStatus == STATUS_CANNOT_RESUME /*489*/ && downloadInfo.currentBytes == downloadInfo.totalBytes) {
                downloadInfo.finish = true
            }
            Logger.getInstance().e("Downloader.run.${e.finalStatus} ${e.message}")
            notifyErrorOccurred?.invoke(mId, e.message)
        } finally {
            downloadInfo.writeToDatabase()

        }
    }

    private fun transferData(conn: HttpURLConnection) {


        if (!(downloadInfo.totalBytes != -1L || "close".equals(conn.connection, true) || "chunked".equals(conn.transferEncoding, true))) {
            throw StopRequestException(STATUS_CANNOT_RESUME, "can't know size of download, giving up")
        }
        var inputStream: InputStream? = null
        var outputStream: RandomAccessFile?

        try {
            inputStream = conn.inputStream
        } catch (e: IOException) {
            throw StopRequestException(STATUS_HTTP_DATA_ERROR, e)
        }
        outputStream = RandomAccessFile(downloadInfo.fileName, "rwd")
        outputStream.seek(downloadInfo.currentBytes)
        inputStream.use { input ->
            outputStream.use { output ->

                val buffer = ByteArray(8 * 1024)
                var bytes = input.read(buffer)
                while (bytes >= 0) {
                    output.write(buffer, 0, bytes)
                    downloadInfo.currentBytes += bytes
                    updateProgress()
                    bytes = input.read(buffer)
                }
            }
        }

    }


    private fun updateProgress() {
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
                notifyDownloadSpeed?.invoke(mId, mSpeed)
            }
            mSpeedSampleStart = now
            mSpeedSampleBytes = currentBytes
        }
        val bytesDelta = currentBytes - mLastUpdateBytes;
        val timeDelta = now - mLastUpdateTime;
        if (bytesDelta > MIN_PROGRESS_STEP && timeDelta > MIN_PROGRESS_TIME) {


            downloadInfo.writeToDatabase()
            mLastUpdateBytes = currentBytes;
            mLastUpdateTime = now;
        }
    }

    companion object {
        private const val TAG = "Downloader"
        private const val DEFAULT_TIMEOUT = 20000
    }
}
