package psycho.euphoria.tools.downloads

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.*
import android.support.v4.app.NotificationCompat
import psycho.euphoria.tools.R
import psycho.euphoria.tools.commons.Tracker
import psycho.euphoria.tools.commons.formatSize
import psycho.euphoria.tools.commons.notificationManager
import psycho.euphoria.tools.commons.round

class DownloadService() : Service() {

    private lateinit var mNotificationBuilder: NotificationCompat.Builder
    private lateinit var mNotificationManager: NotificationManager
    private lateinit var mUpdateHandler: Handler
    private lateinit var mUpdateThread: HandlerThread
    private lateinit var mWorkThread: HandlerThread
    private var mAlarmManager: AlarmManager? = null
    private var mCurrentDownloadInfo: DownloadInfo? = null
    private var mWorkHandler: Handler? = null

    private val mUpdateCallback = Handler.Callback { msg ->
        when (msg.what) {

            MSG_UPDATE -> updateNotify(msg.obj as Long)
            MSG_COMPLETE -> {


                downStart()
            }
            MSG_ERROR -> downStart()

        }
        true
    }

    override fun onCreate() {
        Tracker.e("onCreate")
        super.onCreate()

        mAlarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        mNotificationManager = notificationManager

        mUpdateThread = HandlerThread(TAG + "-UpdateThread");
        mUpdateThread.start()
        mUpdateHandler = Handler(mUpdateThread.looper, mUpdateCallback)

        mWorkThread = HandlerThread(TAG + "-WorkThread")
        mWorkThread.start()
        mWorkHandler = Handler(mWorkThread.looper)

        startForeground(NOTIFICATION_ID, makeNotify())

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Tracker.e("onStartCommand")
        val returnValue = super.onStartCommand(intent, flags, startId)

        downStart()
        return returnValue
    }


    private fun createNotificationChannel(): String {
        if (Build.VERSION.SDK_INT >= 26) {
            // NotificationManager.IMPORTANCE_NONE Turn off the notifition sound
            mNotificationManager.createNotificationChannel(NotificationChannel(CHANNEL_ACTIVE,
                    TAG, NotificationManager.IMPORTANCE_NONE).also {
                it.lightColor = Color.BLUE
                it.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            })
        }
        return CHANNEL_ACTIVE
    }

    private fun downStart() {


        mCurrentDownloadInfo = fecthDownloadInfo()


        if (mCurrentDownloadInfo == null) {

            stopSelf()
        } else {
            val runnable = Downloader(mCurrentDownloadInfo!!).apply {
                notifyCompleted = fun(id) {
                    mUpdateHandler.sendMessage(mUpdateHandler.obtainMessage(MSG_COMPLETE, id))
                    Tracker.e("notifyCompleted")

                }
                notifyDownloadSpeed = fun(id, speed) {


                    mUpdateHandler.sendMessage(mUpdateHandler.obtainMessage(MSG_UPDATE, speed))
                }
                notifyErrorOccurred = fun(id, filename) {
                    Tracker.e("notifyErrorOccurred")
                    mUpdateHandler.sendMessage(mUpdateHandler.obtainMessage(MSG_ERROR))
                }
            }
            mWorkHandler?.post(runnable)
        }


    }

    private fun fecthDownloadInfo(): DownloadInfo? {
        val downloadInfo = DownloadDatabase.getInstance(this).listOne()
        if (downloadInfo != null && downloadInfo.fileName == null) {
            downloadInfo.fileName = generateFileNameFromURL(downloadInfo.url, Environment.getExternalStorageDirectory())
        }
        return downloadInfo
    }

    override fun onDestroy() {
        mUpdateThread?.quit()

        Tracker.e("onDestroy")
        super.onDestroy()

    }

    private fun makeNotify(): Notification {
        createNotificationChannel()
        val title = resources.getString(R.string.notifiaction_download_title)
        mNotificationBuilder = NotificationCompat.Builder(this, CHANNEL_ACTIVE)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle(title)
                .setContentText(title)
                .setWhen(System.currentTimeMillis())
        return mNotificationBuilder.build()
    }

    private fun updateNotify(speed: Long) {

        mCurrentDownloadInfo?.let {
            val totalBytes = it.totalBytes

            if (totalBytes == 0L) {
                Tracker.e("updateNotify", "totalBytes => $totalBytes fileName=${it.fileName}")
            }
            if (totalBytes > 0L) {

                mNotificationBuilder.setContentTitle(speed.formatSize())
                mNotificationBuilder.setProgress(100, (it.currentBytes * 100 / totalBytes).toInt(), false)
                mNotificationBuilder.setContentText("${it.currentBytes.formatSize()}/${totalBytes.formatSize()}(${(it.currentBytes * 100.0 / totalBytes).round()}%)");
            }


            mNotificationManager.notify(NOTIFICATION_ID, mNotificationBuilder.build())
        }
    }

    override fun onBind(i: Intent?): IBinder {
        throw  UnsupportedOperationException("Cannot bind to Download Manager Service");
    }


    companion object {
        private const val MSG_START = 0
        private const val MSG_UPDATE = 1
        private const val MSG_COMPLETE = 2
        private const val MSG_ERROR = 3
        private const val TAG = "DownloadService"
        private const val NOTIFICATION_ID = 151;
        private const val CHANNEL_ACTIVE = "active"


    }
}