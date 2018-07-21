package psycho.euphoria.tools.commons

import android.util.Patterns
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.*



fun ClosedRange<Char>.randomString(lenght: Int) =
        (1..lenght)
                .map { (Random().nextInt(endInclusive.toInt() - start.toInt()) + start.toInt()).toChar() }
                .joinToString("")

fun ClosedRange<Int>.random() =
        Random().nextInt((endInclusive + 1) - start) + start

fun Long.formatSize(): String {
    if (this <= 0)
        return "0 B"
    val units = arrayOf("B", "kB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(toDouble()) / Math.log10(1024.0)).toInt()
    return "${DecimalFormat("#,##0.#").format(this / Math.pow(1024.0, digitGroups.toDouble()))} ${units[digitGroups]}"
}

fun Double.round(newScale: Int = 2): Double {
    return toBigDecimal().setScale(newScale, RoundingMode.HALF_UP).toDouble()
}

fun String.isValidURL(): Boolean {
    return Patterns.WEB_URL.matcher(this).matches() //URLUtil.isValidUrl(this)
}