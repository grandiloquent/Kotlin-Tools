package psycho.euphoria.common.extension

import android.os.Build

object C {
    const val TIME_UNSET = Long.MIN_VALUE + 1
    const val INDEX_UNSET = -1

    inline fun atLeast(sdk: Int, f1: () -> Unit, f2: () -> Unit) {
        if (Build.VERSION.SDK_INT >= sdk) f1()
        else f2.invoke()
    }

    inline fun atMost(sdk: Int, f1: () -> Unit, f2: () -> Unit) {
        if (Build.VERSION.SDK_INT <= sdk) f1()
        else f2.invoke()
    }

    inline fun more(sdk: Int, f1: () -> Unit, f2: () -> Unit) {
        if (Build.VERSION.SDK_INT > sdk) f1()
        else f2.invoke()
    }
}