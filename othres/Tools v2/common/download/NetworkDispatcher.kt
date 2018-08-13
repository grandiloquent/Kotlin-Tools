package psycho.euphoria.common.download

import android.os.Process
import java.util.concurrent.BlockingQueue

class NetworkDispatcher(private val queue: BlockingQueue<Request>,
                        private val network: Network) : Thread() {

    override fun run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
        while (true) {

        }
    }

    private fun processRequest() {
        val request = queue.take()
        try {
            if (request.isCanceled()) {
                request.finished("network_discard-cancelled")
                request.notifyNoUsable()
                return
            }
           network.performRequest(request)

            //if (networkResponse.notModified) {
            //    return
            //}

        } catch (e: Exception) {

        }
    }

    fun quit() {
        interrupt()
    }
}
