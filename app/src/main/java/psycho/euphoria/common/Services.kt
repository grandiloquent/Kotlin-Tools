package psycho.euphoria.common

import android.app.ActivityManager
import android.app.Application
import android.app.NotificationManager
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import android.media.AudioManager
import android.os.Build
import android.preference.PreferenceManager
import android.util.Log
import android.view.ViewConfiguration
import android.view.WindowManager
import kotlin.properties.Delegates
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewParent

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Services.context = this
    }
}

object Services {
    private const val TAG = "Services"
    private const val KEY_OTG_PARTITION = "otg_partition"
    private const val KEY_SD_CARD_PATH = "sd_card_path"
    private const val KEY_TREE_URI = "tree_uri"
    private const val KEY_OTG_TREE_URI = "otg_tree_uri"
    private const val KEY_TEXT_COLOR = "text_color"
    private const val KEY_BACKGROUND_COLOR = "background_color"
    private const val KEY_KEEP_LAST_MODIFIED = "keep_last_modified"
    var context: Context by Delegates.notNull<Context>()
    // public static final int BASE = 1;
    var backgroundColor: Int
        get() = prefer.getInt(KEY_BACKGROUND_COLOR, 0XFF424242.toInt())
        set(backgroundColor) = prefer.edit().putInt(KEY_BACKGROUND_COLOR, backgroundColor).apply()
    var keepLastModified: Boolean
        get() = prefer.getBoolean(KEY_KEEP_LAST_MODIFIED, true)
        set(keepLastModified) = prefer.edit().putBoolean(KEY_KEEP_LAST_MODIFIED, keepLastModified).apply()
    var musicVolume: Int
        get() = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        set(value) = audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, value, 0)
    var OTGPartition: String
        get() = prefer.getString(KEY_OTG_PARTITION, "")
        set(OTGPartition) = prefer.edit().putString(KEY_OTG_PARTITION, OTGPartition).apply()
    var OTGTreeUri: String
        get() = prefer.getString(KEY_OTG_TREE_URI, "")
        set(value) = prefer.edit().putString(KEY_OTG_TREE_URI, value).apply()
    var sdCardPath: String
        get() = prefer.getString(KEY_SD_CARD_PATH, "")
        set(sdCardPath) = prefer.edit().putString(KEY_SD_CARD_PATH, sdCardPath).apply()
    var textColor: Int
        get() = prefer.getInt(KEY_TEXT_COLOR, 0XFFEEEEEE.toInt())
        set(textColor) = prefer.edit().putInt(KEY_TEXT_COLOR, textColor).apply()
    var treeUri: String
        get() = prefer.getString(KEY_TREE_URI, "")
        set(value) = prefer.edit().putString(KEY_TREE_URI, value).apply()
    /*==================================*/
    val activityManager by lazy {
        if (Build.VERSION.SDK_INT >= 23)
            context.getSystemService(ActivityManager::class.java)
        else
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    }
    val audioManager by lazy {
        if (Build.VERSION.SDK_INT >= 23)
            context.getSystemService(AudioManager::class.java)
        else
            context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    val clipboardManager by lazy {
        if (Build.VERSION.SDK_INT >= 23)
            context.getSystemService(ClipboardManager::class.java)
        else
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }
    val density by lazy {
        context.resources.displayMetrics.density
    }
    val maxMusicVolume by lazy {
        audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    }
    val navigationBarBottom by lazy {
        usableScreenSize.y < realScreenSize.y
    }
    val navigationBarHeight by lazy {
        //resources.getIdentifier("navigation_bar_height", "dimen", "android")
        //resources.getDimensionPixelSize(resourceId)
        if (navigationBarBottom) navigationBarSize.y else 0
    }
    val navigationBarRight by lazy {
        usableScreenSize.x < realScreenSize.x
    }
    val navigationBarSize by lazy {
        when {
            navigationBarRight -> Point(realScreenSize.x - usableScreenSize.x, usableScreenSize.y)
            navigationBarBottom -> Point(usableScreenSize.x, realScreenSize.y - usableScreenSize.y)
            else -> Point()
        }
    }
    val navigationBarWidth by lazy {
        if (navigationBarRight) navigationBarSize.x else 0
    }
    val notificationManager by lazy {
        if (Build.VERSION.SDK_INT >= 23)
            context.getSystemService(NotificationManager::class.java)
        else
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    val packageManager by lazy {
        context.packageManager
    }
    val prefer by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
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
        Log.e(TAG, "[usableScreenSize] $size")
        size
    }
    val scaledMaximumFlingVelocity by lazy {
        ViewConfiguration.get(context).scaledMaximumFlingVelocity
    }
    val scaledMinimumFlingVelocity by lazy {
        ViewConfiguration.get(context).scaledMinimumFlingVelocity
    }
    val touchSlop by lazy {
        ViewConfiguration.get(context).scaledTouchSlop
    }
    val usableScreenSize by lazy {
        val size = Point()
        /**
         * Gets the size of the display, in pixels.
         * Value returned by this method does not necessarily represent the actual raw size
         * (native resolution) of the display.
         * <p>
         * 1. The returned size may be adjusted to exclude certain system decor elements
         * that are always visible.
         * </p><p>
         * 2. It may be scaled to provide compatibility with older applications that
         * were originally designed for smaller displays.
         * </p><p>
         * 3. It can be different depending on the WindowManager to which the display belongs.
         * </p><p>
         * - If requested from non-Activity context (e.g. Application context via
         * {@code (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE)})
         * it will report the size of the entire display based on current rotation and with subtracted
         * system decoration areas.
         * </p><p>
         * - If requested from activity (either using {@code getWindowManager()} or
         * {@code (WindowManager) getSystemService(Context.WINDOW_SERVICE)}) resulting size will
         * correspond to current app window size. In this case it can be smaller than physical size in
         * multi-window mode.
         * </p><p>
         * Typically for the purposes of layout apps should make a request from activity context
         * to obtain size available for the app content.
         * </p>
         *
         * @param outSize A {@link Point} object to receive the size information.
         */
        windowManager.defaultDisplay.getSize(size)
        Log.e(TAG, "[realScreenSize] $size")
        size
    }
    val windowManager by lazy {
        if (Build.VERSION.SDK_INT >= 23)
            context.getSystemService(WindowManager::class.java)
        else
            context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }
    val widthPixels by lazy {
        context.resources.displayMetrics.widthPixels
    }
    val handler by lazy {
        Handler(Looper.getMainLooper())
    }

    /*==================================*/
    fun dp2px(dp: Int): Int {
        return Math.round(dp.toFloat() * density)
    }

    fun setBackground(view: View, background: Drawable) {
        if (Build.VERSION.SDK_INT >= 16) {
            view.background = background
        } else {
            view.setBackgroundDrawable(background)
        }
    }

    fun offsetTopAndBottom(view: View, offset: Int) {
        if (Build.VERSION.SDK_INT >= 23) {
            view.offsetTopAndBottom(offset)
        } else if (Build.VERSION.SDK_INT >= 21) {
            val parentRect = Rect()
            var needInvalidateWorkaround = false
            val parent = view.parent
            if (parent is View) {
                val p = parent as View
                parentRect.set(p.left, p.top, p.right, p.bottom)
                // If the view currently does not currently intersect the parent (and is therefore
                // not displayed) we may need need to invalidate
                needInvalidateWorkaround = !parentRect.intersects(view.left, view.top,
                        view.right, view.bottom)
            }
            // Now offset, invoking the API 14+ implementation (which contains its own workarounds)
            compatOffsetTopAndBottom(view, offset)
            // The view has now been offset, so let's intersect the Rect and invalidate where
            // the View is now displayed
            if (needInvalidateWorkaround && parentRect.intersect(view.left, view.top,
                            view.right, view.bottom)) {
                (parent as View).invalidate(parentRect)
            }
        } else {
            compatOffsetTopAndBottom(view, offset)
        }
    }

    private fun compatOffsetTopAndBottom(view: View, offset: Int) {
        view.offsetTopAndBottom(offset)
        if (view.visibility == View.VISIBLE) {
            tickleInvalidationFlag(view)
            val parent = view.parent
            if (parent is View) {
                tickleInvalidationFlag(parent as View)
            }
        }
    }

    private fun tickleInvalidationFlag(view: View) {
        val y = view.translationY
        view.translationY = y + 1
        view.translationY = y
    }

    fun getColor(id: Int): Int {
        if (Build.VERSION.SDK_INT >= 23) {
            return context.getColor(id);
        } else {
            return context.resources.getColor(id);
        }
    }
}

inline fun View.visible() {
    if (visibility != View.VISIBLE) {
        visibility = View.VISIBLE
    }
}