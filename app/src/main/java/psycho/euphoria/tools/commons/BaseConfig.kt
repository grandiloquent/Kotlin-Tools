package psycho.euphoria.tools.commons

import android.content.Context

open class BaseConfig(val context: Context) {

    protected val prefs = context.getSharedPrefs()

    private fun getDefaultSDCardPath() = if (prefs.contains(KEY_SDCARD_PATH)) "" else context.getSDCardPath()

    var OTGPartition: String
        get() = prefs.getString(KEY_OTG_PARTITION, "")
        set(OTGPartition) = prefs.edit().putString(KEY_OTG_PARTITION, OTGPartition).apply()

    var OTGTreeUri: String
        get() = prefs.getString(KEY_OTG_TREE_URI, "")
        set(OTGTreeUri) = prefs.edit().putString(KEY_OTG_TREE_URI, OTGTreeUri).apply()
    var sdCardPath: String
        get() = prefs.getString(KEY_SDCARD_PATH, getDefaultSDCardPath())
        set(sdCardPath) = prefs.edit().putString(KEY_SDCARD_PATH, sdCardPath).apply()

    var treeUri: String
        get() = prefs.getString(KEY_TREE_URI, "")
        set(uri) = prefs.edit().putString(KEY_TREE_URI, uri).apply()

    companion object {
        fun newInstance(context: Context) = BaseConfig(context)

    }
}