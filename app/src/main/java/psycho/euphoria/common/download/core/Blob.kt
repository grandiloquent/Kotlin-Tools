package psycho.euphoria.common.download.core

import java.io.InputStream

data class Blob(
        val name: String = "",
        val length: Long,
        val inputStream: () -> InputStream)



