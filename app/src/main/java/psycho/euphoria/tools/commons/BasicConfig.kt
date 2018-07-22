package psycho.euphoria.tools.commons

import android.content.Context

open class BasicConfig(val context: Context) {

    protected val prefs = context.getSharedPrefs()

    companion object {
        fun newInstance(context: Context) = BasicConfig(context)

    }
}