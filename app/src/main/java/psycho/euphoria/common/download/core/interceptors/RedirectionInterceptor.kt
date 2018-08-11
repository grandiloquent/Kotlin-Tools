package psycho.euphoria.common.download.core.interceptors

import psycho.euphoria.common.download.core.*
import java.net.URI
import java.net.URL
import javax.net.ssl.HttpsURLConnection

private val redirectStatusWithGets = listOf(HttpsURLConnection.HTTP_MOVED_PERM,
        HttpsURLConnection.HTTP_MOVED_TEMP,
        HttpsURLConnection.HTTP_SEE_OTHER)

class RedirectException : Exception("Redirection fail, not found URL to redirect to")

fun redirectResponseInterceptor(manager: FuelManager) =
        { next: (Request, Response) -> Response ->
            { request: Request, response: Response ->

                if (response.isStatusRedirection && request.isAllowRedirects) {
                    val redirectedUrl = response.headers["Location"] ?: response.headers["location"]
                    val newMethod = when (response.statusCode) {
                        in redirectStatusWithGets -> Method.GET
                        else -> {
                            request.method
                        }
                    }

                    if (redirectedUrl?.isNotEmpty() == true) {
                        val redirectedUrlString = redirectedUrl.first()
                        val newUrl = if (URI(redirectedUrlString).isAbsolute) {
                            URL(redirectedUrlString)
                        } else {
                            URL(request.url, redirectedUrlString)
                        }
                        val newHeaders = request.headers.toMutableMap()

                        val encoding = Encoding(httpMethod = newMethod, urlString = newUrl.toString())

                        // check whether it is the same host or not
                        if (newUrl.host != request.url.host) {
                            newHeaders.remove("Authorization")
                        }

                        // redirect
                        next(request, manager.request(encoding).header(newHeaders).response().second)
                    } else {
                        // there is no location detected, just passing along
                        next(request, response)
                    }
                } else {
                    next(request, response)
                }
            }
        }




