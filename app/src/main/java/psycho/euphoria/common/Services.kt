package psycho.euphoria.common

import android.app.ActivityManager
import android.app.NotificationManager
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Point
import android.media.AudioManager
import android.os.Build
import android.preference.PreferenceManager
import android.view.WindowManager
import psycho.euphoria.common.extension.getSDCardPath
import psycho.euphoria.common.extension.putString
import kotlin.properties.Delegates

object Services {

    private const val KEY_OTG_PARTITION = "otg_partition"
    private const val KEY_SD_CARD_PATH = "sd_card_path"
    private const val KEY_TREE_URI = "tree_uri"
    private const val KEY_OTG_TREE_URI = "otg_tree_uri"
    private const val KEY_TEXT_COLOR = "text_color"
    private const val KEY_BACKGROUND_COLOR = "background_color"
    private const val KEY_KEEP_LAST_MODIFIED = "keep_last_modified"
    var context: Context by Delegates.notNull<Context>()
    // public static final int BASE = 1;

    private fun getDefaultSDCardPath() = if (prefer.contains(KEY_SD_CARD_PATH)) "" else context.getSDCardPath()

    val windowManager by lazy {
        if (Build.VERSION.SDK_INT >= 23)
            context.getSystemService(WindowManager::class.java)
        else
            context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }
    val prefer by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    val density by lazy {
        context.resources.displayMetrics.density
    }
    var treeUri: String
        get() = prefer.getString(KEY_TREE_URI, "")
        set(value) = prefer.putString(KEY_TREE_URI, value)
    var OTGTreeUri: String
        get() = prefer.getString(KEY_OTG_TREE_URI, "")
        set(value) = prefer.putString(KEY_OTG_TREE_URI, value)
    var sdCardPath: String
        get() = prefer.getString(KEY_SD_CARD_PATH, getDefaultSDCardPath())
        set(sdCardPath) = prefer.edit().putString(KEY_SD_CARD_PATH, sdCardPath).apply()
    var textColor: Int
        get() = prefer.getInt(KEY_TEXT_COLOR, 0XFFEEEEEE.toInt())
        set(textColor) = prefer.edit().putInt(KEY_TEXT_COLOR, textColor).apply()
    var backgroundColor: Int
        get() = prefer.getInt(KEY_BACKGROUND_COLOR, 0XFF424242.toInt())
        set(backgroundColor) = prefer.edit().putInt(KEY_BACKGROUND_COLOR, backgroundColor).apply()

    var OTGPartition: String
        get() = prefer.getString(KEY_OTG_PARTITION, "")
        set(OTGPartition) = prefer.edit().putString(KEY_OTG_PARTITION, OTGPartition).apply()
    var keepLastModified: Boolean
        get() = prefer.getBoolean(KEY_KEEP_LAST_MODIFIED, true)
        set(keepLastModified) = prefer.edit().putBoolean(KEY_KEEP_LAST_MODIFIED, keepLastModified).apply()

    val clipboardManager by lazy {
        if (Build.VERSION.SDK_INT >= 23)
            context.getSystemService(ClipboardManager::class.java)
        else
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }
    val navigationBarHeight by lazy {
        if (navigationBarBottom) navigationBarSize.y else 0
    }

    val navigationBarBottom by lazy {
        usableScreenSize.y < realScreenSize.y
    }
    val usableScreenSize by lazy {
        val size = Point()
        windowManager.defaultDisplay.getSize(size)
        size

    }
    val navigationBarRight by lazy {
        usableScreenSize.x < realScreenSize.x
    }
    val navigationBarWidth by lazy {
        if (navigationBarRight) navigationBarSize.x else 0
    }
    val realScreenSize by lazy {
        val size = Point()
        /**
         * Gets the real size of the display without subtracting any window decor or
         * applying any compatibility scale factors.
         * <p>
         * The size is adjusted based on the current rotation of the display.
         * </p><p>
         * The real size may be smaller than the physical size of the screen when the
         * window manager is emulating a smaller display (using adb shell wm size).
         * </p>
         *
         * @param outSize Set to the real size of the display.
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            windowManager.defaultDisplay.getRealSize(size)


        size
    }
    val navigationBarSize by lazy {
        when {
            navigationBarRight -> Point(realScreenSize.x - usableScreenSize.x, usableScreenSize.y)
            navigationBarBottom -> Point(usableScreenSize.x, realScreenSize.y - usableScreenSize.y)
            else -> Point()
        }
    }

    val audioManager by lazy {
        if (Build.VERSION.SDK_INT >= 23)
            context.getSystemService(AudioManager::class.java)
        else
            context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    val activityManager by lazy {
        if (Build.VERSION.SDK_INT >= 23)
            context.getSystemService(ActivityManager::class.java)
        else
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    }
    val notificationManager by lazy {
        if (Build.VERSION.SDK_INT >= 23)
            context.getSystemService(NotificationManager::class.java)
        else
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    val maxMusicVolume by lazy {
        audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    }

    var musicVolume: Int
        get() = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        set(value) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, value, 0)
        }
}
