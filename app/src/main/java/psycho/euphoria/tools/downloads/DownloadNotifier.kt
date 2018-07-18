package psycho.euphoria.tools.downloads

import android.app.NotificationManager
import android.content.Context
import android.os.SystemClock

class DownloadNotifier(private val context: Context) {

    private val mNotificationManager: NotificationManager
    private val mDownloadSpeed = HashMap<Long, Long>()
    private val mDownloadTouch = HashMap<Long, Long>()


    init {
        mNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    }

    fun cancelAll() {
        mNotificationManager.cancelAll()
    }

    fun updateWith(downloads: Collection<DownloadInfo>) {

    }

    private fun updateWithLocked(downloads: Collection<DownloadInfo>) {

    }

    fun notifyDownloadSpeed(id: Long, bytesPerSecond: Long) {
        synchronized(mDownloadSpeed) {
            if (bytesPerSecond != 0L) {
                mDownloadSpeed.put(id, bytesPerSecond)
                mDownloadTouch.put(id, SystemClock.elapsedRealtime())
            } else {
                mDownloadSpeed.remove(id)
                mDownloadTouch.remove(id)
            }
        }
    }
}