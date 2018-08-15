package psycho.euphoria.tools.commons
import android.graphics.Point
import android.text.format.DateFormat
import java.io.File
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.*
fun ClosedRange<Char>.randomString(lenght: Int) = (1..lenght).map { (Random().nextInt(endInclusive.toInt() - start.toInt()) + start.toInt()).toChar() }.joinToString("")
fun ClosedRange<Int>.random() = Random().nextInt((endInclusive + 1) - start) + start
fun Point.formatAsResolution() = "$x x $y ${getMPx()}"
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
fun Int.formatNumberWithLocale(): String {
    return String.format(Locale.getDefault(), "%d", this)
}
fun Point.getMPx(): String {
    val px = x * y / 1000000.toFloat()
    val rounded = Math.round(px * 10) / 10.toFloat()
    return "(${rounded}MP)"
}
fun getPaths(file: File): ArrayList<String> {
    val paths = arrayListOf<String>(file.absolutePath)
    if (file.isDirectory) {
        val files = file.listFiles() ?: return paths
        for (curFile in files) {
            paths.addAll(getPaths(curFile))
        }
    }
    return paths
}