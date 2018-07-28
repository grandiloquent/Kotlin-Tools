package psycho.euphoria.tools.downloads

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.*
import android.support.v4.app.NotificationCompat
import psycho.euphoria.tools.R
import psycho.euphoria.tools.commons.*

class DownloadService() : Service() {

    private lateinit var mNotificationBuilder: NotificationCompat.Builder
    private lateinit var mNotificationManager: NotificationManager
    private lateinit var mUpdateHandler: Handler
    private lateinit var mUpdateThread: HandlerThread // Thread for update ui.If you use the same thread, the user interface will not be updated smoothly
    private lateinit var mWorkThread: HandlerThread // Thread for serialization download
    private var mAlarmManager: AlarmManager? = null
    private var mCurrentDownloadInfo: DownloadInfo? = null // Information about the download task currently being executed
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


    private fun createNotificationChannel(): String {
        if (Build.VERSION.SDK_INT >= 26) {
            // NotificationManager.IMPORTANCE_NONE Turn off the notification sound
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
            // If there is no download task, close the current service
            stopSelf()
            Logger.getInstance().d("Download service called the method: downStart,All tasks are downloaded.Stop the download service.")
        } else {
            val runnable = Downloader(mCurrentDownloadInfo!!).apply {
                notifyCompleted = fun(id) {
                    Logger.getInstance().d("Downloader called the method:notifyCompleted.")
                    mUpdateHandler.sendMessage(mUpdateHandler.obtainMessage(MSG_COMPLETE, id))
                }
                notifyDownloadSpeed = fun(id, speed) {

                    mUpdateHandler.sendMessage(mUpdateHandler.obtainMessage(MSG_UPDATE, speed))
                }
                notifyErrorOccurred = fun(id, filename) {
                    Logger.getInstance().e("Downloader called the method: notifyErrorOccured. $filename")
                    mUpdateHandler.sendMessage(mUpdateHandler.obtainMessage(MSG_ERROR))
                }
            }
            mWorkHandler?.post(runnable)
        }
    }

    private fun fecthDownloadInfo(): DownloadInfo? {
        // Query download tasks from database
        val downloadInfo = DownloadDatabase.getInstance(this).listOne()
        if (downloadInfo != null) {
            // Generate a suitable file name based on the download address.
            downloadInfo.fileName = generateFileNameFromURL(downloadInfo.url, Environment.getExternalStorageDirectory())
        }
        return downloadInfo
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

    override fun onBind(i: Intent?): IBinder {
        throw  UnsupportedOperationException("Cannot bind to Download Manager Service");
    }

    override fun onCreate() {
        Logger.getInstance().d("Download Service called method: onCreate()")
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

    override fun onDestroy() {
        mUpdateThread?.quit()
        Tracker.e("关闭")
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val returnValue = super.onStartCommand(intent, flags, startId)
        Logger.getInstance().d("Download Service called the method: onStartCommand,the return value = $returnValue")
        downStart()
        return returnValue
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