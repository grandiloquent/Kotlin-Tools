package psycho.euphoria.common.extension

import android.util.DisplayMetrics
import kotlin.math.max
import kotlin.math.min

fun Int.dpToPx(metrics: DisplayMetrics) = (this * metrics.density + 0.5f).toInt()
fun Int.contrain(minValue: Int, maxValue: Int) = max(minValue, min(this, maxValue))