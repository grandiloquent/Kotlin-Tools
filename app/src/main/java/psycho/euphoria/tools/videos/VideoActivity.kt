package psycho.euphoria.tools.videos

import android.content.res.Configuration
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.SeekBar
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.FileDataSource
import com.google.android.exoplayer2.video.VideoListener
import kotlinx.android.synthetic.main.activity_video.*
import psycho.euphoria.tools.R
import psycho.euphoria.tools.commons.*
import java.io.File

class VideoActivity : AppCompatActivity(), TextureView.SurfaceTextureListener, SeekBar.OnSeekBarChangeListener {


    private val mHidePauseHandler: Handler = Handler()
    private val mTimeHandler: Handler = Handler()
    private var mCurrTime = 0
    private var mDuration = 0
    private var mExoPlayer: SimpleExoPlayer? = null
    private var mIsDragged = false
    private var mIsExoPlayerInitialized = false
    private var mIsFullScreen = false
    private var mIsPlaying = true
    private var mVideoSize: Point? = null
    private lateinit var mGestureDetector: GestureDetector

    private fun cleanup() {
        pauseVideo()
        video_curr_time.text = 0.getFormattedDuration()
        releaseExoPlayer()
        video_seekbar.progress = 0
        mTimeHandler.removeCallbacksAndMessages(null)
        mHidePauseHandler.removeCallbacksAndMessages(null)
    }

    private fun hasNavBar(): Boolean {
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
        setupPlayer()
        initializeExoPlayer()
        mGestureDetector = GestureDetector(this, object : GestureDetector.OnGestureListener {
            override fun onShowPress(p0: MotionEvent?) {
                Tracker.e("onShowPress", "p0 => ${p0} \n")
            }

            override fun onSingleTapUp(p0: MotionEvent?): Boolean {
                Tracker.e("onSingleTapUp", "p0 => ${p0} \n")
                return true
            }

            override fun onDown(p0: MotionEvent?): Boolean {
                Tracker.e("onDown", "p0 => ${p0} \n")
                return true
            }

            override fun onFling(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float): Boolean {
                Tracker.e("onFling", "p0 => ${p0} \np1 => ${p1} \np2 => ${p2} \np3 => ${p3} \n")
                return true
            }

            override fun onScroll(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float): Boolean {
                Tracker.e("onScroll", "p0 => ${p0} \np1 => ${p1} \np2 => ${p2} \np3 => ${p3} \n")
                return true
            }

            override fun onLongPress(p0: MotionEvent?) {
                Tracker.e("onLongPress", "p0 => ${p0} \n")
            }


        })
        video_holder.setOnTouchListener { view, motionEvent ->
            mGestureDetector.onTouchEvent(motionEvent)
        }
    }

