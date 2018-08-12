package psycho.euphoria.download

import android.text.format.DateUtils.SECOND_IN_MILLIS
import android.util.Log
import psycho.euphoria.common.extension.*
import psycho.euphoria.common.extension.HttpStatus.HTTP_MOVED_PERM
import psycho.euphoria.common.extension.HttpStatus.HTTP_MOVED_TEMP
import psycho.euphoria.common.extension.HttpStatus.HTTP_OK
import psycho.euphoria.common.extension.HttpStatus.HTTP_PARTIAL
import psycho.euphoria.common.extension.HttpStatus.HTTP_REQUESTED_RANGE_NOT_SATISFIABLE
import psycho.euphoria.common.extension.HttpStatus.HTTP_SEE_OTHER
import psycho.euphoria.common.extension.HttpStatus.HTTP_TEMP_REDIRECT
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

class Network {
    private var mSpeedSampleStart = 0L
    private var mSpeedSampleBytes = 0L
    private var mSpeed = 0L
    private var mLastUpdateBytes = 0L
    private var mLastUpdateTime = 0L
    private fun transferData(httpURLConnection: HttpURLConnection?, request: Request) {
        Log.e(TAG, "transferData ${request.id}")
        if (request.currentBytes >= request.totalBytes) {
            Log.e(TAG, "[ERROR]: request => ${request} \n")
            return
        }
        val inputStream = httpURLConnection?.inputStream
        val outputStream = RandomAccessFile(request.fileName, "rwd")
        if (request.currentBytes > 0L)
            outputStream.seek(request.currentBytes)
        inputStream?.use { input ->
            outputStream.use { output ->
                val buffer = ByteArray(8 * 1024)
                var bytes = input.read(buffer)
                while (bytes >= 0) {
                    output.write(buffer, 0, bytes)
                    request.currentBytes += bytes

                    updateProgress(request)
                    bytes = input.read(buffer)
                    if (request.currentBytes > request.totalBytes) {
                        Log.e(TAG, "[ERROR]: currentBytes>totalBytes")
                    }
                }
            }
        }
    }

    private fun addRequest(httpURLConnection: HttpURLConnection?, request: Request) {
        Log.e(TAG, "[addRequest] ${request.id}")
        httpURLConnection?.let {
            val file = File(request.fileName)
            if (file.exists()) {
                request.currentBytes = file.length()
                request.totalBytes += request.currentBytes
                request.etag?.apply {
                    it.ifMatch = this
                }
                it.range = "${request.currentBytes}"
            }
            it.acceptEncoding = "identity"
            it.connection = "close"
        }
    }

    private fun parseHeaders(httpURLConnection: HttpURLConnection?, request: Request) {
        Log.e(TAG, "[parseHeaders]")
        httpURLConnection?.let {
            for (header in it.headerFields) {
                println("${header.key} ${header.value}")
            }
            request.etag = it.eTag
            if (it.transferEncoding == null) {
                request.totalBytes += (httpURLConnection.contentLength_.toLongOrNull()
                        ?: 0L)
            }
        }
    }

    private fun updateProgress(request: Request) {
        //Log.e(TAG,"[updateProgress]")
        val now = System.currentTimeMillis()
        val currentBytes = request.currentBytes
        val sampleDelta = now - mSpeedSampleStart
        if (sampleDelta > 500L) {
            val sampleSpeed = ((currentBytes - mSpeedSampleBytes) * 1000) / sampleDelta
            if (mSpeed == 0L) {
                mSpeed = sampleSpeed
            } else {
                mSpeed = ((mSpeed * 3) + sampleSpeed) / 4
            }
            if (mSpeedSampleStart != 0L) {
                request.notifySpeed(mSpeed)
            }
            mSpeedSampleStart = now
            mSpeedSampleBytes = currentBytes
        }
        val bytesDelta = currentBytes - mLastUpdateBytes
        val timeDelta = now - mLastUpdateTime
        if (bytesDelta > MIN_PROGRESS_STEP && timeDelta > MIN_PROGRESS_TIME) {
            mLastUpdateBytes = currentBytes
            mLastUpdateTime = now
            //Log.e(TAG, "updateProgress ${request.id}")
            request.writeDatabase()
        }
    }

    fun performRequest(request: Request) {
        Log.e(TAG, "[performRequest]")
        var url = URL(request.uri)
        var httpURLConnection: HttpURLConnection? = null
        try {
            httpURLConnection = (url.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = DEFAULT_TIMEOUT
                readTimeout = DEFAULT_TIMEOUT
            }
            addRequest(httpURLConnection, request)
            var responseCode = httpURLConnection.responseCode
            branch@ when (responseCode) {
                HTTP_OK -> {
                    parseHeaders(httpURLConnection, request)
                    transferData(httpURLConnection, request)
                    // Download task has been completed, update the database
                    request.finish = 1
                    request.writeDatabase()
                    request.notifyCompleted()
                }
                HTTP_PARTIAL -> {
//                    if (request.totalBytes <= 0L)// The cached information may be lost and re-parsed
                    parseHeaders(httpURLConnection, request)
                    transferData(httpURLConnection, request)
                    // Download task has been completed, update the database
                    request.finish = 1
                    request.writeDatabase()
                    request.notifyCompleted()
                }
                HTTP_MOVED_PERM,
                HTTP_MOVED_TEMP,
                HTTP_SEE_OTHER,
                HTTP_TEMP_REDIRECT -> {
                    val location = httpURLConnection.location
                    url = URL(url, location)
                    if (responseCode == HTTP_MOVED_PERM) {
                        request.uri = url.toString()
                    }
                    return@branch
                }
                HTTP_REQUESTED_RANGE_NOT_SATISFIABLE -> {
                    request.finish = 1
                    request.writeDatabase()
                    request.notifyCompleted()
                }
                else -> throw Exception("Response code $responseCode")
            }
        } catch (malformedURLException: MalformedURLException) {
            // The resource address is incorrect, the task cannot be restarted, so it is marked as completed
            request.finish = 1
            request.writeDatabase()
            request.notifyCompleted()
            Log.e(TAG, "performRequest", malformedURLException)
        } catch (ignored: Exception) {
            request.failedCount += 1
            request.writeDatabase()
            Log.e(TAG, "performRequest", ignored)
        } finally {
            httpURLConnection?.disconnect()
        }
    }

    companion object {
        private const val MIN_PROGRESS_STEP = 65536
        private const val MIN_PROGRESS_TIME = 2000L
        val DEFAULT_TIMEOUT = (20 * SECOND_IN_MILLIS).toInt()
        private const val TAG = "Network"
    }
}