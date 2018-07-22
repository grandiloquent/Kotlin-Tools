package psycho.euphoria.tools.commons

import java.io.File


fun File.createDirectory() {
    if (!exists()) mkdirs()
}
