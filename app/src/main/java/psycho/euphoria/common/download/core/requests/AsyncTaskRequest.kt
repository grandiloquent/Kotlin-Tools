package psycho.euphoria.common.download.core.requests

import psycho.euphoria.common.download.core.FuelError
import psycho.euphoria.common.download.core.Response

internal class AsyncTaskRequest(private val task: TaskRequest) : TaskRequest(task.request) {
    var successCallback: ((Response) -> Unit)? = null
    var failureCallback: ((FuelError, Response) -> Unit)? = null

    override fun call(): Response = try {
        task.call().apply {
            successCallback?.invoke(this)
        }
    } catch (error: FuelError) {
        failureCallback?.invoke(error, error.response)
        errorResponse()
    } catch (ex: Exception) {
        val error = FuelError(ex)
        val response = errorResponse()
        failureCallback?.invoke(error, response)
        response
    }

    private fun errorResponse() = Response(request.url)
}




