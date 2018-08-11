package psycho.euphoria.tools.ui.offline

import java.io.IOException

class DownloadException : IOException {
    constructor(message: String) : super(message)
    constructor(cause: Throwable) : super(cause)
}