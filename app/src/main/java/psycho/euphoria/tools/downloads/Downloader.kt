package psycho.euphoria.tools.downloads

import android.util.Log
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

class Downloader(private val notifySpeed: (TaskState) -> Unit) {
    private var mSpeedSampleStart = 0L
    private var mSpeedSampleBytes = 0L
    private var mSpeed = 0L
    private var mLastUpdateBytes = 0L
    private var mLastUpdateTime = 0L
    private fun addRequest(httpURLConnection: HttpURLConnection?, downloadInfo: DownloadInfo) {
        httpURLConnection?.let {
            val file = File(downloadInfo.fileName)
            if (file.exists()) {
                downloadInfo.currentBytes = file.length()
                downloadInfo.totalBytes += downloadInfo.currentBytes
                downloadInfo.etag?.apply {
                    it.ifMatch = this
                }
                it.range = "bytes=" + downloadInfo.currentBytes + "-";
            }
            it.acceptEncoding = "identity"
            it.connection = "close"
        }
    }

    private fun dispatchErrorMessage(e: Exception, message: String? = null) {
        Log.e(TAG, "dispatchErrorMessage $e $message")
    }

    fun execute(downloadInfo: DownloadInfo) {
        Log.e(TAG, "execute ${downloadInfo.id}")
        var url = URL(downloadInfo.uri)
        var httpURLConnection: HttpURLConnection? = null
        try {
            httpURLConnection = (url.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = DEFAULT_TIME_OUT
                readTimeout = DEFAULT_TIME_OUT
            }
            addRequest(httpURLConnection, downloadInfo)
            var responseCode = httpURLConnection.responseCode
            branch@ when (responseCode) {
                HTTP_OK -> {
                    parseHeaders(httpURLConnection, downloadInfo)
                    transferData(httpURLConnection, downloadInfo)
                    // Download task has been completed, update the database
                    downloadInfo.finish = 1
                    Log.e(TAG, "writeDatabase")
                    downloadInfo.writeDatabase()
                }
                HTTP_PARTIAL -> {
//                    if (downloadInfo.totalBytes <= 0L)// The cached information may be lost and re-parsed
                    parseHeaders(httpURLConnection, downloadInfo)
                    transferData(httpURLConnection, downloadInfo)
                    // Download task has been completed, update the database
                    downloadInfo.finish = 1
                    Log.e(TAG, "writeDatabase")
                    downloadInfo.writeDatabase()
                }
                HTTP_MOVED_PERM,
                HTTP_MOVED_TEMP,
                HTTP_SEE_OTHER,
                HTTP_TEMP_REDIRECT -> {
                    val location = httpURLConnection.location
                    url = URL(url, location)
                    if (responseCode == HTTP_MOVED_PERM) {
                        downloadInfo.uri = url.toString()
                    }
                    return@branch
                }
                HTTP_REQUESTED_RANGE_NOT_SATISFIABLE->{
                    downloadInfo.finish = 1
                    Log.e(TAG, "${downloadInfo.id} range not satisfiable")
                    downloadInfo.writeDatabase()
                }
                else -> throw Exception("Response code $responseCode")
            }
        } catch (malformedURLException: MalformedURLException) {
            dispatchErrorMessage(malformedURLException, "${downloadInfo.id}")
            // The resource address is incorrect, the task cannot be restarted, so it is marked as completed
            downloadInfo.finish = 1

            Log.e(TAG, "writeDatabase", malformedURLException)
            downloadInfo.writeDatabase()
        } catch (ignored: Exception) {
            Log.e(TAG, "writeDatabase", ignored)

            dispatchErrorMessage(ignored)
            downloadInfo.failedCount += 1
            downloadInfo.writeDatabase()
        } finally {
            httpURLConnection?.disconnect()
        }
    }

    private fun parseHeaders(httpURLConnection: HttpURLConnection?, downloadInfo: DownloadInfo) {
        httpURLConnection?.let {
            for (header in it.headerFields) {
                println("${header.key} ${header.value}")
            }
            downloadInfo.etag = it.eTag
            if (it.transferEncoding == null) {
                downloadInfo.totalBytes += (httpURLConnection.contentLength_.toLongOrNull() ?: 0L)
            }
            println(downloadInfo.toString())
        }
    }

    private fun transferData(httpURLConnection: HttpURLConnection?, downloadInfo: DownloadInfo) {
        Log.e(TAG, "transferData ${downloadInfo.id}")
        if (downloadInfo.currentBytes >= downloadInfo.totalBytes) {
            Log.e(TAG, "[ERROR]: downloadInfo => ${downloadInfo} \n")
            return
        }
        val inputStream = httpURLConnection?.inputStream
        val outputStream = RandomAccessFile(downloadInfo.fileName, "rwd")
        if (downloadInfo.currentBytes > 0L)
            outputStream.seek(downloadInfo.currentBytes)
        inputStream?.use { input ->
            outputStream.use { output ->
                val buffer = ByteArray(8 * 1024)
                var bytes = input.read(buffer)
                while (bytes >= 0) {
                    output.write(buffer, 0, bytes)
                    downloadInfo.currentBytes += bytes
                    updateProgress(downloadInfo)
                    bytes = input.read(buffer)
                    if (downloadInfo.currentBytes > downloadInfo.totalBytes) {
                        Log.e(TAG, "[ERROR]: currentBytes>totalBytes")
                    }
                }
            }
        }
        Log.e(TAG, "transferData ${downloadInfo.currentBytes}")

    }

    private fun updateProgress(downloadInfo: DownloadInfo) {
        val now = System.currentTimeMillis()
        val currentBytes = downloadInfo.currentBytes
        val sampleDelta = now - mSpeedSampleStart
        if (sampleDelta > 500L) {
            val sampleSpeed = ((currentBytes - mSpeedSampleBytes) * 1000) / sampleDelta
            if (mSpeed == 0L) {
                mSpeed = sampleSpeed
            } else {
                mSpeed = ((mSpeed * 3) + sampleSpeed) / 4
            }
            if (mSpeedSampleStart != 0L) {
                notifySpeed(TaskState(downloadInfo.id, mSpeed, currentBytes, downloadInfo.totalBytes))
            }
            mSpeedSampleStart = now
            mSpeedSampleBytes = currentBytes
        }
        val bytesDelta = currentBytes - mLastUpdateBytes
        val timeDelta = now - mLastUpdateTime
        if (bytesDelta > MIN_PROGRESS_STEP && timeDelta > MIN_PROGRESS_TIME) {
            mLastUpdateBytes = currentBytes
            mLastUpdateTime = now
            //Log.e(TAG, "updateProgress ${downloadInfo.id}")
            downloadInfo.writeDatabase()

        }
    }

    companion object {
        private const val DEFAULT_TIME_OUT = 2000 * 1000
        private const val MIN_PROGRESS_STEP = 65536
        private const val MIN_PROGRESS_TIME = 2000L
        private const val TAG = "Downloader"
        fun getRemainingMillis(total: Long, current: Long, speed: Long): Long {
            return ((total - current) * 1000) / speed;
        }
    }
}