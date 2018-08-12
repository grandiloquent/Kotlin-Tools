package psycho.euphoria.download

import android.util.Log


class Request(
        val id: Long,
        var uri: String,
        val fileName: String,
        var etag: String?,
        var currentBytes: Long,
        var totalBytes: Long,
        var failedCount: Int,
        var finish: Int) : Comparable<Request> {

    private var mCanceled = false

    var mLock = java.lang.Object()
    var sequence = 0
    var requestQueue: RequestQueue? = null
    var tag: Any? = null
    var requestCompleteListener: RequestCompleteListener? = null


    override fun compareTo(other: Request): Int {
        val left = this.getPriority()
        val right = other.getPriority()

        // High-priority requests are "lesser" so they are sorted to the front.
        // Equal priorities are sorted by sequence number to provide FIFO ordering.
        return if (left === right) this.sequence - other.sequence else right.ordinal - left.ordinal
    }

    fun getPriority(): Priority {
        return Priority.NORMAL
    }

    fun cancel() {
        synchronized(mLock) {
            mCanceled = true
        }
    }

    fun finished(reason: String) {
    }

    fun isCanceled(): Boolean {
        synchronized(mLock) {
            return mCanceled
        }
    }

    fun notifyNoUsable() {
        var listener: RequestCompleteListener? = null
        synchronized(mLock) {
            listener = requestCompleteListener
        }
        listener?.onNoUsableReceived(this)
    }

    fun notifySpeed(speed: Long) {
        var listener: RequestCompleteListener? = null
        synchronized(mLock) {
            listener = requestCompleteListener
        }
        // Log.e(TAG, "notifySpeed $speed")
        listener?.onNotifySpeed(TaskState(id, speed, currentBytes, totalBytes))
    }

    fun notifyCompleted() {
        var listener: RequestCompleteListener? = null
        synchronized(mLock) {
            listener = requestCompleteListener
        }
        listener?.onNotifyCompleted(id)
    }


    fun writeDatabase() {
        DownloadTaskProvider.getInstance().update(this)
    }

    companion object {
        private const val TAG = "Request"
    }

    interface RequestCompleteListener {

        fun onNoUsableReceived(request: Request)
        fun onNotifySpeed(taskState: TaskState)
        fun onNotifyCompleted(id: Long)
    }

    enum class Priority {
        LOW,
        NORMAL,
        HIGH,
        IMMEDIATE
    }
}