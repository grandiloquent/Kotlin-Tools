package psycho.euphoria.common.extension
fun CharSequence?.eq(b: CharSequence?): Boolean {
    if (this == b) return true
    this?.let {
        val length = it.length
        if (b != null && length == b.length) {
            if (it is String && b is String) {
                return it.equals(b)
            } else {
                for (i in 0 until length) {
                    if (it[i] != b[i]) return false
                }
                return true
            }
        }
    }
    return false
}