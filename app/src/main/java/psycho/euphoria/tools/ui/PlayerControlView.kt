package psycho.euphoria.tools.ui

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.util.RepeatModeUtil
import com.google.android.exoplayer2.util.Util
import java.util.*
import kotlin.math.max
import kotlin.math.round


class PlayerControlView : FrameLayout {

    var controlDispatcher = DefaultControlDispatcher()
        set(value) {
            if (value == null) {
                field = DefaultControlDispatcher()
            } else {
                controlDispatcher = value
            }
        }
    var player: Player? = null
        set(value) {
            if (field == value) {
                return
            }
            if (field != null) {
                field?.removeListener(mComponetListener)
            }
            field = value
            field?.let {
                it.addListener(mComponetListener)
            }
            updateAll()
        }
    private val mWindow = Timeline.Window()
    private val mPeriod = Timeline.Period()
    private var mMultiWindowTimeBar = false
    private val mTimeBar: DefaultTimeBar? = null
    private var mExtraAdGroupTimesMs: LongArray? = null
    private var mDurationView: TextView? = null
    private val mFormatBuilder = StringBuilder()
    private val mFormatter = Formatter()
    private var mPositionView: TextView? = null
    private var mScrubbing = false
    private var mPlayButton: View? = null
    private var mPauseButton: View? = null
    private var mCurrentMode = 0
    private val mUpdateProgressAction = Runnable { updateProgress() }
    private val mComponetListener = ComponetListener()
    var showMultiWindowTimeBar = false
        set(value) {
            field = value
            updateTimeBarMode()
        }
    var visibilityListener: VisibilityListener? = null
    var playbackPreparer: PlaybackPreparer? = null
    var rewinMs = 0L
        set(value) {
            field = value
            updateNavigation()
        }
    var fastForwardMs = 0L
        set(value) {
            field = value
            updateNavigation()
        }
    var showTimeoutMs: Int = 0
        set(value) {
            field = value
            if (isVisible()) {
                hideAfterTimeout()
            }
        }
    var repeatToggleModes = 0
        set(value) {
            player?.let {

                if (field == RepeatModeUtil.REPEAT_TOGGLE_MODE_NONE && mCurrentMode != Player.REPEAT_MODE_OFF) {
                    controlDispatcher.dispatchSetRepeatMode(it, Player.REPEAT_MODE_OFF)
                } else if (field == RepeatModeUtil.REPEAT_TOGGLE_MODE_ONE && mCurrentMode == Player.REPEAT_MODE_ALL) {
                    controlDispatcher.dispatchSetRepeatMode(it, Player.REPEAT_MODE_ONE)
                } else if (field == RepeatModeUtil.REPEAT_TOGGLE_MODE_ALL && mCurrentMode == Player.REPEAT_MODE_ONE) {
                    controlDispatcher.dispatchSetRepeatMode(it, Player.REPEAT_MODE_ALL)
                } else {
                }
            }
        }


    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)


    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        return dispatchMediaKeyEvent(event) || super.dispatchKeyEvent(event)
    }

    private fun hideAfterTimeout() {

    }

    private fun isPlaying(): Boolean {
        return player?.playbackState != Player.STATE_ENDED
                && player?.playbackState != Player.STATE_IDLE
                && player?.playWhenReady == true
    }

    fun dispatchMediaKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        if (player == null || !isHandledMediaKey(keyCode)) return false

        return true
    }

    private fun updateAll() {

    }

    private fun updateNavigation() {

    }

    private fun updateTimeBarMode() {

    }

    private fun seekTo(positionMs: Long) {
        player?.let {
            seekTo(it.currentWindowIndex, positionMs)
        }
    }

    private fun seekTo(windowIndex: Int, positionMs: Long) {
        val dispatched = controlDispatcher.dispatchSeekTo(player, windowIndex, positionMs)
        if (!dispatched) {
            updateProgress()
        }
    }

    fun isVisible(): Boolean {
        return visibility == View.VISIBLE
    }

    private fun updateProgress() {
        if (Build.VERSION.SDK_INT >= 19) {
            if (!isVisible() || !isAttachedToWindow) return
        }
        var position = 0L
        var bufferedPosition = 0L
        var duration = 0L

        player?.let {
            var currentWindowTimeBarOffsetUs = 0L
            var durationUs = 0L
            var adGroupCount = 0

            val timeline = it.currentTimeline
            if (!timeline.isEmpty) {
                val currentWindowIndex = it.currentWindowIndex
                val firstWindowIndex = if (mMultiWindowTimeBar) 0 else currentWindowIndex
                val lastWindowIndex = if (mMultiWindowTimeBar) timeline.windowCount - 1 else currentWindowIndex
                for (i in firstWindowIndex..lastWindowIndex) {
                    if (i == currentWindowIndex) {
                        currentWindowTimeBarOffsetUs = durationUs
                    }
                    timeline.getWindow(i, mWindow)
                    if (mWindow.durationUs == C.TIME_UNSET) {
                        break
                    }
                    for (j in mWindow.firstPeriodIndex..mWindow.lastPeriodIndex) {
                        timeline.getPeriod(j, mPeriod)
                        val periodAdGroupCount = mPeriod.adGroupCount
                        for (adGroupIndex in 0 until periodAdGroupCount) {
                        }
                    }
                    durationUs += mWindow.durationUs
                }
            }
            duration = C.usToMs(durationUs)
            position = C.usToMs(currentWindowTimeBarOffsetUs)
            bufferedPosition = position
            if (it.isPlayingAd) {
                position += it.currentPosition
                bufferedPosition = position
            } else {
                position += it.currentPosition
                bufferedPosition += it.bufferedPercentage
            }
            if (mTimeBar != null) {
                //val extraAdGroupCount = mExtraAdGroupTimesMs?.let { it.size } ?: 0

            }
        }
        mDurationView?.apply {
            text = Util.getStringForTime(mFormatBuilder, mFormatter, duration)
        }
        mPositionView?.apply {
            if (!mScrubbing) {
                text = Util.getStringForTime(mFormatBuilder, mFormatter, duration)
            }
        }
        mTimeBar?.apply {
            setPosition(position)
            setBufferedPosition(bufferedPosition)
            setDuration(duration)
        }
        removeCallbacks(mUpdateProgressAction)
        val playbackState = player?.let { it.playbackState } ?: run { Player.STATE_IDLE }
        if (playbackState != Player.STATE_IDLE && playbackState != Player.STATE_ENDED) {
            var delayMs = 0L
            player?.let {
                if (it.playWhenReady && playbackState == Player.STATE_READY) {
                    val playbackSpeed = it.playbackParameters.speed
                    if (playbackSpeed <= 0.1f) {
                        delayMs = 1000L
                    } else if (playbackSpeed <= 5f) {
                        val mediaTimeUpdatePeriodMs = 1000 / max(1f, round(1 / playbackSpeed))
                        var mediaTimeDelayMs = mediaTimeUpdatePeriodMs - (position % mediaTimeUpdatePeriodMs)
                        if (mediaTimeDelayMs < (mediaTimeUpdatePeriodMs / 5)) {
                            mediaTimeDelayMs += mediaTimeUpdatePeriodMs
                        }
                        delayMs = if (playbackSpeed == 1f) mediaTimeDelayMs.toLong() else (mediaTimeDelayMs / playbackSpeed).toLong()

                    } else {
                        delayMs = 200L
                    }
                } else {
                    delayMs = 1000L
                }
                postDelayed(mUpdateProgressAction, delayMs)
            }
        }
    }

    private fun previous() {
        player?.let {
            val timeline = it.currentTimeline
            if (timeline.isEmpty) return
            val windowIndex = it.currentWindowIndex
            timeline.getWindow(windowIndex, mWindow)
            val previousWindowIndex = it.previousWindowIndex
            if (previousWindowIndex != C.INDEX_UNSET
                    && (it.currentPosition <= MAX_POSITION_FOR_SEEK_TO_PREVIOUS
                            || (mWindow.isDynamic && !mWindow.isSeekable))) {

            } else {

            }
        }
    }

    private fun requestPlayPauseFocus() {
        val playing = isPlaying()

        if (mPlayButton != null && !playing) {
            mPlayButton?.requestFocus()
        } else if (playing && mPauseButton != null) {
            mPauseButton?.requestFocus()
        }

    }

    private fun setButtonEnabled(enabled: Boolean, view: View?) {
        view?.apply {
            isEnabled = enabled
            alpha = if (enabled) 1f else 0.3f
            visibility = View.VISIBLE
        }
    }

    companion object {
        private const val MAX_WINDOWS_FOR_MULTI_WINDOW_TIME_BAR = 100
        private const val MAX_POSITION_FOR_SEEK_TO_PREVIOUS = 3000
        private fun isHandledMediaKey(keyCode: Int): Boolean {
            return (keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD
                    || keyCode == KeyEvent.KEYCODE_MEDIA_REWIND
                    || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                    || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY
                    || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE
                    || keyCode == KeyEvent.KEYCODE_MEDIA_NEXT
                    || keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        }

        private fun canShowMultiWindowTimeBar(timeline: Timeline, window: Timeline.Window): Boolean {

            if (timeline.windowCount > MAX_WINDOWS_FOR_MULTI_WINDOW_TIME_BAR) {
                return false
            }
            val windowCount = timeline.windowCount
            for (i in 0 until windowCount) {
                if (timeline.getWindow(i, window).durationUs == C.TIME_UNSET) {
                    return false
                }
            }
            return true
        }

    }


    interface VisibilityListener {
        fun onVisibilityChange(visibility: Int)
    }

    private inner class ComponetListener : Player.DefaultEventListener() {

    }
}