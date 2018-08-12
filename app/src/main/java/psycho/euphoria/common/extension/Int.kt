package psycho.euphoria.common.extension

import android.util.DisplayMetrics
import java.util.*
import kotlin.math.max
import kotlin.math.min

fun Int.dpToPx(metrics: DisplayMetrics) = (this * metrics.density + 0.5f).toInt()
fun Int.contrain(minValue: Int, maxValue: Int) = max(minValue, min(this, maxValue))

fun <T> Int.inRange(array: Array<T>): Boolean {
    return this >= 0 && this < array.size
}

fun <T> Int.inRange(array: List<T>): Boolean {
    return this >= 0 && this < array.size
}

fun Int.getFormattedDuration(sb: StringBuilder = StringBuilder(8)): String {
    val hours = this / 3600
    val minutes = this % 3600 / 60
    val seconds = this % 60
    if (this >= 3600) {
        sb.append(String.format(Locale.getDefault(), "%02d", hours)).append(":")
    }
    sb.append(String.format(Locale.getDefault(), "%02d", minutes))
    sb.append(":").append(String.format(Locale.getDefault(), "%02d", seconds))
    return sb.toString()
}

fun Int.clamp(min: Int, max: Int): Int {
    if (this > max) return max
    return if (this < min) min else this
}