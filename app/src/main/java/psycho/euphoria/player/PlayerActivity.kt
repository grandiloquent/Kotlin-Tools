package psycho.euphoria.player

import android.app.Activity
import android.net.Uri
import android.opengl.Visibility
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.view.View
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.extractor.ExtractorsFactory
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.FileDataSourceFactory
import com.google.android.exoplayer2.video.VideoListener
import kotlinx.android.synthetic.main.activity_player_video.*
import psycho.euphoria.common.extension.*
import psycho.euphoria.common.extension.C
import psycho.euphoria.tools.R
import java.io.File
import java.util.*
import kotlin.math.max
import kotlin.math.round

class PlayerActivity : Activity(), TimeBar.OnScrubListener, Player.EventListener, VideoListener, PlaybackPreparer {
    override fun preparePlayback() {

    }

    private var mPlayer: SimpleExoPlayer? = null
    private val mHideAction = Runnable { }
    private val mHanlder = Handler()
    private val mStringBuilder = StringBuilder()
    private val mFormatter = Formatter()
    private var mShowTimeoutMs = DEFAULT_SHOW_TIMEOUT_MS
    private var mWindow = Timeline.Window()
    private val mUpdateProgressAction = Runnable { updateProgress() }
    private val mControlDispatcher = DefaultControlDispatcher()


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
        }
        exo_pause.setOnClickListener { mControlDispatcher.dispatchSetPlayWhenReady(mPlayer, false) }
        exo_next.setOnClickListener { next() }
        exo_prev.setOnClickListener { previous() }
    }
    private fun generateMediaSource(uri: Uri): MediaSource? {
        val files = uri.path.listVideoFiles()
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
        setContentView(R.layout.activity_player_video)
        bindActions()
        updateAll()
    }
    private fun initializePlayer() {
        if (mPlayer == null) {
            mPlayer = ExoPlayerFactory.newSimpleInstance(this, DefaultTrackSelector()).also {
                it.addListener(this)
                it.videoComponent?.apply {
                    setVideoTextureView(texture_view)
                    addVideoListener(this@PlayerActivity)
                }
                val testMp4 = File(File(Environment.getExternalStorageDirectory(), "1"), "1.mp4")
                val mediaSource = generateMediaSource(testMp4.toUri())
                it.prepare(mediaSource)
            }
        }
    }
    private fun isPlaying(): Boolean {
        mPlayer?.let {
            return it.playbackState == Player.STATE_ENDED
                    && it.playbackState == Player.STATE_IDLE
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(arrayOf("android.permission.ACCESS_NETWORK_STATE",
                    "android.permission.INTERNET",
                    "android.permission.WAKE_LOCK",
                    "android.permission.WRITE_EXTERNAL_STORAGE"), 100)
        } else initialize()
    }
    override fun onLoadingChanged(p0: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
    override fun onPause() {
        super.onPause()
        C.atMost(23, { releasePlayer() }, {})
    }
    override fun onPlaybackParametersChanged(p0: PlaybackParameters?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
    override fun onPlayerError(p0: ExoPlaybackException?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
    override fun onPlayerStateChanged(p0: Boolean, p1: Int) {
        updatePlayPauseButton()
    }
    override fun onPositionDiscontinuity(p0: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
    override fun onRenderedFirstFrame() {
    }
    override fun onRepeatModeChanged(p0: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
    override fun onScrubStart(timeBar: TimeBar, position: Long) {
    }
    override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
    override fun onSeekProcessed() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
    override fun onShuffleModeEnabledChanged(p0: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
    override fun onTracksChanged(p0: TrackGroupArray?, p1: TrackSelectionArray?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
    override fun onVideoSizeChanged(p0: Int, p1: Int, p2: Int, p3: Float) {
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
    }
    private fun seekTo(position: Long) {
    }
    private fun setButtonEnabled(enabled: Boolean, view: View?) {
        view?.apply {
            isEnabled = enabled
            alpha = if (enabled) 1f else 0.3f
            visibility = View.VISIBLE
        }
    }
    private fun updateAll() {
        updatePlayPauseButton()
        updateProgress()
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
            timeline.getWindow(it.currentWindowIndex, mWindow)
            duration = mWindow.durationUs.usToMs()
            position = it.currentPosition
            playbackState = it.playbackState
        }
        exo_position.text = position.getStringForTime(mStringBuilder, mFormatter)
        exo_duration.text = duration.getStringForTime(mStringBuilder, mFormatter)
        Log.e(TAG, "updateProgress $position $duration ${exo_position.text} ${exo_duration.text}")
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

    companion object {
        private const val MAX_POSITION_FOR_SEEK_TO_PREVIOUS = 3000
        private const val DEFAULT_SHOW_TIMEOUT_MS = 5000L
        private const val TAG = "PlayerActivity"
    }
}