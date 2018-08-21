package psycho.euphoria.download

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import psycho.euphoria.common.*
import psycho.euphoria.common.Services.notificationManager
import java.util.*

class DownloadService : Service() {
    private val mLock = java.lang.Object()
    private val mActiveNotifies = HashMap<String, Long>()
    private val mStringBuilder = StringBuilder(8)
    private var mUpdateThread: HandlerThread? = null
    private var mUpdateHandler: Handler? = null
    private var mRequestQueue: RequestQueue? = null
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        mUpdateThread = HandlerThread("${TAG}-Thread")
        mUpdateThread?.start()
        mUpdateHandler = Handler(mUpdateThread?.looper, Handler.Callback {
            when (it.what) {
                MSG_UPDATE_NOTIFICATION -> {
                    val taskState = it.obj as TaskState
                    makeNotification(taskState.id, taskState.speed, taskState.current, taskState.total, taskState.title)
                }
                MSG_COMPLETE_NOTIFICATION -> {
                    synchronized(mLock) {
                        Services.notificationManager.cancel("${it.arg1}", 0)
                    }
                }
                MSG_OCCURRED_ERROR -> {
                    Log.i(TAG, "[onCreate]:${it} ")
                    if (it.arg1 == Network.TYPE_NO_NETWORK) {

                        Services.toast("The device is not connected to the network.")
                    } else {
                        Services.toast("An error occurred during the download")
                    }
                }
            }
            /**
             * @param msg A {@link android.os.Message Message} object
             * @return True if no further handling is desired
             */
            true
        })
        Services.context = this.applicationContext
        createNotificationChannel(CHANNEL_ACTIVE, "In progress")
    }

    private fun startDownload() {
        if (mRequestQueue == null) {
            mRequestQueue = RequestQueue(DEFAULT_THREAD_POOL_SIZE).also { it.start() }
        }
        val tasks = DownloadTaskProvider.getInstance().listTasks()
        Log.e(TAG, "startDownload ${tasks.size}")
        for (task in tasks) {
            task.requestCompleteListener = object : Request.RequestCompleteListener {
                override fun onNotifyError(type: Int) {
                    mUpdateHandler?.send(MSG_OCCURRED_ERROR, null, type)
                }

                override fun onNoUsableReceived(request: Request) {
                }

                override fun onNotifySpeed(taskState: TaskState) {
                    mUpdateHandler?.send(MSG_UPDATE_NOTIFICATION, taskState)

                }

                override fun onNotifyCompleted(id: Long) {
                    mUpdateHandler?.send(MSG_COMPLETE_NOTIFICATION, null, id.toInt())
                }
            }
            mRequestQueue?.add(task)
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        /**
         * Forces the thread to stop executing.
         * <p>
         * If there is a security manager installed, its <code>checkAccess</code>
         * method is called with <code>this</code>
         * as its argument. This may result in a
         * <code>SecurityException</code> being raised (in the current thread).
         * <p>
         * If this thread is different from the current thread (that is, the current
         * thread is trying to stop a thread other than itself), the
         * security manager's <code>checkPermission</code> method (with a
         * <code>RuntimePermission("stopThread")</code> argument) is called in
         * addition.
         * Again, this may result in throwing a
         * <code>SecurityException</code> (in the current thread).
         * <p>
         * The thread represented by this thread is forced to stop whatever
         * it is doing abnormally and to throw a newly created
         * <code>ThreadDeath</code> object as an exception.
         * <p>
         * It is permitted to stop a thread that has not yet been started.
         * If the thread is eventually started, it immediately terminates.
         * <p>
         * An application should not normally try to catch
         * <code>ThreadDeath</code> unless it must do some extraordinary
         * cleanup operation (note that the throwing of
         * <code>ThreadDeath</code> causes <code>finally</code> clauses of
         * <code>try</code> statements to be executed before the thread
         * officially dies).  If a <code>catch</code> clause catches a
         * <code>ThreadDeath</code> object, it is important to rethrow the
         * object so that the thread actually dies.
         * <p>
         * The top-level error handler that reacts to otherwise uncaught
         * exceptions does not print out a message or otherwise notify the
         * application if the uncaught exception is an instance of
         * <code>ThreadDeath</code>.
         *
         * @exception  SecurityException  if the current thread cannot
         *               modify this thread.
         * @see        #interrupt()
         * @see        #checkAccess()
         * @see        #run()
         * @see        #start()
         * @see        ThreadDeath
         * @see        ThreadGroup#uncaughtException(Thread,Throwable)
         * @see        SecurityManager#checkAccess(Thread)
         * @see        SecurityManager#checkPermission
         * @deprecated This method is inherently unsafe.  Stopping a thread with
         *       Thread.stop causes it to unlock all of the monitors that it
         *       has locked (as a natural consequence of the unchecked
         *       <code>ThreadDeath</code> exception propagating up the stack).  If
         *       any of the objects previously protected by these monitors were in
         *       an inconsistent state, the damaged objects become visible to
         *       other threads, potentially resulting in arbitrary behavior.  Many
         *       uses of <code>stop</code> should be replaced by code that simply
         *       modifies some variable to indicate that the target thread should
         *       stop running.  The target thread should check this variable
         *       regularly, and return from its run method in an orderly fashion
         *       if the variable indicates that it is to stop running.  If the
         *       target thread waits for long periods (on a condition variable,
         *       for example), the <code>interrupt</code> method should be used to
         *       interrupt the wait.
         *       For more information, see
         *       <a href="{@docRoot}openjdk-redirect.html?v=8&path=/technotes/guides/concurrency/threadPrimitiveDeprecation.html">Why
         *       are Thread.stop, Thread.suspend and Thread.resume Deprecated?</a>.
         */
        try {
            mUpdateThread?.interrupt()
        } catch (e: Exception) {
            Log.e(TAG, "onDestroy", e)
        }
    }

    private fun addNewTask(id: Long) {
        if (mRequestQueue == null) {
            mRequestQueue = RequestQueue(DEFAULT_THREAD_POOL_SIZE).also { it.start() }
        }
        DownloadTaskProvider.getInstance().fecthTask(id)?.let {
            it.requestCompleteListener = object : Request.RequestCompleteListener {
            override fun onNotifyError(type: Int) {
                mUpdateHandler?.send(MSG_OCCURRED_ERROR, null, type)
            }

            override fun onNoUsableReceived(request: Request) {
            }

            override fun onNotifySpeed(taskState: TaskState) {
                mUpdateHandler?.send(MSG_UPDATE_NOTIFICATION, taskState)

            }

            override fun onNotifyCompleted(id: Long) {
                mUpdateHandler?.send(MSG_COMPLETE_NOTIFICATION, null, id.toInt())
            }
        }
            mRequestQueue?.add(it)
        }
    }

    private fun makeNotification(id: Long,
                                 speed: Long,
                                 current: Long,
                                 total: Long,
                                 title: String?) {
        val tag = "$id"
        val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, CHANNEL_ACTIVE) else Notification.Builder(this)
        builder.setSmallIcon(android.R.drawable.stat_sys_download_done)
        var firstShow = 0L
        if (mActiveNotifies.containsKey(tag)) {
            firstShow = mActiveNotifies.get(tag) ?: 0L
        } else {
            firstShow = System.currentTimeMillis()
            mActiveNotifies.put(tag, firstShow)
        }
        builder.setWhen(firstShow)
        builder.setOnlyAlertOnce(true)
        builder.setContentTitle(speed.formatSize())

        if (total > 0L) {
            builder.setProgress(100, ((current * 100) / total).toInt(), false)
        } else {
            builder.setProgress(100, 0, true)
        }
        builder.setContentText("${(getRemainingMillis(total, current, speed) / 1000).toInt().getFormattedDuration(mStringBuilder)} (${current.formatSize()}/${total.formatSize()})")
        mStringBuilder.setLength(0)
        addNotificationAction(builder, id)
        notificationManager.notify(tag, 0, builder.build())
    }

    private fun makeNotification() {
        val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, CHANNEL_ACTIVE) else Notification.Builder(this)
        builder.setSmallIcon(android.R.drawable.stat_sys_download_done)
        builder.setWhen(System.currentTimeMillis())
        builder.setOnlyAlertOnce(true)
        addNotificationAction(builder, 0L)
        notificationManager.notify("1", 0, builder.build())
    }

    private fun addNotificationAction(builder: Notification.Builder, id: Long) {
        val stopIntent = Intent(this, DownloadService::class.java)
        stopIntent.action = ACTION_STOP_TASK
        stopIntent.putExtra(EXTRA_ID, id)
        // REQUEST_CODE
        val stopPendingIntent = PendingIntent.getService(this, id.toInt(), stopIntent, PendingIntent.FLAG_ONE_SHOT)

        val title = if (Locale.getDefault() == Locale.CHINA) "停止" else "Stop"
        if (Build.VERSION.SDK_INT >= 23) {
            builder.addAction(Notification.Action.Builder(null, title, stopPendingIntent).build())
        } else {
            builder.addAction(android.R.drawable.stat_sys_download_done, title, stopPendingIntent)

        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        intent?.let {
            if (it.action?.equals(ACTION_STOP_TASK) == true) {
                val id = it.long(EXTRA_ID)
                Log.e(TAG, "[onStartCommand]: Stop Task => id ${id} ")
                mRequestQueue?.cancelAll(id)
                notificationManager.cancel("$id", 0)
            } else if (it.action?.equals(ACTION_ADD_NEW_TASK) == true) {
                val id = it.long(EXTRA_ID)
                addNewTask(id)
            } else {
                startDownload()
                Log.e(TAG, "[onStartCommand]: startDownload")
            }
        }
        return START_NOT_STICKY
    }

    companion object {
        private const val MSG_UPDATE_NOTIFICATION = 1
        private const val MSG_COMPLETE_NOTIFICATION = 2
        private const val MSG_OCCURRED_ERROR = 3

        const val ACTION_STOP_TASK = "psycho.euphoria.STOP_TASK"
        const val ACTION_ADD_NEW_TASK = "psycho.euphoria.ADD_NEW_TASK"
        const val EXTRA_ID = "id"
        private const val DEFAULT_THREAD_POOL_SIZE = 3
        private const val REQUEST_CODE = 100

        private const val CHANNEL_ACTIVE = "active"
        private const val TAG = "DownloadService"
        fun getRemainingMillis(total: Long, current: Long, speed: Long): Long {
            return ((total - current) * 1000) / speed;
        }
    }
}
