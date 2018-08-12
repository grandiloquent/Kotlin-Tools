package psycho.euphoria.common.extension

fun Float.clamp(min: Float, max: Float): Float {
    if (this > max) return max
    return if (this < min) min else this
}