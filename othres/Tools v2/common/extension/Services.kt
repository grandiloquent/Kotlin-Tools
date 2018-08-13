package psycho.euphoria.common.extension

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.view.WindowManager
import kotlin.properties.Delegates

object Services {
    var context: Context by Delegates.notNull<Context>()
    val audioManager by lazy {
        if (Build.VERSION.SDK_INT >= 23)
            context.getSystemService(AudioManager::class.java)
        else
            context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    val windowManaer by lazy {
        if (Build.VERSION.SDK_INT >= 23)
            context.getSystemService(WindowManager::class.java)
        else
            context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
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
