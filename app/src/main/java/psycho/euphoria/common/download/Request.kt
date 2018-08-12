package psycho.euphoria.common.download

class Request(
        val id: Long,
        var uri: String,
        val fileName: String,
        var etag: String?,
        var currentBytes: Long,
        var totalBytes: Long,
        var failedCount: Int,
        var finish: Int) {

    private var mCanceled = false

    var mLock = java.lang.Object()
    var sequence = 0
    var requestQueue: RequestQueue? = null
    var tag: Any? = null
    var requestCompleteListener: RequestCompleteListener? = null


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
        listener?.onNotifySpeed(TaskState(id, speed, currentBytes, totalBytes))
    }

    fun notifyCompleted() {
        var listener: RequestCompleteListener? = null
        synchronized(mLock) {
            listener = requestCompleteListener
        }
        listener?.onNotifyCompleted()
    }


    fun writeDatabase() {
        DownloadTaskProvider.getInstance().update(this)
    }

    interface RequestCompleteListener {

        fun onNoUsableReceived(request: Request)
        fun onNotifySpeed(taskState: TaskState)
        fun onNotifyCompleted()
    }

}