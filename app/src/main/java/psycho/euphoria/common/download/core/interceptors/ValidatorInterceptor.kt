package psycho.euphoria.common.download.core.interceptors

import psycho.euphoria.common.download.core.FuelError
import psycho.euphoria.common.download.core.HttpException
import psycho.euphoria.common.download.core.Request
import psycho.euphoria.common.download.core.Response

fun validatorResponseInterceptor(validRange: IntRange) =
        { next: (Request, Response) -> Response ->
            { request: Request, response: Response ->
                if (!validRange.contains(response.statusCode)) {
                    val error = FuelError(HttpException(response.statusCode, response.responseMessage), response.data, response)
                    throw error
                } else {
                    next(request, response)
                }
            }
        }




