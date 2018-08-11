package psycho.euphoria.player

import android.app.Activity
import android.content.Context
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.view.View
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.FileDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.video.VideoListener
import kotlinx.android.synthetic.main.activity_player_video.*
import psycho.euphoria.common.extension.*
import psycho.euphoria.common.extension.C
import psycho.euphoria.tools.R
import java.io.File
import java.util.*
import kotlin.math.max
import kotlin.math.round
import android.graphics.RectF
import android.media.AudioManager
import android.opengl.ETC1.getHeight
import android.opengl.ETC1.getWidth
import android.view.MotionEvent
import android.view.TextureView
import kotlin.math.abs


class PlayerActivity : Activity(), TimeBar.OnScrubListener, Player.EventListener, VideoListener, PlaybackPreparer, View.OnLayoutChangeListener, View.OnTouchListener {


    private var mPlayer: SimpleExoPlayer? = null
    private val mHideAction = Runnable { hide() }
    private val mHanlder = Handler()
    private val mStringBuilder = StringBuilder()
    private val mFormatter = Formatter(mStringBuilder)
    private var mShowTimeoutMs = DEFAULT_SHOW_TIMEOUT_MS
    private var mWindow = Timeline.Window()
    private val mUpdateProgressAction = Runnable { updateProgress() }
    private val mControlDispatcher = DefaultControlDispatcher()

    private var mDownX = 0
    private var mDownY = 0
    private lateinit var mAudioManager: AudioManager

    private var mIsAutoPlay = true
    private var mStartPosition = 0L
    private var mStartWindow = 0
    private var mScrubbing = false
    private var mTextureViewRotation = 0
    private var mIsChangeVolume = false
    private var mIsChangePosition = false
    private var mCurrentPosition = 0L
    private var mSreenWidth = 0
    private var mVolume = 0


    private fun bindActions() {
        exo_play.setOnClickListener { it ->
            mPlayer?.let {
                if (it.playbackState == Player.STATE_IDLE) {
                    this.preparePlayback()
                } else if (it.playbackState == Player.STATE_ENDED) {
                    mControlDispatcher.dispatchSeekTo(it, it.currentWindowIndex, C.TIME_UNSET)
                } else {
                    mControlDispatcher.dispatchSetPlayWhenReady(it, true)
                }
            }
            hideController()
        }
        exo_pause.setOnClickListener {
            mControlDispatcher.dispatchSetPlayWhenReady(mPlayer, false)
            hideController()
        }
        exo_next.setOnClickListener {
            next()
            hideController()
        }
        exo_prev.setOnClickListener {
            previous()
            hideController()
        }
        exo_progress.addListener(this)
        root_view.setOnTouchListener(this)
    }

    private fun generateMediaSource(uri: Uri): MediaSource? {
        val files = uri.path.getParentFilePath().listVideoFiles()
        files?.let {
            val mediaSources = arrayOfNulls<MediaSource>(it.size)
            val fileDataSourceFactory = FileDataSourceFactory()
            for (i in 0 until it.size) {
                mediaSources[i] = ExtractorMediaSource.Factory(fileDataSourceFactory).createMediaSource(it[i].toUri())
            }
            if (it.size > 1) {
                val mediaSource = ConcatenatingMediaSource()
                for (i in 0 until it.size) mediaSource.addMediaSource(mediaSources[i])
                return mediaSource
            } else {
                return mediaSources[0]
            }
        }
        return null
    }

    private fun hide() {
        if (controller.visibility == View.VISIBLE) {
            controller.visibility = View.GONE
            mHanlder.removeCallbacks(mUpdateProgressAction)
            mHanlder.removeCallbacks(mHideAction)
        }
    }

    private fun hideController() {
        mHanlder.postDelayed(mHideAction, mShowTimeoutMs)
    }

