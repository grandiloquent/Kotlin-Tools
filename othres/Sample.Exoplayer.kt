package psycho.euphoria.tools

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Pair
import android.view.KeyEvent
import android.view.View
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.RandomTrackSelection
import com.google.android.exoplayer2.ui.PlayerControlView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import kotlinx.android.synthetic.main.activity_video.*
import psycho.euphoria.tools.commons.KEY_PATH
import psycho.euphoria.tools.commons.requestFullScreen
import psycho.euphoria.tools.commons.version
import java.io.File
import kotlin.math.max


class VideoPlayerActivity : AppCompatActivity(), View.OnClickListener, PlaybackPreparer, PlayerControlView.VisibilityListener {


    private var mStartAutoPlayer: Boolean = true
    private var mStartWindow = 0
    private var mStartPosition = 0L
    private lateinit var mTrackSelectorParameters: DefaultTrackSelector.Parameters
    private var mPlayer: SimpleExoPlayer? = null
    private var mTrackSelector: DefaultTrackSelector? = null
    private var mMediaSource: MediaSource? = null

    private fun clearStartPosition() {
        mStartAutoPlayer = true
        mStartWindow = C.INDEX_UNSET
        mStartPosition = C.INDEX_UNSET.toLong()
    }

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        return player_view.dispatchMediaKeyEvent(event) || super.dispatchKeyEvent(event)
    }

    private fun initialize() {
        setContentView(R.layout.activity_video)
        with(player_view) {
            setControllerVisibilityListener { }
            requestFocus()
            setErrorMessageProvider { throwable -> Pair.create(0, throwable.toString()) }
        }
    }

    private fun initializePlayer() {
        val path = intent.getStringExtra(KEY_PATH)


        if (mPlayer == null) {
            mTrackSelector = DefaultTrackSelector(RandomTrackSelection.Factory()).also {
                it.parameters = mTrackSelectorParameters
            }

            mPlayer = ExoPlayerFactory.newSimpleInstance(DefaultRenderersFactory(this), mTrackSelector).apply {
                addListener(object : Player.DefaultEventListener() {
                    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {

                    }
                })
                playWhenReady = mStartAutoPlayer
                player_view.player = this
            }

            player_view.setPlaybackPreparer(this)
        }

//        val fileDataSource = FileDataSource()
//        fileDataSource.open(DataSpec(Uri.fromFile(File(path))))
//        val dataSourceFactory = DefaultDataSourceFactory(this, Util.getUserAgent(this, "VideoPlayerActivity"))
//        val videoSource = ExtractorMediaSource(fileDataSource.uri, dataSourceFactory, DefaultExtractorsFactory(), null, null)
        mMediaSource = buildMediaSource(Uri.fromFile(File(path)))
        val haveStartPosition = mStartWindow != C.INDEX_UNSET
        if (haveStartPosition) {
            mPlayer?.seekTo(mStartWindow, mStartPosition)
        }
        mPlayer?.prepare(mMediaSource, !haveStartPosition, false)

    }

    private fun buildMediaSource(uri: Uri): MediaSource {
        val dataSourceFactory = DefaultDataSourceFactory(this, Util.getUserAgent(this, "VideoPlayerActivity"))
        return ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
    }

    override fun onClick(view: View?) {
        view?.let {

        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        requestFullScreen()
        savedInstanceState?.let {
            mTrackSelectorParameters = it.getParcelable(KEY_TRACK_SELECTOR_PARAMETERS)
            mStartAutoPlayer = it.getBoolean(KEY_AUTO_PLAY)
            mStartWindow = it.getInt(KEY_WINDOW)
            mStartPosition = it.getLong(KEY_POSITION)
        } ?: run {
            mTrackSelectorParameters = DefaultTrackSelector.ParametersBuilder().build()
            clearStartPosition()
        }

        super.onCreate(savedInstanceState)

        initialize()
    }

    override fun onNewIntent(intent: Intent?) {
        releasePlayer()
        clearStartPosition()
        setIntent(intent)
    }

    override fun onPause() {
        super.onPause()
        if (version <= 23) releasePlayer()
    }

    override fun onResume() {
        super.onResume()
        if (version <= 23 || mPlayer == null) {
            initializePlayer()
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        updateTrackSelectorParameters()
        updateStartPosition()
        outState?.apply {
            putParcelable(KEY_TRACK_SELECTOR_PARAMETERS, mTrackSelectorParameters)
            putBoolean(KEY_AUTO_PLAY, mStartAutoPlayer)
            putInt(KEY_WINDOW, mStartWindow)
            putLong(KEY_POSITION, mStartPosition)
        }
    }

    override fun onStart() {
        super.onStart()
        if (version > 23) {
            initializePlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        if (version > 23) releasePlayer()
    }

    override fun onVisibilityChange(visibility: Int) {
    }

    override fun preparePlayback() {
        initializePlayer()
    }

    private fun releasePlayer() {
        mPlayer?.let {
            updateTrackSelectorParameters()
            updateStartPosition()
            it.release()
            mTrackSelector = null
            mMediaSource = null
            mPlayer = null
        }

    }

    private fun updateStartPosition() {
        mPlayer?.let {
            mStartAutoPlayer = it.playWhenReady
            mStartWindow = it.currentWindowIndex
            mStartPosition = max(0, it.contentPosition)
        }
    }

    private fun updateTrackSelectorParameters() {
        mTrackSelector?.let {

            mTrackSelectorParameters = it.parameters
        }
    }

    companion object {


        private const val KEY_AUTO_PLAY = "auto_play"
        private const val KEY_WINDOW = "window"
        private const val KEY_POSITION = "position"
        private const val KEY_TRACK_SELECTOR_PARAMETERS = "track_selector_parameters"

    }
}