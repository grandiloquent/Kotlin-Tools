package psycho.euphoria.tools.videos

import android.app.Activity
import android.content.res.Configuration
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.view.*
import android.widget.SeekBar
import android.widget.Toast
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.FileDataSourceFactory
import com.google.android.exoplayer2.video.VideoListener
import kotlinx.android.synthetic.main.activity_video.*
import psycho.euphoria.common.extension.beGone
import psycho.euphoria.common.extension.beInvisible
import psycho.euphoria.common.extension.beVisible
import psycho.euphoria.tools.R
import psycho.euphoria.tools.commons.*
import java.io.File
import kotlin.math.abs
import kotlin.math.min

class VideoActivity : CustomActivity(), TextureView.SurfaceTextureListener, SeekBar.OnSeekBarChangeListener, View.OnTouchListener {


    private val mHidePauseHandler: Handler = Handler()
    private val mTimeHandler: Handler = Handler()
    private var mCurrTime = 0
    private var mDuration = 0
    private var mExoPlayer: SimpleExoPlayer? = null
    private var mIsDragged = false
    private var mIsExoPlayerInitialized = false
    private var mIsFullScreen = false
    private var mIsPlaying = false
    private var mVideoSize: Point? = null
    // private lateinit var mGestureDetector: GestureDetector
    private var mBookmarker: Bookmarker? = null
    private var mCurrentURI: Uri? = null
    private var mIsChangingPosition = false
    private var mDownX = 0
    private var mDownY = 0
    private var THRESHOLD = 80
    private var mState = 0
    private var mCurrentPosition = 0L
    private var mSeekPosition = 0L
    private var mScreenWidth = 0
    private lateinit var mAudioManager: AudioManager
    private var mScreenHeight = 0

//    private val mOnAudioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
//        when (focusChange) {
//            AudioManager.AUDIOFOCUS_GAIN -> {
//            }
//            AudioManager.AUDIOFOCUS_LOSS -> {
//
//            }
//            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
//
//            }
//            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
//
//            }
//        }
//    }

    private fun cleanup() {
        // Log.e(TAG, "cleanup")
        pauseVideo()
        video_curr_time.text = 0.getFormattedDuration()
        releaseExoPlayer()
        video_seekbar.progress = 0
        mTimeHandler.removeCallbacksAndMessages(null)
        mHidePauseHandler.removeCallbacksAndMessages(null)
    }

    private fun getDuration(): Long? {
        return mExoPlayer?.duration
    }

    private fun getPlayingPosition(): Long? {
        return mExoPlayer?.currentPosition
    }

