package psycho.euphoria.tools

import android.content.Context
import psycho.euphoria.tools.commons.BasicConfig
import psycho.euphoria.tools.commons.EXT_EXIF_PROPERTIES
import psycho.euphoria.tools.commons.EXT_LAST_MODIFIED
import psycho.euphoria.tools.commons.EXT_RESOLUTION

const val KEY_SHOW_EXTENDED_DETAILS = "show_extended_details"
const val KEY_EXTENDED_DETAILS = "extended_details"
const val KEY_MEDIUM = "medium"
const val KEY_AUTOPLAY_VIDEOS = "AUTOPLAY_VIDEOS"
const val KEY_ALLOW_VIDEO_GESTURES = "allow_video_gestures"
const val KEY_ALLOW_INSTANT_CHANGE = "allow_instant_change"
const val KEY_BOTTOM_ACTIONS = "bottom_actions"
const val KEY_LOOP_VIDEOS = "loop_videos"
const val KEY_HIDE_EXTENDED_DETAILS = "hide_extended_details"
val Context.config: Config get() = Config.newInstance(applicationContext)

class Config(context: Context) : BasicConfig(context) {

    var allowInstantChange: Boolean
        get() = prefs.getBoolean(KEY_ALLOW_INSTANT_CHANGE, false)
        set(allowInstantChange) = prefs.edit().putBoolean(KEY_ALLOW_INSTANT_CHANGE, allowInstantChange).apply()

    var allowVideoGestures: Boolean
        get() = prefs.getBoolean(KEY_ALLOW_VIDEO_GESTURES, true)
        set(allowVideoGestures) = prefs.edit().putBoolean(KEY_ALLOW_VIDEO_GESTURES, allowVideoGestures).apply()
    var autoplayVideos: Boolean
        get() = prefs.getBoolean(KEY_AUTOPLAY_VIDEOS, false)
        set(autoplay) = prefs.edit().putBoolean(KEY_AUTOPLAY_VIDEOS, autoplay).apply()

    var bottomActions: Boolean
        get() = prefs.getBoolean(KEY_BOTTOM_ACTIONS, true)
        set(bottomActions) = prefs.edit().putBoolean(KEY_BOTTOM_ACTIONS, bottomActions).apply()

    var extendedDetails: Int
        get() = prefs.getInt(KEY_EXTENDED_DETAILS, EXT_RESOLUTION or EXT_LAST_MODIFIED or EXT_EXIF_PROPERTIES)
        set(value) = prefs.edit().putInt(KEY_EXTENDED_DETAILS, value).apply()

    var hideExtendedDetails: Boolean
        get() = prefs.getBoolean(KEY_HIDE_EXTENDED_DETAILS, false)
        set(hideExtendedDetails) = prefs.edit().putBoolean(KEY_HIDE_EXTENDED_DETAILS, hideExtendedDetails).apply()

    var loopVideos: Boolean
        get() = prefs.getBoolean(KEY_LOOP_VIDEOS, false)
        set(loop) = prefs.edit().putBoolean(KEY_LOOP_VIDEOS, loop).apply()

    var showExtendedDetails: Boolean
        get() = prefs.getBoolean(KEY_SHOW_EXTENDED_DETAILS, false)
        set(showExtendedDetails) = prefs.edit().putBoolean(KEY_SHOW_EXTENDED_DETAILS, showExtendedDetails).apply()

    companion object {
        fun newInstance(context: Context) = Config(context)
    }
}