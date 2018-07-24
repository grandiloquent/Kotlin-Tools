package psycho.euphoria.tools.commons

import android.os.Build
import android.view.View
import android.view.ViewTreeObserver

private const val SIXTY_FPS_INTERVAL = 1000 / 60L

fun View.isVisible() = visibility == View.VISIBLE
fun View.beVisibleIf(beVisible: Boolean) = if (beVisible) beVisible() else beGone()

fun View.postOnAnimationCompat(runnable: Runnable) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
        postOnAnimation(runnable)
    else postDelayed(runnable, SIXTY_FPS_INTERVAL)
}
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