    private fun initialize() {
        bindActions()
        mSreenWidth = resources.displayMetrics.widthPixels
        mAudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    private fun initializePlayer() {
        if (mPlayer == null) {
            mPlayer = ExoPlayerFactory.newSimpleInstance(this, DefaultTrackSelector()).also {
                it.addListener(this)
                it.playWhenReady = true
                it.videoComponent?.apply {
                    setVideoTextureView(texture_view)
                    addVideoListener(this@PlayerActivity)
                }
                val testMp4 = File(File(Environment.getExternalStorageDirectory(), "1"), "1.mp4")
                val mediaSource = generateMediaSource(testMp4.toUri())
                it.prepare(mediaSource)
            }
        }
        updateAll()
    }

    private fun isPlaying(): Boolean {
        mPlayer?.let {
            return it.playbackState != Player.STATE_ENDED
                    && it.playbackState != Player.STATE_IDLE
                    && it.playWhenReady
        } ?: run { return false }
    }

    private fun next() {
        mPlayer?.apply {
            if (currentTimeline.isEmpty) return
            if (nextWindowIndex != C.INDEX_UNSET) {
                seekTo(nextWindowIndex, C.TIME_UNSET)
            } else {
                seekTo(currentWindowIndex, C.TIME_UNSET)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inject UI immediately
        setContentView(R.layout.activity_player_video)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(arrayOf("android.permission.ACCESS_NETWORK_STATE",
                    "android.permission.WAKE_LOCK",
                    "android.permission.WRITE_EXTERNAL_STORAGE"), 100)
        } else initialize()
    }

    override fun onLayoutChange(view: View, p1: Int, p2: Int, p3: Int, p4: Int, p5: Int, p6: Int, p7: Int, p8: Int) {
        applyTextureViewRotation(view as TextureView, mTextureViewRotation)
    }

    override fun onLoadingChanged(change: Boolean) {
    }

    override fun onPause() {
        super.onPause()
        C.atMost(23, { releasePlayer() }, {})
    }

    override fun onPlaybackParametersChanged(p0: PlaybackParameters?) {
    }

    override fun onPlayerError(error: ExoPlaybackException) {
        Log.e(TAG, "onPlayerError", error)
        exo_error_message.text = error.message
    }

    override fun onPlayerStateChanged(p0: Boolean, p1: Int) {
        updatePlayPauseButton()
        updateProgress()
    }

    override fun onPositionDiscontinuity(p0: Int) {
        mPlayer?.let {
            if (it.playbackError == null) updateStartPosition()
        }
        updateProgress()
        updateNavigation()
    }

    override fun onRenderedFirstFrame() {
    }

    override fun onRepeatModeChanged(p0: Int) {
        updateNavigation()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>?, grantResults: IntArray?) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.e(TAG, "onRequestPermissionsResult ${permissions.dump()} ${grantResults.dump()}")
        initialize()
    }

    override fun onResume() {
        super.onResume()
        C.atMost(23, { if (mPlayer == null) initializePlayer() }, {})
    }

    override fun onScrubMove(timeBar: TimeBar, position: Long) {
    }

    override fun onScrubStart(timeBar: TimeBar, position: Long) {
        mHanlder.removeCallbacks(mHideAction)
        mScrubbing = true
    }

    override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
        mScrubbing = false
        seekToTimeBarPosition(position)
        hideController()
    }

    override fun onSeekProcessed() {
    }

    override fun onShuffleModeEnabledChanged(p0: Boolean) {
        updateNavigation()
    }

    override fun onStart() {
        super.onStart()
        C.more(23, { initializePlayer() }, {})
    }

    override fun onStop() {
        super.onStop()
        C.more(23, { releasePlayer() }, {})
    }

    override fun onTimelineChanged(p0: Timeline?, p1: Any?, p2: Int) {
        updateProgress()
        updateNavigation()
    }

    override fun onTouch(view: View?, event: MotionEvent): Boolean {
        val x = event.x.toInt()
        val y = event.y.toInt()
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mDownX = x
                mDownY = y
                mIsChangeVolume = false
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = x - mDownX
                val dy = y - mDownY
                var adx = abs(dx)
                var ady = abs(dy)
                if (!mIsChangeVolume) {
                    if (adx > THRESHOLD || ady > THRESHOLD) {
                        if (adx >= THRESHOLD) {
                            mIsChangePosition = true
                            mCurrentPosition = mPlayer?.contentPosition ?: 0L
                        } else {
                            if (mDownX > mSreenWidth * 0.5f) {
                                mIsChangeVolume = true
                                mVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                            }
                        }
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                if (mIsChangeVolume) {
                } else {
                    show()
                }
            }
        }
        return true
    }

