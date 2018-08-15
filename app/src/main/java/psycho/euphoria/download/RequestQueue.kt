package psycho.euphoria.download
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
class RequestQueue(private val threadPoolSize: Int) {
    private val mDispatchers: Array<NetworkDispatcher?> = arrayOfNulls(threadPoolSize)
    private val mNetworkQueue = PriorityBlockingQueue<Request>()
    private val mCurrentRequests = HashSet<Request>()
    private val mFinishedListeners = ArrayList<RequestFinishedListener>()
    private val mSequenceGenerator = AtomicInteger()
    fun start() {
        stop()
        for (i in 0 until threadPoolSize) {
            val networkDispatcher = NetworkDispatcher(mNetworkQueue, Network())
            mDispatchers[i] = networkDispatcher
            networkDispatcher.start()
        }
    }
    fun finish(request: Request) {
        synchronized(mCurrentRequests) {
            mCurrentRequests.remove(request)
        }
        synchronized(mFinishedListeners) {
            mFinishedListeners.forEach { it.onRequestFinished(request) }
        }
    }
    private fun getSequenceNumber(): Int {
        return mSequenceGenerator.incrementAndGet()
    }
    fun addRequestFinishedListener(listener: RequestFinishedListener) {
        synchronized(mFinishedListeners) {
            mFinishedListeners.add(listener)
        }
    }
    fun removeRequestFinishedListener(listener: RequestFinishedListener) {
        synchronized(mFinishedListeners) {
            mFinishedListeners.remove(listener)
        }
    }
    fun stop() {
        mDispatchers.forEach { it?.quit() }
    }
    fun add(request: Request): Request {
        request.requestQueue = this
        synchronized(mCurrentRequests) {
            mCurrentRequests.add(request)
        }
        request.sequence = getSequenceNumber()
        mNetworkQueue.add(request)
        return request
    }
    fun cancelAll(filter: RequestFilter) {
        synchronized(mCurrentRequests) {
            for (request in mCurrentRequests) {
                if (filter.apply(request)) {
                    request.cancel()
                }
            }
        }
    }
    fun cancelAll(tag: Any?) {
        if (tag == null) {
            throw IllegalArgumentException("Cannot cancelAll with a null tag")
        }
        cancelAll(
                object : RequestFilter {
                    override fun apply(request: Request): Boolean {
                        return request.tag == tag
                    }
                })
    }
    interface RequestFilter {
        fun apply(request: Request): Boolean
    }
    interface RequestFinishedListener {
        fun onRequestFinished(request: Request)
    }
}