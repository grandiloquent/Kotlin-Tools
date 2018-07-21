package psycho.euphoria.tools.commons

import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.log

object Tracker {

    private const val ENABLE = true
    private const val TAG = "Tracker"
    fun e(category: String, message: String = "fired") {
        if (ENABLE)
            Log.e(TAG, "[$category]: $message")
    }
}

class Logger {

    private val outputStream: FileOutputStream
    var isEnable = true

    init {
        val loggerFile = File(Environment.getExternalStorageDirectory(), "logger.txt");
        outputStream = FileOutputStream(loggerFile, true)
    }

    private fun currentDateTimeString(): String {
        return SimpleDateFormat("yyyy-MM-dd:hh:mm:ss").format(Calendar.getInstance().time)
    }

    private fun log(message: String, level: String = " [ERROR]") {
        if (!isEnable) return
        outputStream.write(currentDateTimeString().toByteArray());
        outputStream.write("$level: ".toByteArray())
        outputStream.write(message.toByteArray())
        outputStream.write('\n'.toInt())
    }

    fun e(message: String) {
        log(message)
    }

    fun d(message: String) {
        log(message, " [DEBUG]")
    }

    fun i(message: String) {
        log(message, " [INFO]")
    }

    fun close() {
        try {
            outputStream.close()
        } catch (ignored: Exception) {

        }
    }

    companion object {
        private var instance: Logger? = null

        fun getInstance(): Logger {
            return instance ?: synchronized(this) {
                Logger().also {
                    instance = it
                }
            }
        }
    }
}

