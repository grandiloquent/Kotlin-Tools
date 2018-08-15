package psycho.euphoria.common.extension
import java.math.RoundingMode
fun Double.round(newScale: Int = 2): Double {
    return toBigDecimal().setScale(newScale, RoundingMode.HALF_UP).toDouble()
}