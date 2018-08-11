package psycho.euphoria.common.download.core

class HttpException(httpCode: Int, httpMessage: String) : Exception("HTTP Exception $httpCode $httpMessage")



