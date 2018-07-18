package psycho.euphoria.tools.downloads

import android.app.AlarmManager
import android.app.Service
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.*
import android.text.format.DateUtils.MINUTE_IN_MILLIS
import android.util.Log
import java.io.File
import java.util.*

class DownloadService() : Service() {

    private val mDownloads = HashMap<Long, DownloadInfo>()
    private val mExecutor = getExecutor()
    private var mUpdateThread: HandlerThread? = null
    private var mUpdateHandler: Handler? = null
    private var mAlarmManager: AlarmManager? = null
    @Volatile
    private var mLastStartId: Int = 0
    private val mUpdateCallback = Handler.Callback { msg ->
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
        val startId = msg.arg1
        val isActive: Boolean
        synchronized(mDownloads) {
        }
        true
    }

    override fun onCreate() {
        super.onCreate()
        mAlarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        mUpdateThread = HandlerThread(TAG + "-UpdateThread").apply { start() }
        mUpdateHandler = Handler(mUpdateThread?.looper, mUpdateCallback)
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            val jobService = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler

            if (needToScheduleCleanup(jobService)) {
                val job = JobInfo
                        .Builder(CLEANUP_JOB_ID, sCleanupServiceName)
                        .setPeriodic(CLEANUP_JOB_PERIOD)
                        .setRequiresCharging(true)
                        .setRequiresDeviceIdle(true)
                        .build();
                jobService.schedule(job)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val returnValue = super.onStartCommand(intent, flags, startId)
        mLastStartId = startId
        enqueueUpdate()
        return returnValue
    }

    fun enqueueUpdate() {

        val ls = DownloadDatabase.getInstance(this).list()

        for (i in ls) {
            mExecutor.execute(Downloader(
                    DownloadInfo(
                            i.first,
                            i.second,
                            generateFileNameFromURL(i.second, Environment.getExternalStorageDirectory())
                    )
            ))
        }

//        if (mUpdateHandler != null) {
//            mUpdateHandler?.run {
//                removeMessages(MSG_UPDATE)
//                obtainMessage(MSG_UPDATE, mLastStartId, -1).sendToTarget()
//            }
//
//        }
    }


    fun needToScheduleCleanup(jobScheduler: JobScheduler): Boolean {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            val jobs = jobScheduler.allPendingJobs
            if (jobs != null) {
                val length = jobs.size
                for (i in 0 until length) {
                    if (jobs[i].id == CLEANUP_JOB_ID) {
                        return false
                    }
                }
            }
            return true
        } else return false
    }

    override fun onDestroy() {
        mUpdateThread?.quit()

        super.onDestroy()

    }

    private fun enqueueFinalUpdate() {
        mUpdateHandler?.run {
            removeMessages(MSG_FINAL_UPDATE)
            sendMessageDelayed(
                    obtainMessage(MSG_FINAL_UPDATE, mLastStartId, -1),
                    5 * MINUTE_IN_MILLIS
            )
        }
    }

    override fun onBind(i: Intent?): IBinder {
        throw  UnsupportedOperationException("Cannot bind to Download Manager Service");
    }

    fun deleteFileExists(path: String) {
        if (path.isNotBlank()) {
            val file = File(path)

            if (file.exists() && !file.delete()) {
                Log.w(TAG, "file: '$path' couldn't be deleted")
            }
        }
    }

    private fun updateDownload(downloadInfo: DownloadInfo) {

    }

    private fun insertDownloadLocked(downloadInfo: DownloadInfo) {
        mDownloads.put(downloadInfo.id, downloadInfo)
    }

    private fun deleteDownloadLocked(id: Long) {
        //  val downloadInfo = mDownloads.get(id)

    }

    companion object {
        private const val MSG_UPDATE = 1
        private const val MSG_FINAL_UPDATE = 2
        private const val TAG = "DownloadService"
        private const val CLEANUP_JOB_ID = 1
        private const val CLEANUP_JOB_PERIOD = (1000 * 60 * 60 * 24).toLong()
        private val sCleanupServiceName = ComponentName(
                DownloadService::class.java.getPackage().name,
                DownloadService::class.java.name)

    }
}