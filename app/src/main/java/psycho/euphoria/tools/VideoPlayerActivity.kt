package psycho.euphoria.tools

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Pair
import android.view.KeyEvent
import android.view.View
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto
import com.google.android.exoplayer2.drm.UnsupportedDrmException
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerControlView
import com.google.android.exoplayer2.util.ErrorMessageProvider
import com.google.android.exoplayer2.util.Util
import kotlinx.android.synthetic.main.activity_video.*
import java.util.*
import com.google.android.exoplayer2.drm.FrameworkMediaDrm
import com.google.android.exoplayer2.drm.HttpMediaDrmCallback
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.RandomTrackSelection
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.FileDataSource
import com.google.android.exoplayer2.upstream.HttpDataSource
import psycho.euphoria.tools.commons.*
import java.io.File


class VideoPlayerActivity : AppCompatActivity(), View.OnClickListener, PlaybackPreparer, PlayerControlView.VisibilityListener {


    private var mStartAutoPlayer: Boolean = true
    private var mStartWindow = 0
    private var mStartPosition = 0L
    private lateinit var mTrackSelectorParameters: DefaultTrackSelector.Parameters
    private var mPlayer: SimpleExoPlayer? = null


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
            val trackSelector = DefaultTrackSelector(RandomTrackSelection.Factory())
            trackSelector.setParameters(mTrackSelectorParameters)
            mPlayer = ExoPlayerFactory.newSimpleInstance(DefaultRenderersFactory(this), trackSelector).apply {
                addListener(object : Player.DefaultEventListener() {
                    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {

                    }
                })
                player_view.player = this
            }

            player_view.setPlaybackPreparer(this)
        }

        val fileDataSource = FileDataSource()
        fileDataSource.open(DataSpec(Uri.fromFile(File(path))))
        val dataSourceFactory = DefaultDataSourceFactory(this, Util.getUserAgent(this, "VideoPlayerActivity"))
        val videoSource = ExtractorMediaSource(fileDataSource.uri, dataSourceFactory, DefaultExtractorsFactory(), null, null)
        mPlayer?.prepare(videoSource, false, false)

    }

    override fun onClick(view: View?) {
        view?.let {

        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        savedInstanceState?.let {
            mTrackSelectorParameters = it.getParcelable(KEY_TRACK_SELECTOR_PARAMETERS)
            mStartAutoPlayer = it.getBoolean(KEY_AUTO_PLAY)
            mStartWindow = it.getInt(KEY_WINDOW)
            mStartPosition = it.getLong(KEY_POSITION)
        }
        if (savedInstanceState == null) {
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
    }

    private fun updateStartPosition() {
    }

    private fun updateTrackSelectorParameters() {
    }

    companion object {
        private const val DRM_LICENSE_URL_EXTRA = ""
        private const val DRM_SCHEME_UUID_EXTRA = ""
        private const val DRM_KEY_REQUEST_PROPERTIES_EXTRA = ""
        private const val DRM_MULTI_SESSION_EXTRA = ""
        private const val KEY_AUTO_PLAY = "auto_play"
        private const val KEY_WINDOW = "window"
        private const val KEY_POSITION = "position"
        private const val KEY_TRACK_SELECTOR_PARAMETERS = "track_selector_parameters"
        const val ACTION_VIEW = "psycho.euphoria.tools.VIEW"
        const val EXTENSION_EXTRA = "extension"
        const val URI_LIST_EXTRA = "uri_list"
        const val ACTION_VIEW_LIST = "psycho.euphoria.tools.VIEW_LIST"
    }
}