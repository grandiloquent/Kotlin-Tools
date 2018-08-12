package psycho.euphoria.common.download

import android.text.format.DateUtils.SECOND_IN_MILLIS
import psycho.euphoria.common.extension.*
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class Network {

    private var mSpeedSampleStart = 0L
    private var mSpeedSampleBytes = 0L
    private var mSpeed = 0L
    private var mLastUpdateBytes = 0L
    private var mLastUpdateTime = 0L

    private fun addRequest(httpURLConnection: HttpURLConnection?, request: Request) {
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
        httpURLConnection?.let {
            for (header in it.headerFields) {
                println("${header.key} ${header.value}")
            }
            request.etag = it.eTag
            if (it.transferEncoding == null) {
                request.totalBytes += (httpURLConnection.contentLength_.toLongOrNull() ?: 0L)
            }
        }
    }

    private fun updateProgress(request: Request) {
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
            //Log.e(TAG, "updateProgress ${downloadInfo.id}")
            request.writeDatabase()

        }
    }

    fun performRequest(request: Request) {

        val httpURLConnection: HttpURLConnection
        try {
            httpURLConnection = URL(request.uri).openConnection() as HttpURLConnection
            httpURLConnection.instanceFollowRedirects = false
            httpURLConnection.connectTimeout = DEFAULT_TIMEOUT
            httpURLConnection.readTimeout = DEFAULT_TIMEOUT

        } catch (e: Exception) {

        }

    }

    companion object {
        private const val MIN_PROGRESS_STEP = 65536
        private const val MIN_PROGRESS_TIME = 2000L
        val DEFAULT_TIMEOUT = (20 * SECOND_IN_MILLIS).toInt()
    }
}