    private fun hasNavBar(): Boolean {
        // Log.e(TAG, "hasNavBar")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            val display = windowManager.defaultDisplay
            val realDisplayMetrics = DisplayMetrics()
            display.getRealMetrics(realDisplayMetrics)
            val rh = realDisplayMetrics.heightPixels
            var rw = realDisplayMetrics.widthPixels
            val displayMetrics = DisplayMetrics()
            display.getMetrics(displayMetrics)
            val h = displayMetrics.heightPixels
            val w = displayMetrics.widthPixels
            rw - w > 0 || rh - h > 0
        } else {
            val hasMenuKey = ViewConfiguration.get(this).hasPermanentMenuKey()
            val hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK)
            !hasMenuKey && !hasBackKey
        }
    }

    private fun initialize() {
        // Log.e(TAG, "initialize")
        mBookmarker = Bookmarker(this)
        mExoPlayer = ExoPlayerFactory.newSimpleInstance(this, DefaultTrackSelector())
        setupPlayer()
        initializeExoPlayer()
//        mGestureDetector = GestureDetector(this, object : GestureDetector.OnGestureListener {
//            override fun onShowPress(p0: MotionEvent?) {
//                // Log.e(TAG, "onShowPress")
//            }
//
//            override fun onSingleTapUp(p0: MotionEvent?): Boolean {
//                // Log.e(TAG, "onSingleTapUp")
//                return true
//            }
//
//            override fun onDown(p0: MotionEvent?): Boolean {
//                // Log.e(TAG, "onDown")
//                return true
//            }
//
//            /**
//             * Notified of a fling event when it occurs with the initial on down {@link MotionEvent}
//             * and the matching up {@link MotionEvent}. The calculated velocity is supplied along
//             * the x and y axis in pixels per second.
//             *
//             * @param e1 The first down motion event that started the fling.
//             * @param e2 The move motion event that triggered the current onFling.
//             * @param velocityX The velocity of this fling measured in pixels per second
//             *              along the x axis.
//             * @param velocityY The velocity of this fling measured in pixels per second
//             *              along the y axis.
//             * @return true if the event is consumed, else false
//             */
//            override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
//                // Log.e(TAG, "onFling")
//                val speed = Math.abs(velocityX * 0.0075f).toInt()
//
//                if (velocityX > 0) {
//                    if (mCurrTime + speed < mDuration) {
//                        mCurrTime += speed
//                        setVideoProgress(mCurrTime)
//                    }
//                } else {
//                    if (mCurrTime - speed > 0) {
//                        mCurrTime -= speed
//                        setVideoProgress(mCurrTime)
//                    }
//                }
//                return true
//            }
//
//            override fun onScroll(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float): Boolean {
//                // Log.e(TAG, "onScroll")
//                return true
//            }
//
//            override fun onLongPress(p0: MotionEvent?) {
//                // Log.e(TAG, "onLongPress")
//            }
//        })
//        video_holder.setOnTouchListener { view, motionEvent ->
//            mGestureDetector.onTouchEvent(motionEvent)
//        }
        video_holder.setOnTouchListener(this)
        toggleFullScreen()
        var uri = mCurrentURI ?: return
        val bookmark = mBookmarker?.getBookmark(uri)
        if (bookmark ?: 0 > 0) {
            setVideoProgress(bookmark ?: 0)
        }
    }

    private fun initializeExoPlayer() {

//        val audioManager = audioManager
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val playbackAttributes = AudioAttributes.Builder()
//                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
//                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
//                    .build()
//            audioManager.requestAudioFocus(AudioFocusRequest
//                    .Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
//                    .setAudioAttributes(playbackAttributes)
//                    .setAcceptsDelayedFocusGain(true)
//                    .setOnAudioFocusChangeListener(mOnAudioFocusChangeListener)
//                    .build())
//        } else {
//            audioManager.requestAudioFocus(mOnAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
//        }

        val filePath = intent.getStringExtra(KEY_PATH)
        mCurrentURI = Uri.fromFile(File(filePath))
//        val dataSpec = DataSpec(mCurrentURI)
//        val fileDataSource = FileDataSource()
//        try {
//            fileDataSource.open(dataSpec)
//        } catch (e: Exception) {
//
//        }
//        val factory = DataSource.Factory { fileDataSource }
//        val audioSource = ExtractorMediaSource(fileDataSource.uri, factory,
//                DefaultExtractorsFactory(), null, null)
        val audioSource = ExtractorMediaSource.Factory(FileDataSourceFactory()).createMediaSource(mCurrentURI)
        mExoPlayer?.run {
            addListener(object : Player.EventListener {
                override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
                    // Log.e(TAG, "onPlaybackParametersChanged")
                }

                override fun onSeekProcessed() {
                    // Log.e(TAG, "onSeekProcessed")
                }

                override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {
                    // Log.e(TAG, "onTracksChanged")
                }

                override fun onPlayerError(error: ExoPlaybackException?) {
                    // Log.e(TAG, "onPlayerError")
                    mIsExoPlayerInitialized = false
                    mState = STATE_ERROR
                }

                override fun onLoadingChanged(isLoading: Boolean) {
                    // Log.e(TAG, "onLoadingChanged")
                }

                /**
                 * Called when a position discontinuity occurs without a change to the timeline. A position
                 * discontinuity occurs when the current window or period index changes (as a result of playback
                 * transitioning from one period in the timeline to the next), or when the playback position
                 * jumps within the period currently being played (as a result of a seek being performed, or
                 * when the source introduces a discontinuity internally).
                 * <p>
                 * When a position discontinuity occurs as a result of a change to the timeline this method is
                 * <em>not</em> called. {@link #onTimelineChanged(Timeline, Object, int)} is called in this
                 * case.
                 *
                 * @param reason The {@link DiscontinuityReason} responsible for the discontinuity.
                 */
                override fun onPositionDiscontinuity(reason: Int) {
                    // Log.e(TAG, "onPositionDiscontinuity")
                }

                override fun onRepeatModeChanged(repeatMode: Int) {
                    // Log.e(TAG, "onRepeatModeChanged")
                }

                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                    // Log.e(TAG, "onShuffleModeEnabledChanged")
                }

                override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {
                    // Log.e(TAG, "onTimelineChanged")
                }

                override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                    // Log.e(TAG, "onPlayerStateChanged")
                    mState = playbackState
                    mIsExoPlayerInitialized = playbackState == Player.STATE_READY || playbackState == Player.STATE_ENDED
                    when (playbackState) {
                        Player.STATE_READY -> videoPrepared()
                        Player.STATE_ENDED -> videoCompleted()
                    }
                }
            })
            addVideoListener(object : VideoListener {
                override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
                    // Log.e(TAG, "onVideoSizeChanged")
                    mVideoSize?.run {
                        x = width
                        y = height
                        setVideoSize()
                    }
                }

                override fun onRenderedFirstFrame() {
                    // Log.e(TAG, "onRenderedFirstFrame")
                }
            })
            audioStreamType = AudioManager.STREAM_MUSIC
            prepare(audioSource)
        }

        val speedRate = mPrefer.getFloat(PREFER_SPEED_RATE, 0f)
        if (speedRate != 0f) {
            mExoPlayer?.playbackParameters = PlaybackParameters(mRate, mRate)
        }
        mVideoSize = filePath.getVideoResolution()
        setVideoSize()
        //setupVideoDuration(filePath)
