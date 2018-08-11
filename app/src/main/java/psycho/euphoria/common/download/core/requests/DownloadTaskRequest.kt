package psycho.euphoria.common.download.core.requests


import psycho.euphoria.common.download.core.Request
import psycho.euphoria.common.download.core.Response
import psycho.euphoria.common.download.util.copyTo
import java.io.File
import java.io.FileOutputStream
import java.net.URL

internal class DownloadTaskRequest(request: Request) : TaskRequest(request) {
    var progressCallback: ((Long, Long) -> Unit)? = null
    lateinit var destinationCallback: ((Response, URL) -> File)

    override fun call(): Response {
        val response = super.call()
        val file = destinationCallback(response, request.url)

        FileOutputStream(file).use {
            response.dataStream.copyTo(out = it, bufferSize =  BUFFER_SIZE, progress =  { readBytes ->
                progressCallback?.invoke(readBytes, response.contentLength)
            }){
                response.data = it
            }
        }
        return response
    }
}

private const val BUFFER_SIZE = 1024


