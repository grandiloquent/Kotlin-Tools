package psycho.euphoria.tools.commons

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.Point
import android.media.AudioManager
import android.os.Build
import android.os.Environment
import android.util.DisplayMetrics
import android.view.WindowManager
import android.widget.Toast
import psycho.euphoria.tools.Config

fun Context.getInternalStoragePath() = Environment.getExternalStorageDirectory().absolutePath.trimEnd('/')
fun Context.getSharedPrefs() = getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
val Context.audioManager get() = getSystemService(Context.AUDIO_SERVICE) as AudioManager
val Context.config: Config get() = Config.newInstance(applicationContext)
val Context.navigationBarBottom: Boolean get() = usableScreenSize.y < realScreenSize.y
val Context.navigationBarHeight: Int get() = if (navigationBarBottom) navigationBarSize.y else 0
val Context.navigationBarRight: Boolean get() = usableScreenSize.x < realScreenSize.x
val Context.navigationBarWidth: Int get() = if (navigationBarRight) navigationBarSize.x else 0
val Context.portrait get() = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
val Context.version: Int get() = Build.VERSION.SDK_INT
val Context.windowManager: WindowManager get() = getSystemService(Context.WINDOW_SERVICE) as WindowManager




internal val Context.navigationBarSize: Point
    get() = when {
        navigationBarRight -> Point(realScreenSize.x - usableScreenSize.x, usableScreenSize.y)
        navigationBarBottom -> Point(usableScreenSize.x, realScreenSize.y - usableScreenSize.y)
        else -> Point()
    }


val Context.alarmManager: AlarmManager
    get() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            return getSystemService(AlarmManager::class.java)
        else
            return getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }
val Context.clipboardManager: ClipboardManager
    get() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            return getSystemService(ClipboardManager::class.java)
        else
            return getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

val Context.notificationManager: NotificationManager
    get() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            return getSystemService(NotificationManager::class.java)
        else
            return getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
val Context.realScreenSize: Point
    get() {
        val size = Point()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            windowManager.defaultDisplay.getRealSize(size)
        return size
    }
val Context.usableScreenSize: Point
    get() {
        val size = Point()
        windowManager.defaultDisplay.getSize(size)
        return size
    }


fun Context.humanizePath(path: String): String {
    return ""
}

fun Context.toast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun Context.getHumanizedFilename(path: String): String {
    val humanized = humanizePath(path)
    return humanized.substring(humanized.lastIndexOf("/") + 1)
}