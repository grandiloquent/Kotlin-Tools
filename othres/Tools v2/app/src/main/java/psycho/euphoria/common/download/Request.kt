package psycho.euphoria.common.download

class Request(
        var uri: String,
        private val requestCompleteListener: RequestCompleteListener) {

    private var mCanceled = false

    var mLock = java.lang.Object()
    var sequence = 0
    var requestQueue: RequestQueue? = null
    var tag: Any? = null
    var fileName: String? = null
    var currentBytes = 0L
    var totalBytes = 0L
    var etag: String? = null
    var id = 0L


    fun finished(reason: String) {

    }

    fun cancel() {
        synchronized(mLock) {
            mCanceled = true

        }
    }

    fun isCanceled(): Boolean {
        synchronized(mLock) {
            return mCanceled
        }
    }

    fun parseNetworkResponse(networkResponse: NetworkResponse) {

    }

    fun notifySpeed(speed: Long) {

    }

    fun notifyNoUsable() {
        var listener: RequestCompleteListener? = null

        synchronized(mLock) {
            listener = requestCompleteListener
        }
        listener?.onNoUsableReceived(this)
    }

    interface RequestCompleteListener {

        fun onReceived(request: Request, response: Response)
        fun onNoUsableReceived(request: Request)
    }

}