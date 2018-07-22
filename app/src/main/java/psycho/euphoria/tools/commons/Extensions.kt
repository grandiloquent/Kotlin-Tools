package psycho.euphoria.tools.commons

import android.graphics.Point
import android.text.format.DateFormat
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.*

fun ClosedRange<Char>.randomString(lenght: Int) = (1..lenght).map { (Random().nextInt(endInclusive.toInt() - start.toInt()) + start.toInt()).toChar() }.joinToString("")
fun ClosedRange<Int>.random() = Random().nextInt((endInclusive + 1) - start) + start
fun Point.formatAsResolution() = "$x x $y ${getMPx()}"


fun Double.round(newScale: Int = 2): Double {
    return toBigDecimal().setScale(newScale, RoundingMode.HALF_UP).toDouble()
}
fun Int.getFormattedDuration(): String {
    val sb = StringBuilder(8)
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
fun Long.formatDate(): String {
    val cal = Calendar.getInstance(Locale.ENGLISH)
    cal.timeInMillis = this
    return DateFormat.format("dd.MM.yyyy kk:mm", cal).toString()
}
fun Long.formatSize(): String {
    if (this <= 0)
        return "0 B"
    val units = arrayOf("B", "kB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(toDouble()) / Math.log10(1024.0)).toInt()
    return "${DecimalFormat("#,##0.#").format(this / Math.pow(1024.0, digitGroups.toDouble()))} ${units[digitGroups]}"
}
fun Point.getMPx(): String {
    val px = x * y / 1000000.toFloat()
    val rounded = Math.round(px * 10) / 10.toFloat()
    return "(${rounded}MP)"
}