//        video_surface.onGlobalLayout {
//            playVideo()
//        }
    }

    private fun initializeTimeHolder() {
        // Log.e(TAG, "initializeTimeHolder")
        val left = 0
        val top = 0
        var right = 0
        var bottom = 0
        if (hasNavBar()) {
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                bottom += navigationBarHeight
            } else {
                right += navigationBarWidth
                bottom += navigationBarHeight
            }
        }
        video_time_holder.setPadding(left, top, right, bottom)
        video_seekbar.setOnSeekBarChangeListener(this)
        if (mIsFullScreen) video_time_holder.beInvisible()
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        // Log.e(TAG, "onConfigurationChanged")
        super.onConfigurationChanged(newConfig)
        setVideoSize()
        initializeTimeHolder()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Log.e(TAG, "onCreate")
        mScreenWidth = widthPixels
        mScreenHeight = heightPixels
        mAudioManager = audioManager
        //requestFullScreen()
        savedInstanceState?.let {
            mCurrTime = it.getInt(KEY_PROGRESS)
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)
        initialize()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.run {
            menuInflater.inflate(R.menu.menu_video, this)

        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onDestroy() {
        // Log.e(TAG, "onDestroy")
        super.onDestroy()
        if (!isChangingConfigurations) cleanup()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_delete -> deleteVideo()
            R.id.action_rename -> renameVideo()

        }
        return super.onOptionsItemSelected(item)
    }

    fun renameVideo() {
        mExoPlayer?.stop()
        val fileItem = File(mCurrentURI?.path).toFileDirItem(this)
        dialog(this, fileItem.name, getString(R.string.menu_rename_file), true) {
            if (!it.isNullOrBlank()) {
                renameFile(fileItem.path, fileItem.path.getParentPath() + File.separator + it.toString()) {
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }
        }
    }

    fun deleteVideo() {
        mExoPlayer?.stop()
        val fileItem = File(mCurrentURI?.path).toFileDirItem(this)
        deleteFile(fileItem) {
            setResult(Activity.RESULT_OK)
            finish()
        }

    }

    override fun onPause() {
        // Log.e(TAG, "onPause")
        super.onPause()
        pauseVideo()
        var uri = mCurrentURI ?: return
        var currentPosition = mExoPlayer?.currentPosition ?: return
        var duration = mExoPlayer?.duration ?: return
        mBookmarker?.setBookmark(uri, currentPosition.toInt() / 1000, duration.toInt())
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        if (mExoPlayer != null && fromUser) setVideoProgress(progress)
    }

    override fun onResume() {
        // Log.e(TAG, "onResume")
        super.onResume()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // Log.e(TAG, "onSaveInstanceState")
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_PROGRESS, mCurrTime)
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
        // Log.e(TAG, "onStartTrackingTouch")
        mExoPlayer?.run {
            playWhenReady = false
        }
        mIsDragged = true
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        // Log.e(TAG, "onStopTrackingTouch")
        if (mExoPlayer == null) return
        if (!mIsPlaying) togglePlayPause()
        else {
            mExoPlayer?.playWhenReady = true
        }
        mIsDragged = false
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, p1: Int, p2: Int) {
        // Log.e(TAG, "onSurfaceTextureAvailable")
        mExoPlayer?.setVideoSurface(Surface(video_surface.surfaceTexture))
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
        // Log.e(TAG, "onSurfaceTextureDestroyed")
        releaseExoPlayer()
        return false
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {}
    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {}

    private var mIsChangeVolume = false
    private var mIsChangeRate = false
    private var mRate = 1f
    private var mVolume = 0

    override fun onTouch(view: View?, event: MotionEvent): Boolean {

        val x = event.x.toInt()
        val y = event.y.toInt()
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mDownX = x
                mDownY = y
                mIsChangingPosition = false
                mIsChangeRate = false
                mIsChangeVolume = false
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = x - mDownX
                var deltaY = y - mDownY
                var absDeltaX = abs(deltaX)
                val absDeltaY = abs(deltaY)
                if (!mIsChangingPosition && !mIsChangeRate && !mIsChangeVolume) {
                    if (absDeltaX > THRESHOLD || absDeltaY > THRESHOLD) {
                        // Log.e(TAG, "$x $y $absDeltaX $absDeltaY $THRESHOLD")

                        if (absDeltaX >= THRESHOLD) {
                            if (mState != STATE_ERROR) {
                                mIsChangingPosition = true
                                mCurrentPosition = getPlayingPosition() ?: 0L
                            }
                        } else {
                            if (mDownX > mScreenWidth * 0.5f) {
                                mIsChangeVolume = true
                                mVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                            }
                        }
                    }
                }
                if (mIsChangingPosition) {
                    val totalDuration = getDuration() ?: 0L
                    val delta = deltaX * totalDuration / mScreenWidth;
                    // Limit the maximum value to 30 seconds
                    mSeekPosition = mCurrentPosition + min(delta, 30000L)
                    if (mSeekPosition > totalDuration) {
                        mSeekPosition = totalDuration
                    }
                }
                if (mIsChangeVolume) {
                    deltaY = -deltaY
                    val max = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    val deltaVolume = max * deltaY * 3 / mScreenHeight
                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mVolume + deltaVolume, 0)
                }

            }
            MotionEvent.ACTION_UP -> {
                if (mIsChangingPosition) {
                    setProgress(mSeekPosition)
                }
                if (!mIsChangingPosition && !mIsChangeVolume ) {
                    if ( mDownX < mScreenWidth * 0.3f) {
                        mRate = (mRate.toInt() / 5 - 1) * 5.0f
                        if (mRate < 1f) {
                            mRate = 1f
                        }
                        mExoPlayer?.playbackParameters = PlaybackParameters(mRate, mRate);
                        toast("当前播放倍速：$mRate", Toast.LENGTH_SHORT)
                    } else if ( mDownX  > mScreenWidth * 0.7f) {

                        mRate = (mRate.toInt() / 5 + 1) * 5.0f
                        mExoPlayer?.playbackParameters = PlaybackParameters(mRate, mRate);
                        toast("当前播放倍速：$mRate", Toast.LENGTH_SHORT)
                    }
                   // mPrefer.edit().putFloat(PREFER_SPEED_RATE, mRate).apply()
                }
            }
        }
        return true
    }

    private fun pauseVideo() {
        // Log.e(TAG, "pauseVideo")
        if (mExoPlayer == null) return
        mIsPlaying = false
        if (!videoEnded()) mExoPlayer?.playWhenReady = false
        video_play_outline.run {
            setImageResource(R.mipmap.ic_play)
            alpha = PLAY_PAUSE_VISIBLE_ALPHA
        }
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun playVideo() {
        // Log.e(TAG, "playVideo")
        if (mExoPlayer == null) return
//        if (video_preview.isVisible()) {
//            video_preview.beGone()
//            initializeExoPlayer()
//        }
        if (videoEnded()) {
            setVideoProgress(0)
        }
        mIsPlaying = true
        mExoPlayer?.playWhenReady = true
        video_play_outline.setImageResource(R.mipmap.ic_pause)
        requestKeepScreenOn()
        mHidePauseHandler.postDelayed({
            video_play_outline.animate().alpha(0f).start()
        }, HIDE_PAUSE_DELAY)
    }

    private fun releaseExoPlayer() {
        // Log.e(TAG, "releaseExoPlayer")
        mExoPlayer?.stop()
        Thread {
            mExoPlayer?.release()
            mExoPlayer = null
        }.start()
    }

    private fun setPlaySpeedRate(rate: Float) {
        val playbackParameters = PlaybackParameters(rate, rate)
        mExoPlayer?.playbackParameters = playbackParameters
    }

    private fun setProgress(millisSecond: Long) {
        // Log.e(TAG, "setVideoProgress")
        mExoPlayer?.seekTo(millisSecond)
        val seconds = (millisSecond / 1000).toInt()
        video_seekbar.progress = seconds
        video_curr_time.text = seconds.getFormattedDuration()
    }

    private fun setupPlayer() {
        // Log.e(TAG, "setupPlayer")
        video_play_outline.setOnClickListener {
            togglePlayPause()
        }
        video_surface.setOnClickListener { toggleFullScreen() }
        video_surface.surfaceTextureListener = this
        video_holder.setOnClickListener { toggleFullScreen() }
        initializeTimeHolder()
    }

    private fun setupTimeHolder() {
        // Log.e(TAG, "setupTimeHolder")
        video_seekbar.max = mDuration
        video_duration.text = mDuration.getFormattedDuration()
        setupTimer()
    }

    private fun setupTimer() {
        // Log.e(TAG, "setupTimer")
        runOnUiThread(object : Runnable {
            override fun run() {
                if (mExoPlayer != null && !mIsDragged && mIsPlaying) {
                    mCurrTime = ((mExoPlayer?.currentPosition ?: 0) / 1000).toInt()
                    video_seekbar.progress = mCurrTime
                    video_curr_time.text = mCurrTime.getFormattedDuration()
                }
                //
                mTimeHandler.postDelayed(this, 1000)
            }
        })
    }

    private fun setupVideoDuration(filePath: String) {
        // Log.e(TAG, "setupVideoDuration")
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(filePath)
            mDuration = Math.round(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toInt() / 1000f)
        } catch (ignored: Exception) {
        }
        setupTimeHolder()
        setVideoProgress(0)
    }

    private fun setVideoProgress(seconds: Int) {
        // Log.e(TAG, "setVideoProgress")
        mExoPlayer?.seekTo(seconds * 1000L)
        video_seekbar.progress = seconds
        video_curr_time.text = seconds.getFormattedDuration()
    }

    private fun setVideoSize() {
        // Log.e(TAG, "setVideoSize")
        mVideoSize?.let {
            val videoProportion = it.x.toFloat() / it.y.toFloat()
            val display = windowManager.defaultDisplay
            val screenWidth: Int
            val screenHeight: Int
            if (isJellyBean1Plus()) {
                val realMetrics = DisplayMetrics()
                display.getRealMetrics(realMetrics)
                screenHeight = realMetrics.heightPixels
                screenWidth = realMetrics.widthPixels
            } else {
                screenHeight = display.height
                screenWidth = display.width
            }
            val screenProportion = screenWidth.toFloat() / screenHeight.toFloat()
            video_surface.layoutParams.apply {
                if (videoProportion > screenProportion) {
                    width = screenWidth
                    height = (screenWidth.toFloat() / videoProportion).toInt()
                } else {
                    width = (videoProportion * screenHeight.toFloat()).toInt()
                    height = screenHeight
                }
            }
        }
    }

    private fun toggleFullScreen() {
        // Log.e(TAG, "toggleFullScreen")
        mIsFullScreen = !mIsFullScreen
        if (mIsFullScreen) {
            video_time_holder.beGone()
            hideSystemUI(true)
        } else {
            video_time_holder.beVisible()
            showSystemUI(true)
        }
    }

    private fun togglePlayPause() {
        // Log.e(TAG, "togglePlayPause")
        mIsPlaying = !mIsPlaying
        mHidePauseHandler.removeCallbacksAndMessages(null)
        if (mIsPlaying) playVideo() else pauseVideo()
    }

    private fun videoCompleted() {
        // Log.e(TAG, "videoCompleted")
        mCurrTime = ((mExoPlayer?.duration ?: 0) / 1000).toInt()
        video_seekbar.progress = video_seekbar.max
        video_curr_time.text = mDuration.getFormattedDuration()
        pauseVideo()
    }

    private fun videoEnded(): Boolean {
        // Log.e(TAG, "videoEnded")
        return mExoPlayer?.playbackState == Player.STATE_ENDED
    }

    private fun videoPrepared() {
        // Log.e(TAG, "videoPrepared")
        if (mDuration == 0) {
            mDuration = ((mExoPlayer?.duration ?: 0) / 1000).toInt()
            setupTimeHolder()
            //setVideoProgress(mCurrTime)
            playVideo()
        }
    }

    companion object {
        private const val THRESHOLD = 80
        private const val TAG = "VideoActivity"
        private const val HIDE_PAUSE_DELAY = 2000L
        private const val KEY_PROGRESS = "progress"
        private const val PLAY_PAUSE_VISIBLE_ALPHA = 0.8f
        private const val STATE_ERROR = 5
        private const val STATE_PLAYING = 6
        private const val STATE_PAUSE = 7
        private const val STATE_IDLE = 1
        private const val STATE_BUFFERING = 2
        private const val STATE_READY = 3
        private const val STATE_ENDED = 4
        private const val MENU_SPEED_0_2_5X = 8
        private const val MENU_SPEED_0_5X = 1
        private const val MENU_SPEED_1X = 2
        private const val MENU_SPEED_2X = 3
        private const val MENU_SPEED_5X = 5
        private const val MENU_SPEED_10X = 7
        private const val PREFER_SPEED_RATE = "speed_rate"
    }
}

