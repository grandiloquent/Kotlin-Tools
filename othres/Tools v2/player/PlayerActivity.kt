package psycho.euphoria.player

import android.app.Activity
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
import android.view.MotionEvent
import android.view.TextureView
import kotlin.math.abs
import kotlin.math.min


class PlayerActivity : Activity(), TimeBar.OnScrubListener, Player.EventListener, VideoListener, PlaybackPreparer, View.OnLayoutChangeListener, View.OnTouchListener {

    private val mBookmarker = Bookmarker(this)
    private val mControlDispatcher = DefaultControlDispatcher()
    private val mStringBuilder = StringBuilder()
    private val mFormatter = Formatter(mStringBuilder)
    private val mHanlder = Handler()
    private val mHideAction = Runnable { hide() }
    private val mUpdateProgressAction = Runnable { updateProgress() }
    private var mCurrentPosition = 0L
    private var mDownX = 0
    private var mDownY = 0
    private var mFiles: List<File>? = null
    private var mIsAutoPlay = true
    private var mIsChangeVolume = false
    private var mMediaSource: MediaSource? = null
    private var mPlayer: SimpleExoPlayer? = null
    private var mScreenHeight = 0
    private var mScrubbing = false
    private var mShowTimeoutMs = DEFAULT_SHOW_TIMEOUT_MS
    private var mScreenWidth = 0
    private var mStartPosition = 0L
    private var mStartWindow = 0
    private var mTextureViewRotation = 0
    private var mVolume = 0
    private var mWindow = Timeline.Window()
    private var mIsChangingPosition = false
    private var mSeekPosition = 0L

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
        exo_ffwd.setOnClickListener { fastForward() }
        exo_rew.setOnClickListener { rewind() }
        root_view.setOnTouchListener(this)
    }

    private fun generateMediaSource(uri: Uri): MediaSource? {
        val files = uri.path.getParentFilePath().listVideoFiles()
        mFiles = files
        files?.let {
            val mediaSources = arrayOfNulls<MediaSource>(it.size)
            val fileDataSourceFactory = FileDataSourceFactory()
            for (i in 0 until it.size) {
                val u = it[i].toUri()
                if (uri == u) {
                    mStartWindow = i
                }
                mediaSources[i] = ExtractorMediaSource.Factory(fileDataSourceFactory).createMediaSource(u)
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

    private fun getCurrentUri(): String? {
        mFiles?.let {
            if (mStartWindow.inRange(it))
                return it[mStartWindow].absolutePath
        }
        return null
    }

    private fun hide() {
        if (controller.visibility == View.VISIBLE) {
            controller.visibility = View.GONE
            mHanlder.removeCallbacks(mUpdateProgressAction)
            mHanlder.removeCallbacks(mHideAction)
            hideSystemUI(true)
        }
    }

    private fun hideController() {
        mHanlder.postDelayed(mHideAction, mShowTimeoutMs)
    }

    private fun initialize() {
        bindActions()
        mScreenWidth = widthPixels
        mScreenHeight = heightPixels
    }

    private fun initializePlayer() {
        hideController()
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
            if (mStartWindow > 0) {
                seekTo(mStartWindow, C.TIME_UNSET)
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
        Services.context = this.applicationContext
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

        getCurrentUri()?.let {
            val position = mBookmarker.getBookmark(it)
            Log.e(TAG, "onTimelineChanged $it $position $mStartWindow")
            position?.let {
                seekTo(it)
            }
        }
    }

    override fun onTouch(view: View?, event: MotionEvent): Boolean {
        val x = event.x.toInt()
        val y = event.y.toInt()
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mDownX = x
                mDownY = y
                mIsChangeVolume = false
                mIsChangingPosition = false
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = x - mDownX
                var dy = y - mDownY
                var adx = abs(dx)
                var ady = abs(dy)
                if (!mIsChangingPosition && !mIsChangeVolume) {
                    if (adx > THRESHOLD || ady > THRESHOLD) {
                        if (adx >= THRESHOLD) {
                            mIsChangingPosition = true
                            mCurrentPosition = mPlayer?.contentPosition ?: 0L
                        } else {
                            if (mDownX > mScreenWidth * 0.5f) {
                                mIsChangeVolume = true
                                mVolume = Services.musicVolume
                            }
                        }
                    }
                }
                if (mIsChangeVolume) {
                    dy = -dy
                    val max = Services.maxMusicVolume
                    val dv = max * dy * 3 / mScreenHeight

                    Services.musicVolume = mVolume + dv
                }
                if (mIsChangingPosition) {
                    val totalDuration = mPlayer?.duration ?: 0L
                    val delta = dx * totalDuration / mScreenHeight;
                    // Limit the maximum value to 30 seconds
                    mSeekPosition = mCurrentPosition + min(delta, 30000L)
                    if (mSeekPosition > totalDuration) {
                        mSeekPosition = totalDuration
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                if (mIsChangingPosition) {

                    seekTo(mSeekPosition)
                } else {
                    show()
                }
            }
        }
        return true
    }

    override fun onTracksChanged(p0: TrackGroupArray?, p1: TrackSelectionArray?) {
        getCurrentUri()?.let {
            val position = mBookmarker.getBookmark(it)
            Log.e(TAG, "onTimelineChanged $it $position $mStartWindow")
            position?.let {
                seekTo(it)
            }
        }
    }

    override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {

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
        updateStartPosition()
        mPlayer?.let {
            getCurrentUri()?.let {
                if (mStartPosition > 0L) {
                    Log.e(TAG, "releasePlayer $it,$mStartPosition")
                    mBookmarker.setBookmark(it, mStartPosition)
                }
            }
            it.release()
            mPlayer = null
            mMediaSource = null
        }
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
        mPlayer?.let {
            val dispatched = mControlDispatcher.dispatchSeekTo(it, windowIndex, position)
            if (!dispatched) updateProgress()
        }
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
            showSystemUI(true)
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

    private fun rewind() {
        mPlayer?.apply {
            val speed = playbackParameters.speed
            var targetSpeed = 0f
            if (speed <= 1.0)
                targetSpeed = speed / 2f
            else
                targetSpeed = ((speed - 5) - speed % 5)
            if (targetSpeed < 1f) targetSpeed = 1f
            playbackParameters = PlaybackParameters(targetSpeed, targetSpeed)

        }
    }

    private fun fastForward() {
        mPlayer?.apply {
            val speed = playbackParameters.speed
            val targetSpeed = ((speed / 5 + 1) * 5).toFloat()
            playbackParameters = PlaybackParameters(targetSpeed, targetSpeed)
        }
    }


    private fun updatePlayPauseButton() {
        var requestFocus = false
        val playing = isPlaying()

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