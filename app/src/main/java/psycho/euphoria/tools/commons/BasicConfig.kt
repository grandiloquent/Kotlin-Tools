package psycho.euphoria.tools.commons

import android.content.Context

open class BasicConfig(val context: Context) {

    protected val prefs = context.getSharedPrefs()

    var sdCardPath: String
        get() = prefs.getString(KEY_SDCARD_PATH, "")
        set(sdCardPath) = prefs.edit().putString(KEY_SDCARD_PATH, sdCardPath).apply()


    companion object {
        fun newInstance(context: Context) = BasicConfig(context)

    }
}