    private fun initializeExoPlayer() {
        val filePath = intent.getStringExtra(KEY_PATH)
        val dataSpec = DataSpec(Uri.fromFile(File(filePath)))
        val fileDataSource = FileDataSource()
        try {
            fileDataSource.open(dataSpec)
        } catch (e: Exception) {
        }
        val factory = DataSource.Factory { fileDataSource }
        val audioSource = ExtractorMediaSource(fileDataSource.uri, factory,
                DefaultExtractorsFactory(), null, null)
        mExoPlayer?.run {
            addListener(object : Player.EventListener {
                override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
                }

                override fun onSeekProcessed() {
                }

                override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {
                }

                override fun onPlayerError(error: ExoPlaybackException?) {
                    mIsExoPlayerInitialized = false
                }

                override fun onLoadingChanged(isLoading: Boolean) {
                }

                override fun onPositionDiscontinuity(reason: Int) {
                }

                override fun onRepeatModeChanged(repeatMode: Int) {
                }

                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                }

                override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {
                }

                override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                    mIsExoPlayerInitialized = playbackState == Player.STATE_READY || playbackState == Player.STATE_ENDED
                    when (playbackState) {
                        Player.STATE_READY -> videoPrepared()
                        Player.STATE_ENDED -> videoCompleted()
                    }
                }
            })
            addVideoListener(object : VideoListener {
                override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
                    mVideoSize?.run {
                        x = width
                        y = height
                        setVideoSize()
                    }
                }

                override fun onRenderedFirstFrame() {
                }
            })
            audioStreamType = AudioManager.STREAM_MUSIC
            prepare(audioSource)
        }
        mVideoSize = filePath.getVideoResolution()
        setVideoSize()
        setupVideoDuration(filePath)
        video_surface.onGlobalLayout {
            playVideo()
        }
    }

    private fun initializeTimeHolder() {
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
        super.onConfigurationChanged(newConfig)
        setVideoSize()
        initializeTimeHolder()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        savedInstanceState?.let {
            mCurrTime = it.getInt(KEY_PROGRESS)
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)
        initialize()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isChangingConfigurations) cleanup()
    }

    override fun onPause() {
        super.onPause()
        pauseVideo()
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        if (mExoPlayer != null && fromUser) setVideoProgress(progress)
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_PROGRESS, mCurrTime)
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
        mExoPlayer?.run {
            playWhenReady = false
        }
        mIsDragged = true
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        if (mExoPlayer == null) return
        if (!mIsPlaying) togglePlayPause()
        else {
            mExoPlayer?.playWhenReady = true
        }
        mIsDragged = false
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, p1: Int, p2: Int) {
        mExoPlayer?.setVideoSurface(Surface(video_surface.surfaceTexture))
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
        releaseExoPlayer()
        return false
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {}
    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {}
    private fun pauseVideo() {
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
        if (mExoPlayer == null) return
        if (video_preview.isVisible()) {
            video_preview.beGone()
            initializeExoPlayer()
        }
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
        mExoPlayer?.stop()
        Thread {
            mExoPlayer?.release()
            mExoPlayer = null
        }.start()
    }

    private fun setupPlayer() {
        video_play_outline.setOnClickListener { togglePlayPause() }
        video_surface.setOnClickListener { toggleFullScreen() }
        video_surface.surfaceTextureListener = this
        video_holder.setOnClickListener { toggleFullScreen() }
        initializeTimeHolder()
    }

    private fun setupTimeHolder() {
        video_seekbar.max = mDuration
        video_duration.text = mDuration.getFormattedDuration()
        setupTimer()
    }

    private fun setupTimer() {
        runOnUiThread(object : Runnable {
            override fun run() {
                if (mExoPlayer != null && !mIsDragged && mIsPlaying) {
                    mCurrTime = ((mExoPlayer?.currentPosition ?: 0) / 1000).toInt()
                    video_seekbar.progress = mCurrTime
                    video_curr_time.text = mCurrTime.getFormattedDuration()
                }
                mTimeHandler.postDelayed(this, 1000)
            }
        })
    }

    private fun setupVideoDuration(filePath: String) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(filePath)
            mDuration = Math.round(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toInt() / 1000f)
        } catch (ignored: Exception) {
        }
    }

    private fun setVideoProgress(seconds: Int) {
        mExoPlayer?.seekTo(seconds * 1000L)
        video_seekbar.progress = seconds
        video_curr_time.text = seconds.getFormattedDuration()
    }

    private fun setVideoSize() {
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
        mIsFullScreen = !mIsFullScreen
        if (mIsFullScreen) {
            hideSystemUI(true)
        } else {
            showSystemUI(true)
        }
    }

    private fun togglePlayPause() {
        mIsPlaying = !mIsPlaying
        mHidePauseHandler.removeCallbacksAndMessages(null)
        if (mIsPlaying) playVideo() else pauseVideo()
    }

    private fun videoCompleted() {
        mCurrTime = ((mExoPlayer?.duration ?: 0) / 1000).toInt()
        video_seekbar.progress = video_seekbar.max
        video_curr_time.text = mDuration.getFormattedDuration()
        pauseVideo()
    }

    private fun videoEnded(): Boolean {
        return false
    }

    private fun videoPrepared() {
        if (mDuration == 0) {
            mDuration = ((mExoPlayer?.duration ?: 0) / 1000).toInt()
            setupTimeHolder()
            setVideoProgress(mCurrTime)
            playVideo()
        }
    }

    companion object {
        private const val TAG = "VideoActivity"
        private const val HIDE_PAUSE_DELAY = 2000L
        private const val KEY_PROGRESS = "progress"
        private const val PLAY_PAUSE_VISIBLE_ALPHA = 0.8f
    }
}