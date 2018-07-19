package psycho.euphoria.tools.downloads

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.SystemClock

class DownloadNotifier(private val context: Context) {


    private fun updateCompletedNotification(downloads: Collection<DownloadInfo>) {
        for (download in downloads) {
            val n = Notification()
            n.icon = android.R.drawable.stat_sys_download_done
            val id = download.id
            val title = download.fileName

        }
    }

    private fun getDownloadingText(totalBytes: Long, currentBytes: Long): String {
        return if (totalBytes <= 0L) ""
        else {
            val progress = currentBytes * 100 / totalBytes
            val sb = StringBuilder()
            sb.append(progress)
            sb.append('%')
            sb.toString()
        }
    }
}