package psycho.euphoria.tools.downloads

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.*
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationCompat.PRIORITY_MIN
import android.util.Log
import android.widget.Toast
import psycho.euphoria.tools.commons.formatSize
import psycho.euphoria.tools.commons.postNotification
import java.io.File
import java.util.*

class DownloadService() : Service() {

    private val mDownloads = HashMap<Long, DownloadInfo>()

    private var mUpdateThread: HandlerThread? = null
    private var mUpdateHandler: Handler? = null
    private var mWorkThread: HandlerThread? = null
    private var mWorkHandler: Handler? = null
    private var mAlarmManager: AlarmManager? = null
    @Volatile
    private var mLastStartId: Int = 0

    private var mNotificationBuilder: NotificationCompat.Builder? = null
    private var mNotificationManager: NotificationManager? = null

    private val mUpdateCallback = Handler.Callback { msg ->
        when (msg.what) {

            MSG_UPDATE -> {
                val speed = (msg.obj as String)

                Log.e(TAG, "[MSG_UPDATE]:$speed")

                mNotificationBuilder?.run {

                    setContentText(speed)
                    mNotificationManager?.notify(NOTIFICATION_ID, build())
                }


            }
            MSG_COMPLETE -> {

                DownloadDatabase.getInstance(this).update((msg.obj as Long))
                downStart()
            }

        }
        true
    }

    override fun onCreate() {
        super.onCreate()
        mAlarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mUpdateThread = HandlerThread(TAG + "-UpdateThread");
        mUpdateThread?.start()
        mUpdateHandler = Handler(mUpdateThread?.looper, mUpdateCallback)

        mWorkThread = HandlerThread(TAG + "-WorkThread")
        mWorkThread?.start()
        mWorkHandler = Handler(mWorkThread?.looper)

        startForeground()

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val returnValue = super.onStartCommand(intent, flags, startId)
        mLastStartId = startId
        downStart()
        return returnValue
    }

    private fun startForeground() {
        val channelId =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    createNotificationChannel()
                } else {
                    // If earlier version channel ID is not used
                    // https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#NotificationCompat.Builder(android.content.Context)
                    ""
                }

        mNotificationBuilder = NotificationCompat.Builder(this, channelId)
        val notification = mNotificationBuilder!!.setOngoing(true)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setPriority(PRIORITY_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(): String {
        val channelId = "my_service"
        val channelName = TAG
        val chan = NotificationChannel(channelId,
                channelName, NotificationManager.IMPORTANCE_NONE)
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    fun downStart() {
        val i = DownloadDatabase.getInstance(this).listOne();
        if (i != null) {
            var filename = generateFileNameFromURL(i.second, Environment.getExternalStorageDirectory())
            val runnable = Downloader(
                    DownloadInfo(
                            i.first,
                            i.second,
                            filename,
                            currentBytes = if (File(filename).exists()) File(filename).length() else 0L
                    )
            ).apply {
                notifyCompleted = fun(id, filename) {
                    mUpdateHandler?.sendMessage(mUpdateHandler?.obtainMessage(MSG_COMPLETE, id))

                }
                notifyDownloadSpeed = fun(id, speed, totalByts) {
                    mUpdateHandler?.sendMessage(mUpdateHandler?.obtainMessage(MSG_UPDATE, " ${speed.formatSize()} ${totalByts}%"))
                }
                notifyErrorOccurred = fun(id, filename) {
                    mUpdateHandler?.sendMessage(mUpdateHandler?.obtainMessage(MSG_ERROR))
                }
            }
            mWorkHandler?.post(runnable)
        } else {
            Toast.makeText(this, "No", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        mUpdateThread?.quit()

        super.onDestroy()

    }


    override fun onBind(i: Intent?): IBinder {
        throw  UnsupportedOperationException("Cannot bind to Download Manager Service");
    }


    companion object {
        private const val MSG_UPDATE = 1
        private const val MSG_COMPLETE = 2
        private const val MSG_ERROR = 3
        private const val TAG = "DownloadService"
        private const val NOTIFICATION_ID = 151;

    }
}