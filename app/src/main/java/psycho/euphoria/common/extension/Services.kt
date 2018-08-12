package psycho.euphoria.common.extension

import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.graphics.Point
import android.media.AudioManager
import android.os.Build
import android.view.WindowManager
import kotlin.properties.Delegates

object Services {

    var context: Context by Delegates.notNull<Context>()

    val windowManager by lazy {
        if (Build.VERSION.SDK_INT >= 23)
            context.getSystemService(WindowManager::class.java)
        else
            context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
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
