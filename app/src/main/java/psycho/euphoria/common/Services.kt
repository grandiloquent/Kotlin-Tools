package psycho.euphoria.common

import android.annotation.SuppressLint
import android.app.*
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Point
import android.graphics.Rect
import android.media.AudioManager
import android.os.Build
import android.preference.PreferenceManager
import android.util.Log
import kotlin.properties.Delegates
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.Toast
import java.text.DecimalFormat
import java.util.*

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Services.context = this
    }
}

object Services {
    const val TAG = "Services"
    private const val KEY_OTG_PARTITION = "otg_partition"
    private const val KEY_SD_CARD_PATH = "sd_card_path"
    private const val KEY_TREE_URI = "tree_uri"
    private const val KEY_OTG_TREE_URI = "otg_tree_uri"
    private const val KEY_TEXT_COLOR = "text_color"
    private const val KEY_BACKGROUND_COLOR = "background_color"
    private const val KEY_KEEP_LAST_MODIFIED = "keep_last_modified"

    var context: Context by Delegates.notNull<Context>()

    var backgroundColor: Int
        get() = prefer.getInt(KEY_BACKGROUND_COLOR, 0XFF424242.toInt())
        set(backgroundColor) = prefer.edit().putInt(KEY_BACKGROUND_COLOR, backgroundColor).apply()
    // public static final int BASE = 1;
    val isNetworkValid: Boolean
        get() {
            try {
                var networkInfo = connectivityManager.getNetworkInfo(0)
                if (networkInfo?.state == NetworkInfo.State.CONNECTED) {
                    return true
                } else {
                    networkInfo = connectivityManager.getNetworkInfo(1)
                    if (networkInfo?.state == NetworkInfo.State.CONNECTED) {
                        return true
                    }
                }
            } catch (ignored: Exception) {
            }
            return false
        }
    var keepLastModified: Boolean
        get() = prefer.getBoolean(KEY_KEEP_LAST_MODIFIED, true)
        set(keepLastModified) = prefer.edit().putBoolean(KEY_KEEP_LAST_MODIFIED, keepLastModified).apply()
    var musicVolume: Int
        get() = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        set(value) = audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, value, 0)
    val orientation: Int
        get() = context.resources.configuration.orientation

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
    val widthPixels: Int
        get() = context.resources.displayMetrics.widthPixels

    /*==================================*/
    val connectivityManager by lazy {
        if (Build.VERSION.SDK_INT >= 23)
            context.getSystemService(ConnectivityManager::class.java)
        else
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
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
    val handler by lazy {
        Handler(Looper.getMainLooper())
    }
    /*==================================*/

    fun toast(message: String, length: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(context, message, length).show()
    }

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

@SuppressLint("WrongConstant")
fun Context.createNotificationChannel(channelId: String, channelName: String, channelImportance: Int = 1) {
    //  public static final int IMPORTANCE_MIN = 1;
    /**
     * Creates a notification channel that notifications can be posted to.
     *
     * This can also be used to restore a deleted channel and to update an existing channel's
     * name, description, and/or importance.
     *
     * <p>The name and description should only be changed if the locale changes
     * or in response to the user renaming this channel. For example, if a user has a channel
     * named 'John Doe' that represents messages from a 'John Doe', and 'John Doe' changes his name
     * to 'John Smith,' the channel can be renamed to match.
     *
     * <p>The importance of an existing channel will only be changed if the new importance is lower
     * than the current value and the user has not altered any settings on this channel.
     *
     * All other fields are ignored for channels that already exist.
     *
     * @param channel  the channel to create.  Note that the created channel may differ from this
     *                 value. If the provided channel is malformed, a RemoteException will be
     *                 thrown.
     */
    /**
     * Creates a notification channel.
     *
     * @param id The id of the channel. Must be unique per package. The value may be truncated if
     *           it is too long.
     * @param name The user visible name of the channel. You can rename this channel when the system
     *             locale changes by listening for the {@link Intent#ACTION_LOCALE_CHANGED}
     *             broadcast. The recommended maximum length is 40 characters; the value may be
     *             truncated if it is too long.
     * @param importance The importance of the channel. This controls how interruptive notifications
     *                   posted to this channel are.
     */
    /**
     * Min notification importance: only shows in the shade, below the fold.  This should
     * not be used with {@link Service#startForeground(int, Notification) Service.startForeground}
     * since a foreground service is supposed to be something the user cares about so it does
     * not make semantic sense to mark its notification as minimum importance.  If you do this
     * as of Android version {@link android.os.Build.VERSION_CODES#O}, the system will show
     * a higher-priority notification about your app running in the background.
     *    public static final int IMPORTANCE_MIN = 1;
     */
    if (Build.VERSION.SDK_INT >= 26) {
        Services.notificationManager.createNotificationChannel(NotificationChannel(channelId, channelName, channelImportance))
    }
}

fun Int.getFormattedDuration(sb: StringBuilder = StringBuilder(8)): String {
    val hours = this / 3600
    val minutes = this % 3600 / 60
    val seconds = this % 60
    if (this >= 3600) {
        sb.append(String.format(Locale.getDefault(), "%02d", hours)).append(":")
    }
    sb.append(String.format(Locale.getDefault(), "%02d", minutes))
    sb.append(":").append(String.format(Locale.getDefault(), "%02d", seconds))
    return sb.toString()
}

fun Intent.string(key: String): String? {
    try {
        return getStringExtra(key)
    } catch (throwable: Throwable) {
        return null
    }
}

fun Intent.long(key: String, defaultValue: Long = -1L): Long {
    try {
        return getLongExtra(key, defaultValue)
    } catch (ignored: Exception) {
        return defaultValue
    }
}

fun Intent.int(key: String, defaultValue: Int = -1): Int {
    try {
        return getIntExtra(key, defaultValue)
    } catch (ignored: Exception) {
        return defaultValue
    }
}

fun Long.formatSize(): String {
    if (this <= 0)
        return "0 B"
    val units = arrayOf("B", "kB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(toDouble()) / Math.log10(1024.0)).toInt()
    return "${DecimalFormat("#,##0.#").format(this / Math.pow(1024.0, digitGroups.toDouble()))} ${units[digitGroups]}"
}

fun Long.getStringForTime(builder: StringBuilder, formatter: Formatter): String {
    var timeMs = this
    val totalSeconds = (timeMs + 500) / 1000
    val seconds = totalSeconds % 60
    val minutes = totalSeconds / 60 % 60
    val hours = totalSeconds / 3600
    builder.setLength(0)
    return if (hours > 0)
        formatter.format("%d:%02d:%02d", hours, minutes, seconds).toString()
    else
        formatter.format("%02d:%02d", minutes, seconds).toString()
}

fun View.visible() {
    if (visibility != View.VISIBLE) {
        visibility = View.VISIBLE
    }
}


fun Activity.launchService(klass: Class<*>, processIntent: ((Intent) -> Unit)? = null) {
    val intent = Intent(this, klass)
    processIntent?.invoke(intent)
    startService(intent)
}

fun String.combinePath(fileName: String): String {
    var p = this
    if (p[p.length - 1] != '/')
        p += '/'
    var name = fileName.trim()
    if (name[0] == '/')
        name = substring(1)
    return p + name
}

fun Notification.Builder.addServiceAction(context: Context, klass: Class<*>, action: String, requestCode: Int, iconResId: Int, title: String) {
    val intent = Intent(context, klass)
    intent.action = action
    val pendingIntent = PendingIntent.getService(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT)

    if (Build.VERSION.SDK_INT >= 23) {
        addAction(Notification.Action.Builder(iconResId, title, pendingIntent).build())
    } else if (Build.VERSION.SDK_INT >= 16) {
        addAction(iconResId, title, pendingIntent)
    }
}

fun Handler.send(what: Int, obj: Any? = null, arg1: Int = -1, arg2: Int = -1) {
    val message = if (obj != null) obtainMessage(what, obj) else obtainMessage(what)
    if (arg1 != -1)
        message.arg1 = arg1
    if (arg2 != -1)
        message.arg2 = arg2
    sendMessage(message)

}

fun Activity.calculateScreenOrientation(): Int {
    val displayRotation = getDisplayRotation()
    var standard = displayRotation < 180
    if (Services.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        if (standard)
            return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        else return ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
    } else {
        if (displayRotation == 90 || displayRotation == 270) {
            standard = !standard
        }

        return if (standard) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT

    }
}

fun Activity.getDisplayRotation(): Int {
    val rotation = windowManager.defaultDisplay.rotation
    return when (rotation) {
        Surface.ROTATION_0 -> 0
        Surface.ROTATION_90 -> 90
        Surface.ROTATION_180 -> 180
        Surface.ROTATION_270 -> 270
        else -> 0
    }
}

fun Context.requestPermission() {
    val intent = Intent("android.content.pm.action.REQUEST_PERMISSIONS")
    intent.putExtra("android.content.pm.extra.REQUEST_PERMISSIONS_NAMES", arrayOf(
            "android.permission.INTERNET"
    ))

    var clazz: Class<*>? = null
    var pn: String? = null
    try {
        clazz = Class.forName("android.content.pm.PackageManager")
        // instance = clazz.newInstance()
        val result = clazz?.getMethod("getPermissionControllerPackageName")?.invoke(Services.packageManager)
        pn = result.toString()
    } catch (e: Exception) {
    } finally {
        // instance?.let { clazz?.getMethod("release")?.invoke(instance) }
    }

    intent.`package` = pn
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    startActivity(intent)
}