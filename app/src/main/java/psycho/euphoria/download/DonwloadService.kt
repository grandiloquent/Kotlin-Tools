package psycho.euphoria.download

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import psycho.euphoria.common.extension.Services
import psycho.euphoria.common.extension.createNotificationChannel
import psycho.euphoria.common.extension.formatSize
import psycho.euphoria.common.extension.getFormattedDuration
import psycho.euphoria.tools.R

class DonwloadService : Service() {

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
        mUpdateHandler = Handler(mUpdateHandler?.looper, Handler.Callback {
            when (it.what) {
                MSG_UPDATE_NOTIFICATION -> {
                    val taskState = it.obj as TaskState
                    makeNotification(taskState.id, taskState.speed, taskState.current, taskState.total)
                }
                MSG_COMPLETE_NOTIFICATION -> {
                    Services.notificationManager.cancel(it.arg1)
                }
            }
            /**
             * @param msg A {@link android.os.Message Message} object
             * @return True if no further handling is desired
             */
            true
        })
        Services.context = this.applicationContext

        createNotificationChannel(CHANNEL_ACTIVE, resources.getString(R.string.download_running))
        mRequestQueue = RequestQueue(Network(), 3)
    }

    private fun startDownload() {
        val tasks = ArrayList<Request>()
        tasks.forEach { mRequestQueue?.add(it) }
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
        mUpdateThread?.stop()
    }

    private fun makeNotification(id: Long,
                                 speed: Long,
                                 current: Long,
                                 total: Long) {
        val tag = "$id"
        val builder = NotificationCompat.Builder(this, CHANNEL_ACTIVE)
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
        builder.setContentText(speed.formatSize())

        if (total > 0L) {
            builder.setProgress(100, ((current * 100) / total).toInt(), false)
        } else {
            builder.setProgress(100, 0, true)
        }
        builder.setContentText("${(getRemainingMillis(total, current, speed) / 1000).toInt().getFormattedDuration(mStringBuilder)} (${current.formatSize()}/${total.formatSize()})")

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        startDownload()
        /**
         * Constant to return from {@link #onStartCommand}: if this service's
         * process is killed while it is started (after returning from
         * {@link #onStartCommand}), then leave it in the started state but
         * don't retain this delivered intent.  Later the system will try to
         * re-create the service.  Because it is in the started state, it will
         * guarantee to call {@link #onStartCommand} after creating the new
         * service instance; if there are not any pending start commands to be
         * delivered to the service, it will be called with a null intent
         * object, so you must take care to check for this.
         *
         * <p>This mode makes sense for things that will be explicitly started
         * and stopped to run for arbitrary periods of time, such as a service
         * performing background music playback.
         */
        return START_STICKY
    }

    companion object {
        private const val MSG_UPDATE_NOTIFICATION = 1
        private const val MSG_COMPLETE_NOTIFICATION = 2
        private const val CHANNEL_ACTIVE = "active"
        private const val TAG = "DonwloadService"
        fun getRemainingMillis(total: Long, current: Long, speed: Long): Long {
            return ((total - current) * 1000) / speed;
        }
    }

}