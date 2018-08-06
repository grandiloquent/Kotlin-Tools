package psycho.euphoria.tools.downloads

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.util.ArrayMap
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import psycho.euphoria.tools.commons.notificationManager

class DownloadService : Service() {
    private val mChannel = Channel<TaskState>()
    private val mDispatcher = newFixedThreadPoolContext(3, "DownloadService")
    private lateinit var mNotificationManager: NotificationManager
    private val mActivityNotifies = ArrayMap<String, Long>()

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        mNotificationManager = notificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startDownload()
        return super.onStartCommand(intent, flags, startId)
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


    private fun makeNotify(id: Long,
                           speed: Long,
                           current: Long,
                           total: Long) {


        val tag = "$id"

        var builder = NotificationCompat.Builder(this, CHANNEL_ACTIVE)

        builder.setSmallIcon(android.R.drawable.stat_sys_download_done)


        var firstShow = 0L // The first time displayed in millisecond

        if (mActivityNotifies.containsKey(tag)) {
            firstShow = mActivityNotifies.get(tag) ?: 0L
        } else {
            firstShow = System.currentTimeMillis()
            mActivityNotifies.put(tag, firstShow)
        }
        builder.setWhen(firstShow)
        builder.setOnlyAlertOnce(true)

        builder.setContentTitle(speed.formatSize())

        if (total > 0L) {
            builder.setProgress(100, ((current * 100) / total).toInt(), false)
        } else {
            builder.setProgress(100, 0, true)
        }
        builder.setContentText("${(Downloader.getRemainingMillis(total, current, speed) / 1000).toInt().getFormattedDuration()} (${current.formatSize()}/${total.formatSize()})")
        mNotificationManager.notify(tag, 0, builder.build())
    }

    private fun startDownload() {

        val downTasks = DownloadTaskProvider.getInstance().listTasks()

        launch(mDispatcher) {
            for (task in downTasks) {
                Downloader { taskState ->
                    launch {
                        mChannel.send(taskState)
//                        if (speed != 0L) {
//                            val message = "${speed.formatSize()} ${totalBytes.formatSize()} ${(Downloader.getRemainingMillis(totalBytes, currentBytes, speed) / 1000).toInt().getFormattedDuration()}"
//                            mChannel.send(1L to message)
//                        }
                    }
                }.execute(task)
            }
        }

        var receive = suspend {
            while (!mChannel.isClosedForReceive) {
                val taskState = mChannel.receive()
                launch {
                    makeNotify(taskState.id, taskState.speed, taskState.current, taskState.total)
                }
            }
        }

        launch {
            receive()
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 151;
        private const val CHANNEL_ACTIVE = "active"
        private const val TAG = "DownloadService"
    }
}