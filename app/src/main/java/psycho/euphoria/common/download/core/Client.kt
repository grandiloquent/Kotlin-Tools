package psycho.euphoria.common.download.core

interface Client {
    fun executeRequest(request: Request): Response
}



