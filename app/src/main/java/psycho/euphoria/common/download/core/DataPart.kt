package psycho.euphoria.common.download.core



import java.io.File

data class DataPart(
        val file: File,
        val name: String = file.nameWithoutExtension,
        val type: String = "")


