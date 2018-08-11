package psycho.euphoria.common.download.core.requests

import psycho.euphoria.common.download.core.FuelError
import psycho.euphoria.common.download.core.Request
import psycho.euphoria.common.download.core.Response
import java.io.InterruptedIOException
import java.util.concurrent.Callable

internal open class TaskRequest(internal val request: Request) : Callable<Response> {
    var interruptCallback: ((Request) -> Unit)? = null

    override fun call(): Response = try {
        val modifiedRequest = request.requestInterceptor?.invoke(request) ?: request
        val response = request.client.executeRequest(modifiedRequest)

        request.responseInterceptor?.invoke(modifiedRequest, response) ?: response
    } catch (error: FuelError) {
        if (error.exception as? InterruptedIOException != null) {
            interruptCallback?.invoke(request)
        }
        throw error
    } catch (exception: Exception) {
        throw FuelError(exception)
    }
}