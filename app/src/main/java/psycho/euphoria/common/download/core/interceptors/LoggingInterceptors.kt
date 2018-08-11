package psycho.euphoria.common.download.core.interceptors


import psycho.euphoria.common.download.core.Request
import psycho.euphoria.common.download.core.Response

fun <T> loggingRequestInterceptor() =
        { next: (T) -> T ->
            { t: T ->
                println(t.toString())
                next(t)
            }
        }

fun cUrlLoggingRequestInterceptor() =
        { next: (Request) -> Request ->
            { r: Request ->
                println(r.cUrlString())
                next(r)
            }
        }

fun loggingResponseInterceptor(): (Request, Response) -> Response =
        { request: Request, response: Response ->
            println(request.toString())
            println(response.toString())
            response
        }