    override fun onTracksChanged(p0: TrackGroupArray?, p1: TrackSelectionArray?) {
    }

    override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
        Log.e(TAG, "width => ${width} \nheight => ${height} \nunappliedRotationDegrees => ${unappliedRotationDegrees} \npixelWidthHeightRatio => ${pixelWidthHeightRatio} \n")
        var ratio = if (height == 0 || width == 0) 1f else (width * pixelWidthHeightRatio) / height
        if (unappliedRotationDegrees == 90 || unappliedRotationDegrees == 270) {
            ratio = 1 / ratio
        }
        if (mTextureViewRotation != 0) {
            texture_view.removeOnLayoutChangeListener(this)
        }
        mTextureViewRotation = unappliedRotationDegrees
        if (mTextureViewRotation != 0) {
            texture_view.addOnLayoutChangeListener(this)
        }
        applyTextureViewRotation(texture_view, mTextureViewRotation)
        exo_content_frame.videoAspectRatio = ratio
        //exo_content_frame.setAspectRatio(ratio)
    }

    override fun preparePlayback() {
    }

    private fun previous() {
        mPlayer?.apply {
            if (currentTimeline == null) return
            currentTimeline.getWindow(currentWindowIndex, mWindow)
            if (previousWindowIndex != C.INDEX_UNSET
                    && currentPosition <= MAX_POSITION_FOR_SEEK_TO_PREVIOUS
                    || (mWindow.isDynamic && !mWindow.isSeekable)) {
                this@PlayerActivity.seekTo(previousWindowIndex, C.TIME_UNSET)
            } else this@PlayerActivity.seekTo(0L)
        }
    }

    private fun releasePlayer() {
    }

    private fun requestPlayPauseFocus() {
        val playing = isPlaying()
        if (!playing) {
            exo_play.requestFocus()
        } else if (playing) {
            exo_pause.requestFocus()
        }
    }

    private fun seekTo(windowIndex: Int, position: Long) {
        val dispatched = mControlDispatcher.dispatchSeekTo(mPlayer, windowIndex, position)
        if (!dispatched) updateProgress()
    }

    private fun seekTo(position: Long) {
        mPlayer?.apply {
            seekTo(currentWindowIndex, position)
        }
    }

    private fun seekToTimeBarPosition(position: Long) {
        mPlayer?.let {
            var windowIndex = 0
            var positionMs = position
            val timeline = it.currentTimeline
            if (!timeline.isEmpty) {
                val windowcount = timeline.windowCount
                while (true) {
                    val windowDurationMs = timeline.getWindow(windowIndex, mWindow).durationMs
                    if (positionMs < windowDurationMs) {
                        break
                    } else if (windowIndex == windowcount - 1) {
                        positionMs = windowDurationMs
                        break
                    }
                    positionMs -= windowDurationMs
                    windowIndex++
                }
            } else {
                windowIndex = it.currentWindowIndex
            }
            seekTo(windowIndex, positionMs)
        }
    }

    private fun setButtonEnabled(enabled: Boolean, view: View?) {
        view?.apply {
            isEnabled = enabled
            alpha = if (enabled) 1f else 0.3f
            visibility = View.VISIBLE
        }
    }

    private fun show() {
        if (controller.visibility != View.VISIBLE) {
            controller.visibility = View.VISIBLE
            updateAll()
            requestPlayPauseFocus()
        }
        hideController()
    }

    private fun updateAll() {
        updatePlayPauseButton()
        updateProgress()
        updateNavigation()
    }

    private fun updateNavigation() {
        if (controller.visibility != View.VISIBLE) return
        val timeline = mPlayer?.currentTimeline
        val haveNonEmptyTimeline = timeline != null && !timeline.isEmpty
        var isSeekable = false
        var enablePrevious = false
        var enableNext = false
        if (haveNonEmptyTimeline) {
            mPlayer?.let {
                timeline?.getWindow(it.currentWindowIndex, mWindow)
                isSeekable = mWindow.isSeekable
                enablePrevious = isSeekable || !mWindow.isDynamic || it.previousWindowIndex != C.INDEX_UNSET
                enableNext = mWindow.isDynamic || it.nextWindowIndex != C.INDEX_UNSET
            }
        }
        setButtonEnabled(enablePrevious, exo_prev)
        setButtonEnabled(enableNext, exo_next)
        setButtonEnabled(isSeekable, exo_ffwd)
        setButtonEnabled(isSeekable, exo_rew)
        exo_progress.isEnabled = isSeekable
    }

    private fun updatePlayPauseButton() {
        var requestFocus = false
        val playing = isPlaying()
        Log.e(TAG, "updatePlayPauseButton $playing")
        exo_play.visibility = if (playing) View.GONE else View.VISIBLE
        requestFocus = requestFocus or (playing && exo_play.isFocused)
        exo_pause.visibility = if (!playing) View.GONE else View.VISIBLE
        requestFocus = requestFocus or (!playing && exo_pause.isFocused)
        if (requestFocus) requestPlayPauseFocus()
    }

    private fun updateProgress() {
        if (controller.visibility != View.VISIBLE) return
        var position = 0L
        var duration = 0L
        var playbackState = 0
        mPlayer?.let {
            val timeline = it.currentTimeline
            if (!timeline.isEmpty)
                timeline.getWindow(it.currentWindowIndex, mWindow)
            duration = mWindow.durationUs.usToMs()
            position = it.currentPosition
            playbackState = it.playbackState
        }
        exo_position.text = Util.getStringForTime(mStringBuilder, mFormatter, position)
        exo_duration.text = Util.getStringForTime(mStringBuilder, mFormatter, duration) // duration.getStringForTime(mStringBuilder, mFormatter)
        exo_progress.duration = duration
        exo_progress.position = position
        mHanlder.removeCallbacks(mUpdateProgressAction)
        if (playbackState != Player.STATE_IDLE && playbackState != Player.STATE_ENDED) {
            var delayMs = 0L
            mPlayer?.let {
                if (it.playWhenReady && playbackState == Player.STATE_READY) {
                    val playbackSpeed = it.playbackParameters.speed
                    if (playbackSpeed <= 0.1f) {
                        delayMs = 1000L
                    } else if (playbackSpeed < 5f) {
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
                mHanlder.postDelayed(mUpdateProgressAction, delayMs)
            }
        }
    }

    private fun updateStartPosition() {
        mPlayer?.apply {
            mIsAutoPlay = playWhenReady
            mStartPosition = max(0, contentPosition)
            mStartWindow = currentWindowIndex
        }
    }

    companion object {
        private const val MAX_POSITION_FOR_SEEK_TO_PREVIOUS = 3000
        private const val DEFAULT_SHOW_TIMEOUT_MS = 5000L
        private const val THRESHOLD = 80
        private const val TAG = "PlayerActivity"
        private fun applyTextureViewRotation(textureView: TextureView, textureViewRotation: Int) {
            val textureViewWidth = textureView.width.toFloat()
            val textureViewHeight = textureView.height.toFloat()
            if (textureViewWidth == 0f || textureViewHeight == 0f || textureViewRotation == 0) {
                textureView.setTransform(null)
            } else {
                val transformMatrix = Matrix()
                val pivotX = textureViewWidth / 2
                val pivotY = textureViewHeight / 2
                transformMatrix.postRotate(textureViewRotation.toFloat(), pivotX, pivotY)

                // After rotation, scale the rotated texture to fit the TextureView size.
                val originalTextureRect = RectF(0f, 0f, textureViewWidth, textureViewHeight)
                val rotatedTextureRect = RectF()
                transformMatrix.mapRect(rotatedTextureRect, originalTextureRect)
                transformMatrix.postScale(
                        textureViewWidth / rotatedTextureRect.width(),
                        textureViewHeight / rotatedTextureRect.height(),
                        pivotX,
                        pivotY)
                textureView.setTransform(transformMatrix)
            }
        }
    }
}