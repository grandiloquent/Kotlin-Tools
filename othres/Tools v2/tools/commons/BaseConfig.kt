package psycho.euphoria.tools.commons

import android.content.Context
import psycho.euphoria.tools.R

open class BaseConfig(val context: Context) {

    protected val prefs = context.getSharedPrefs()

    private fun getDefaultSDCardPath() = if (prefs.contains(KEY_SDCARD_PATH)) "" else context.getSDCardPath()

    var backgroundColor: Int
        get() = prefs.getInt(KEY_BACKGROUND_COLOR, context.resources.getColor(R.color.default_background_color))
        set(backgroundColor) = prefs.edit().putInt(KEY_BACKGROUND_COLOR, backgroundColor).apply()

    var keepLastModified: Boolean
        get() = prefs.getBoolean(KEY_KEEP_LAST_MODIFIED, true)
        set(keepLastModified) = prefs.edit().putBoolean(KEY_KEEP_LAST_MODIFIED, keepLastModified).apply()

    var OTGPartition: String
        get() = prefs.getString(KEY_OTG_PARTITION, "")
        set(OTGPartition) = prefs.edit().putString(KEY_OTG_PARTITION, OTGPartition).apply()

    var OTGTreeUri: String
        get() = prefs.getString(KEY_OTG_TREE_URI, "")
        set(OTGTreeUri) = prefs.edit().putString(KEY_OTG_TREE_URI, OTGTreeUri).apply()

    var primaryColor: Int
        get() = prefs.getInt(KEY_PRIMARY_COLOR, context.resources.getColor(R.color.color_primary))
        set(primaryColor) = prefs.edit().putInt(KEY_PRIMARY_COLOR, primaryColor).apply()

    var sdCardPath: String
        get() = prefs.getString(KEY_SDCARD_PATH, getDefaultSDCardPath())
        set(sdCardPath) = prefs.edit().putString(KEY_SDCARD_PATH, sdCardPath).apply()

    var textColor: Int
        get() = prefs.getInt(KEY_TEXT_COLOR, context.resources.getColor(R.color.default_text_color))
        set(textColor) = prefs.edit().putInt(KEY_TEXT_COLOR, textColor).apply()

    var treeUri: String
        get() = prefs.getString(KEY_TREE_URI, "")
        set(uri) = prefs.edit().putString(KEY_TREE_URI, uri).apply()

    companion object {
        fun newInstance(context: Context) = BaseConfig(context)

    }
}