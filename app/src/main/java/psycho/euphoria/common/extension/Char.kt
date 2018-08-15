package psycho.euphoria.common.extension
fun Char.isValidFatFilenameChar(): Boolean {
    if (this.toInt() in 0x00..0x1f) {
        return false
    }
    when (this) {
        '"', '*', '/', ':', '<', '>', '?', '\\', '|', 0x7F.toChar() -> return false
        else -> return true
    }
}