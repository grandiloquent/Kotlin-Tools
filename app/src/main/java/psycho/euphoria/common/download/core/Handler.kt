package psycho.euphoria.common.download.core

interface Handler<in T> {
    fun success(request: Request, response: Response, value: T)
    fun failure(request: Request, response: Response, error: FuelError)
}




