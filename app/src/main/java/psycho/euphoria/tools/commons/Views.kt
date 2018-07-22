package psycho.euphoria.tools.commons

import android.view.View
import android.view.ViewTreeObserver

fun View.isVisible() = visibility == View.VISIBLE
fun View.beVisibleIf(beVisible: Boolean) = if (beVisible) beVisible() else beGone()


fun View.beGone() {
    visibility = View.GONE
}

fun View.beInvisible() {
    visibility = View.INVISIBLE
}

fun View.beVisible() {
    visibility = View.VISIBLE
}

fun View.onGlobalLayout(callback: () -> Unit) {
    viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            viewTreeObserver.removeOnGlobalLayoutListener(this)
            callback()
        }
    })
}