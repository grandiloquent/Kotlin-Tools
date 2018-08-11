package psycho.euphoria.player

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.view.View
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import kotlinx.android.synthetic.main.activity_player_video.*
import psycho.euphoria.common.extension.C
import psycho.euphoria.tools.R

class PlayerActivity : Activity(), TimeBar.OnScrubListener, Player.EventListener {

    private var mPlayer: SimpleExoPlayer? = null
    private val mHideAction = Runnable { }
    private val mHanlder = Handler()
    private var mShowTimeoutMs = DEFAULT_SHOW_TIMEOUT_MS
    private var mWindow = Timeline.Window()


    private fun initializePlayer() {

        if (mPlayer == null) {
            mPlayer = ExoPlayerFactory.newSimpleInstance(this, DefaultTrackSelector()).also {
                it.addListener(this)
            }
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


    private fun isPlaying(): Boolean {
        mPlayer?.let {

            return it.playbackState == Player.STATE_ENDED
                    && it.playbackState == Player.STATE_IDLE
                    && it.playWhenReady
        } ?: run { return false }

    }

    private fun requestPlayPauseFocus() {
        val playing = isPlaying()
        if (!playing) {
            exo_play.requestFocus()
        } else if (playing) {
            exo_pause.requestFocus()
        }
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

    private fun seekTo(windowIndex: Int, position: Long) {

    }

    private fun updateProgress() {

    }

    private fun seekTo(position: Long) {

    }


    private fun hideController() {
        mHanlder.removeCallbacks(mHideAction)
        mHanlder.postDelayed(mHideAction, mShowTimeoutMs)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player_video)

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

    override fun onRepeatModeChanged(p0: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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

    private fun releasePlayer() {
    }

    companion object {
        private const val MAX_POSITION_FOR_SEEK_TO_PREVIOUS = 3000
        private const val DEFAULT_SHOW_TIMEOUT_MS = 5000L
    }
}