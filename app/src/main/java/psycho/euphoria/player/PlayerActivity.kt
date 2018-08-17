package psycho.euphoria.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.*
import com.google.android.exoplayer2.text.Cue
import com.google.android.exoplayer2.text.TextOutput
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.FileDataSourceFactory
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.video.VideoListener
import kotlinx.android.synthetic.main.activity_player_video.*
import kotlinx.android.synthetic.main.item_file.*
import psycho.euphoria.common.C
import psycho.euphoria.common.CustomActivity
import psycho.euphoria.common.Services
import psycho.euphoria.common.Services.navigationBarHeight
import psycho.euphoria.common.Services.navigationBarWidth
import psycho.euphoria.common.Services.orientation
import psycho.euphoria.common.calculateScreenOrientation
import psycho.euphoria.common.extension.*
import psycho.euphoria.tools.R
import java.io.File
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.round

class PlayerActivity : CustomActivity(), TimeBar.OnScrubListener,
        Player.EventListener, VideoListener, PlaybackPreparer, View.OnLayoutChangeListener,
        View.OnTouchListener,
        TextOutput {

    private lateinit var mTextureView: TextureView
    private val mBookmarker = Bookmarker(this)
    private val mControlDispatcher = DefaultControlDispatcher()
    private val mHanlder = Handler()
    private val mHideAction = Runnable { hide() }
    private val mStringBuilder = StringBuilder()
    private val mFormatter = Formatter(mStringBuilder)
    private val mUpdateProgressAction = Runnable { updateProgress() }
    private var mCurrentPosition = 0L
    private var mDownX = 0
    private var mDownY = 0
    private var mFiles: List<File>? = null
    private var mIsAutoPlay = true
    private var mIsChangeVolume = false
    private var mIsChangingPosition = false
    private var mMediaSource: MediaSource? = null
    private var mPlayer: SimpleExoPlayer? = null
    private var mScreenHeight = 0
    private var mScreenWidth = 0
    private var mScrubbing = false
    private var mSeekPosition = 0L
    private var mShowTimeoutMs = DEFAULT_SHOW_TIMEOUT_MS
    private var mStartPosition = 0L
    private var mStartWindow = 0
    private var mTextureViewRotation = 0
    private var mVolume = 0
    private var mWindow = Timeline.Window()
//    private var mOrientation = 0
//    private val mOrientationEventListener = object : OrientationEventListener(this) {
//        override fun onOrientationChanged(degress: Int) {
//            mOrientation = degress
//        }
//
//    }
    //private val mTracker = Tracker("PlayerActivity")


    private fun bindActions() {
        //mTracker.e("[bindActions]")
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

    private fun deleteVideo(path: String?) {
        //mTracker.e("[deleteVideo]")
        mPlayer?.let {
            val path = path ?: return
            val files = mFiles ?: return
            var index = it.nextWindowIndex
            val file: File
            if (index != -1) {
                file = files[index]
            } else if (files.size > 1) {
                index = 0
                file = files[index]
            } else {
                file = File(path)
                deleteFile(file, false) {
                    toast(path)
                    setResult(Activity.RESULT_OK)
                    finish()
                    return@deleteFile
                }
            }
            deleteFile(File(path), false) {
                toast(path)
                setResult(Activity.RESULT_OK)
            }
            mMediaSource = generateMediaSource(file.toUri())
            it.prepare(mMediaSource)
            it.seekTo(index, C.TIME_UNSET)
        }
    }

    private fun fastForward() {
        //mTracker.e("[fastForward]")
        mPlayer?.apply {
            val speed = playbackParameters.speed
            val targetSpeed = ((speed / 5 + 1) * 5)
            toast("[fastForward] $targetSpeed")
            playbackParameters = PlaybackParameters(targetSpeed, targetSpeed)
        }
    }

    private fun generateMediaSource(uri: Uri): MediaSource? {
        //mTracker.e("[generateMediaSource]")
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
                val m = ExtractorMediaSource.Factory(fileDataSourceFactory).createMediaSource(u)
                val sm = buildSubtitleMediaSource(it[i])
                if (sm != null) {
                    Log.e(TAG, "[generateMediaSource] Create Subtitle Source ${it[i]}")
                    mediaSources[i] = MergingMediaSource(m, sm)
                } else {
                    mediaSources[i] = m
                }
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

    private fun buildSubtitleMediaSource(file: File): SingleSampleMediaSource? {
        val subtitleFile = file.changeExtension("srt")
        //Log.e(TAG, "[buildSubtitleMediaSource] ${subtitleFile.absolutePath}")
        if (subtitleFile.exists()) {
            return SingleSampleMediaSource.Factory(FileDataSourceFactory()).createMediaSource(subtitleFile.toUri(),
                    SRT_FORMAT, C.TIME_UNSET)
        }
        return null
    }

    private fun getCurrentUri(): String? {
        //mTracker.e("[getCurrentUri]")
        mFiles?.let {
            if (mStartWindow.inRange(it))
                return it[mStartWindow].absolutePath
        }
        return null
    }

    private fun hide() {
        //mTracker.e("[hide]")
        if (controller.visibility == View.VISIBLE) {
            controller.visibility = View.GONE
            mHanlder.removeCallbacks(mUpdateProgressAction)
            mHanlder.removeCallbacks(mHideAction)
            hideSystemUI(false)
//            if (mIsHasBar) {
//                controller.setPadding(0, 0, 0,0)
//               Log.e(TAG, "[hide] ${controller.paddingLeft} ${controller.paddingRight} ${controller.paddingTop} ${controller.paddingBottom}")
//
//            }
        }
    }

    private fun hideController() {
        //mTracker.e("[hideController]")
        mHanlder.postDelayed(mHideAction, mShowTimeoutMs)
    }

    private fun initialize() {
        //mTracker.e("[initialize]")
        bindActions()
        mScreenWidth = widthPixels
        mScreenHeight = heightPixels
    }

    private fun initializePlayer() {
        //mTracker.e("[initializePlayer]")
        hideController()
        if (mPlayer == null) {
            mPlayer = ExoPlayerFactory.newSimpleInstance(this, DefaultTrackSelector()).also {
                it.addListener(this)
                it.playWhenReady = true
                it.videoComponent?.apply {
                    setVideoTextureView(mTextureView)
                    addVideoListener(this@PlayerActivity)
                }
                it.textComponent?.addTextOutput(this)
                // val testMp4 = File(File(Environment.getExternalStorageDirectory(), "1"), "1.mp4")
                val mediaSource = generateMediaSource(intent.data)
                mMediaSource = mediaSource
                it.prepare(mediaSource)
            }
            if (mStartWindow > 0) {
                seekTo(mStartWindow, C.TIME_UNSET)
            }
        }
        updateAll()
    }

    private fun isPlaying(): Boolean {
        //mTracker.e("[isPlaying]")
        mPlayer?.let {
            return it.playbackState != Player.STATE_ENDED
                    && it.playbackState != Player.STATE_IDLE
                    && it.playWhenReady
        } ?: run { return false }
    }

    // Jump to the next video in the playlist
    private fun next() {
        //mTracker.e("[next]")
        mPlayer?.apply {
            if (currentTimeline.isEmpty) return
            if (nextWindowIndex != C.INDEX_UNSET) {
                Log.e(TAG, "[next] Seek to next: currentWindowIndex $currentWindowIndex,nextWindowIndex $nextWindowIndex")
                seekTo(nextWindowIndex, C.TIME_UNSET)
            } else {
                seekTo(currentWindowIndex, C.TIME_UNSET)
                Log.e(TAG, "[next] Seek to current: currentWindowIndex $currentWindowIndex,nextWindowIndex $nextWindowIndex")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        //mTracker.e("[onCreate]")
        Services.context = this.applicationContext
        super.onCreate(savedInstanceState)
        // Inject UI immediately
        setContentView(R.layout.activity_player_video)
        mTextureView = TextureView(this)
        mTextureView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        exo_content_frame.addView(mTextureView, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(arrayOf("android.permission.ACCESS_NETWORK_STATE",
                    "android.permission.WAKE_LOCK",
                    "android.permission.WRITE_EXTERNAL_STORAGE"), 100)
        } else initialize()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        //mTracker.e("[onCreateOptionsMenu]")
        menuInflater.inflate(R.menu.menu_video, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onCues(cues: MutableList<Cue>?) {
        Log.e(TAG, "[onCues] ${cues?.size}")
        exo_subtitles.setCues(cues)
    }

    override fun onLayoutChange(view: View, p1: Int, p2: Int, p3: Int, p4: Int, p5: Int, p6: Int, p7: Int, p8: Int) {
        //mTracker.e("[onLayoutChange]")
        applyTextureViewRotation(view as TextureView, mTextureViewRotation)
    }

    override fun onLoadingChanged(change: Boolean) {
        //mTracker.e("[onLoadingChanged]")
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        //mTracker.e("[onOptionsItemSelected]")
        when (item.itemId) {
            R.id.action_landspace -> if (calculateScreenOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            R.id.action_delete -> deleteVideo(getCurrentUri())
            R.id.action_rename -> renameVideo(getCurrentUri())

        }

        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        //mTracker.e("[onPause]")
        super.onPause()
        C.atMost(23, { releasePlayer() }, {})
    }

    override fun onPlaybackParametersChanged(p0: PlaybackParameters?) {
        //mTracker.e("[onPlaybackParametersChanged]")
    }

    override fun onPlayerError(error: ExoPlaybackException) {
        //mTracker.e("[onPlayerError]")
        exo_error_message.text = error.message
    }

    override fun onPlayerStateChanged(p0: Boolean, p1: Int) {
        //mTracker.e("[onPlayerStateChanged]")
        updatePlayPauseButton()
        updateProgress()
    }

    override fun onPositionDiscontinuity(p0: Int) {
        //mTracker.e("[onPositionDiscontinuity]")
        mPlayer?.let {
            if (it.playbackError == null) updateStartPosition()
        }
        updateProgress()
        updateNavigation()
    }

    override fun onRenderedFirstFrame() {
        //mTracker.e("[onRenderedFirstFrame]")
    }

    override fun onRepeatModeChanged(p0: Int) {
        //mTracker.e("[onRepeatModeChanged]")
        updateNavigation()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        //mTracker.e("[onRequestPermissionsResult]")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        initialize()
    }

    override fun onResume() {
        //mTracker.e("[onResume]")
        super.onResume()
        C.atMost(23, { if (mPlayer == null) initializePlayer() }, {})
    }

    override fun onScrubMove(timeBar: TimeBar, position: Long) {
        //mTracker.e("[onScrubMove]")
    }

    override fun onScrubStart(timeBar: TimeBar, position: Long) {
        //mTracker.e("[onScrubStart]")
        mHanlder.removeCallbacks(mHideAction)
        mScrubbing = true
    }

    override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
        //mTracker.e("[onScrubStop]")
        mScrubbing = false
        seekToTimeBarPosition(position)
        hideController()
    }

    override fun onSeekProcessed() {
        //mTracker.e("[onSeekProcessed]")
    }

    override fun onShuffleModeEnabledChanged(p0: Boolean) {
        //mTracker.e("[onShuffleModeEnabledChanged]")
        /**
         * Called when the value of {@link #getShuffleModeEnabled()} changes.
         *
         * @param shuffleModeEnabled Whether shuffling of windows is enabled.
         */
        updateNavigation()
    }

    override fun onStart() {
        //mTracker.e("[onStart]")
        super.onStart()
        C.more(23, { initializePlayer() }, {})
    }

    override fun onStop() {
        //mTracker.e("[onStop]")
        super.onStop()
        C.more(23, { releasePlayer() }, {})
    }

    override fun onTimelineChanged(p0: Timeline?, p1: Any?, p2: Int) {
        //mTracker.e("[onTimelineChanged]")
        updateProgress()
        updateNavigation()
        seekToLastedState()
    }

    override fun onTouch(view: View?, event: MotionEvent): Boolean {
        //mTracker.e("[onTouch]")
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
                    mSeekPosition = mCurrentPosition + abs(delta).clamp(0L, MAX_SEEK_DELTA) * (if (delta < 0L) -1L else 1L)
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
        //mTracker.e("[onTracksChanged]")
        seekToLastedState()
    }

    override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
        //mTracker.e("[onVideoSizeChanged]")
        var ratio = if (height == 0 || width == 0) 1f else (width * pixelWidthHeightRatio) / height
        if (unappliedRotationDegrees == 90 || unappliedRotationDegrees == 270) {
            ratio = 1 / ratio
        }
        if (mTextureViewRotation != 0) {
            mTextureView.removeOnLayoutChangeListener(this)
        }
        mTextureViewRotation = unappliedRotationDegrees
        if (mTextureViewRotation != 0) {
            mTextureView.addOnLayoutChangeListener(this)
        }
        applyTextureViewRotation(mTextureView, mTextureViewRotation)
        exo_content_frame.videoAspectRatio = ratio
        //exo_content_frame.setAspectRatio(ratio)
    }

    override fun preparePlayback() {
        //mTracker.e("[preparePlayback]")
    }

    // Jump to the previous video of the playlist
    private fun previous() {
        //mTracker.e("[previous]")
        mPlayer?.apply {
            if (currentTimeline == null) return
            currentTimeline.getWindow(currentWindowIndex, mWindow)
            if (previousWindowIndex != C.INDEX_UNSET
                    && currentPosition <= MAX_POSITION_FOR_SEEK_TO_PREVIOUS
                    || (mWindow.isDynamic && !mWindow.isSeekable)) {
                //Log.e(TAG, "[previous] Seek to previous: previousWindowIndex,$previousWindowIndex currentWindowIndex,$currentWindowIndex")
                this@PlayerActivity.seekTo(previousWindowIndex, C.TIME_UNSET)
            } else {
                //Log.e(TAG, "[previous] Seek to the starting position")
                this@PlayerActivity.seekTo(0L)
            }
        }
    }

    private fun releasePlayer() {
        //mTracker.e("[releasePlayer]")
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

    private fun renameVideo(path: String?) {
        mPlayer?.let {
            val path = path ?: return
            dialog(this, path.getFilenameFromPath(), getString(R.string.menu_rename_file), true) {
                renameFile(path, path.getParentPath() + File.separator + it.toString()) {
                    if (it) {
                        mMediaSource = generateMediaSource(File(path).toUri())
                        mPlayer?.prepare(mMediaSource)
                        mPlayer?.seekTo(mStartWindow, C.TIME_UNSET)
                    } else {
                        //
                        toast("Renaming the file failed : ${path.getFilenameFromPath()}")
                    }
                }
            }
        }
    }

    private fun requestPlayPauseFocus() {
        //mTracker.e("[requestPlayPauseFocus]")
        val playing = isPlaying()
        if (!playing) {
            exo_play.requestFocus()
        } else if (playing) {
            exo_pause.requestFocus()
        }
    }

    private fun rewind() {
        //mTracker.e("[rewind]")
        mPlayer?.apply {
            val speed = playbackParameters.speed
            var targetSpeed = 0f
            if (speed <= 1.0)
                targetSpeed = speed / 2f
            else
                targetSpeed = ((speed - 5) - speed % 5)
            if (targetSpeed < 1f) targetSpeed = 1f
            toast("[frewind] $targetSpeed")
            playbackParameters = PlaybackParameters(targetSpeed, targetSpeed)
        }


    }

    private fun seekTo(windowIndex: Int, position: Long) {
        //mTracker.e("[seekTo]")
        mPlayer?.let {
            val dispatched = mControlDispatcher.dispatchSeekTo(it, windowIndex, position)
            if (!dispatched) updateProgress()
        }
    }

    private fun seekTo(position: Long) {
        //mTracker.e("[seekTo]")
        mPlayer?.apply {
            seekTo(currentWindowIndex, position)
        }
    }

    private fun seekToLastedState() {
        getCurrentUri()?.let {
            supportActionBar?.title = it.getFilenameFromPath()
            val position = mBookmarker.getBookmark(it)
            // Log.e(TAG, "onTimelineChanged $it $position $mStartWindow")
            position?.let {
                seekTo(it)
            }
        }
    }

    private fun seekToTimeBarPosition(position: Long) {
        //mTracker.e("[seekToTimeBarPosition]")
        mPlayer?.let {
            seekTo(it.currentWindowIndex, position)
        }
    }

    private fun setButtonEnabled(enabled: Boolean, view: View?) {
        //mTracker.e("[setButtonEnabled]")
        view?.apply {
            isEnabled = enabled
            alpha = if (enabled) 1f else 0.3f
            visibility = View.VISIBLE
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        Log.i(TAG, "[onConfigurationChanged]: ")
    }

    private fun show() {
        //mTracker.e("[show]")
        if (controller.visibility != View.VISIBLE) {
            controller.visibility = View.VISIBLE
            updateAll()
            requestPlayPauseFocus()
            showSystemUI(true)
            if (mIsHasBar) {
                var left = 0
                val top = 0
                var right = 0
                var bottom = 0
                val orientation = calculateScreenOrientation()

                if (orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                    bottom += navigationBarHeight
                } else if (orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                    right += navigationBarWidth
                    Log.i(TAG, "[show]:$right ")
                } else if (orientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
                    left += navigationBarWidth
                }
                controller.setPadding(left, top, right, bottom)
                //Log.e(TAG, "[show] $left $top $right $bottom")
            }
        }
        hideController()
    }

    private fun updateAll() {
        //mTracker.e("[updateAll]")
        updatePlayPauseButton()
        updateProgress()
        updateNavigation()
    }

    private fun updateNavigation() {
        //mTracker.e("[updateNavigation]")
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
        //mTracker.e("[updatePlayPauseButton]")
        var requestFocus = false
        val playing = isPlaying()
        exo_play.visibility = if (playing) View.GONE else View.VISIBLE
        requestFocus = requestFocus or (playing && exo_play.isFocused)
        exo_pause.visibility = if (!playing) View.GONE else View.VISIBLE
        requestFocus = requestFocus or (!playing && exo_pause.isFocused)
        if (requestFocus) requestPlayPauseFocus()
    }

    private fun updateProgress() {
        //mTracker.e("[updateProgress]")
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
        //mTracker.e("[updateStartPosition]")
        mPlayer?.apply {
            mIsAutoPlay = playWhenReady
            mStartPosition = max(0, contentPosition)
            mStartWindow = currentWindowIndex
        }
    }

    companion object {
        private const val MAX_SEEK_DELTA = 30000L
        private const val MAX_POSITION_FOR_SEEK_TO_PREVIOUS = 3000
        private const val DEFAULT_SHOW_TIMEOUT_MS = 5000L
        private const val THRESHOLD = 80
        private const val TAG = "PlayerActivity"
        private val SRT_FORMAT by lazy {
            Format.createTextSampleFormat(null, MimeTypes.APPLICATION_SUBRIP,
                    Format.NO_VALUE, "en")
        }

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