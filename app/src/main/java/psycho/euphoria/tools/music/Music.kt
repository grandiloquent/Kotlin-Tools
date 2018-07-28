package psycho.euphoria.tools.music

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.os.*
import android.support.v4.app.NotificationCompat
import android.util.Log
import android.widget.RemoteViews
import psycho.euphoria.tools.R
import psycho.euphoria.tools.downloads.DownloadService
import psycho.euphoria.tools.getFileName
import psycho.euphoria.tools.listAudioFiles
import java.io.File
import java.io.IOException

class MediaPlaybackService : Service() {
    private var mAudioManager: AudioManager? = null
    private var mCurrentDirectory: File? = null
    private var mFileToPlay: String? = null
    var isPlaying = false
        private set
    private val mOnAudioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange -> mMediaplayerHandler.obtainMessage(FOCUSCHANGE, focusChange, 0).sendToTarget() }
    private var mPausedByTransientLossOfFocus: Boolean = false
    private var mPlayList: List<String>? = null
    private val mPlayListLen = 0
    private var mPlayPos = -1
    private var mPlayer: MultiPlayer? = null
    private val mRepeatMode = REPEAT_NONE
    private var mServiceStartId = -1
    private lateinit var mNotificationBuilder: NotificationCompat.Builder
    private lateinit var mNotificationManager: NotificationManager

    private val mDelayedStopHandler = object : Handler() {
        override fun handleMessage(msg: Message) {
            if (isPlaying || mPausedByTransientLossOfFocus) {
                return
            }
            stopSelf(mServiceStartId)
        }
    }
    private var mWakeLock: PowerManager.WakeLock? = null
    private val mMediaplayerHandler = object : Handler() {
        internal var mCurrentVolume = 1.0f
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                FADEDOWN -> {
                    mCurrentVolume -= .05f
                    if (mCurrentVolume > .2f) {
                        this.sendEmptyMessageDelayed(FADEDOWN, 10)
                    } else {
                        mCurrentVolume = .2f
                    }
                    mPlayer!!.setVolume(mCurrentVolume)
                }
                FADEUP -> {
                    mCurrentVolume += .01f
                    if (mCurrentVolume < 1.0f) {
                        this.sendEmptyMessageDelayed(FADEUP, 10)
                    } else {
                        mCurrentVolume = 1.0f
                    }
                    mPlayer!!.setVolume(mCurrentVolume)
                }
                TRACK_WENT_TO_NEXT -> {
                    mPlayPos = getNextPosition(true)
                    setNextTrack()
                }
                TRACK_ENDED -> if (mRepeatMode == REPEAT_CURRENT) {
                    seek(0)
                    play()
                } else {
                    gotoNext(false)
                }
                RELEASE_WAKELOCK -> mWakeLock!!.release()
                FOCUSCHANGE ->
                    when (msg.arg1) {
                        AudioManager.AUDIOFOCUS_LOSS -> {
                            Log.v(LOGTAG, "AudioFocus: received AUDIOFOCUS_LOSS")
                            if (isPlaying) {
                                mPausedByTransientLossOfFocus = false
                            }
                            pause()
                        }
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                            this.removeMessages(FADEUP)
                            this.sendEmptyMessage(FADEDOWN)
                        }
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                            Log.v(LOGTAG, "AudioFocus: received AUDIOFOCUS_LOSS_TRANSIENT")
                            if (isPlaying) {
                                mPausedByTransientLossOfFocus = true
                            }
                            pause()
                        }
                        AudioManager.AUDIOFOCUS_GAIN -> {
                            Log.v(LOGTAG, "AudioFocus: received AUDIOFOCUS_GAIN")
                            if (!isPlaying && mPausedByTransientLossOfFocus) {
                                mPausedByTransientLossOfFocus = false
                                mCurrentVolume = 0f
                                mPlayer!!.setVolume(mCurrentVolume)
                                play()
                            } else {
                                this.removeMessages(FADEDOWN)
                                this.sendEmptyMessage(FADEUP)
                            }
                        }
                        else -> Log.e(LOGTAG, "Unknown audio focus change code")
                    }
                else -> {
                }
            }
        }
    }

    private fun disposeAudioManager() {
        mAudioManager!!.abandonAudioFocus(mOnAudioFocusChangeListener)
    }

    private fun disposeMediaPlayer() {
        mPlayer!!.release()
        mPlayer = null
    }

    private fun disposeWakeLock() {
        mWakeLock!!.release()
    }

    private fun createNotificationChannel(): String {
        if (Build.VERSION.SDK_INT >= 26) {
            // NotificationManager.IMPORTANCE_NONE Turn off the notification sound
            mNotificationManager.createNotificationChannel(NotificationChannel(CHANNEL_ACTIVE,
                    TAG, NotificationManager.IMPORTANCE_NONE).also {
                it.lightColor = Color.BLUE
                it.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            })
        }
        return CHANNEL_ACTIVE
    }

    private fun getNextPosition(force: Boolean): Int {
        if (mPlayPos < 0) return 0
        return if (mPlayPos + 1 < mPlayList!!.size) {
            ++mPlayPos
        } else {
            0
        }
    }

    private fun gotoIdleState() {
        mDelayedStopHandler.removeCallbacksAndMessages(null)
        val msg = mDelayedStopHandler.obtainMessage()
        mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY.toLong())
        stopForeground(true)
    }

    fun gotoNext(force: Boolean) {
        synchronized(this) {
            if (mPlayList!!.size <= 0) {
                Log.d(LOGTAG, "No play queue")
                return
            }
            val pos = getNextPosition(force)
            if (pos < 0) {
                gotoIdleState()
                if (isPlaying) {
                    isPlaying = false
                }
                return
            }
            mPlayPos = pos
            stop(false)
            setNextTrack()
            play()
        }
    }

    fun gotoPosition(position: Int) {
        synchronized(this) {
            mPlayPos = position
            stop(false)
            setNextTrack()
            play()
        }
    }

    private fun initializeAudioManager() {
        mAudioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    private fun initializeMediaPlayer() {
        mPlayer = MultiPlayer()
        mPlayer!!.setHandler(mMediaplayerHandler)
    }

    private fun initializeWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.javaClass.name)
        mWakeLock!!.setReferenceCounted(false)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        initializeAudioManager()
        initializeMediaPlayer()
        initializeWakeLock()
        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val msg = mDelayedStopHandler.obtainMessage()
        mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY.toLong())
    }

    override fun onDestroy() {
        disposeMediaPlayer()
        disposeAudioManager()
        disposeWakeLock()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        mServiceStartId = startId
        mDelayedStopHandler.removeCallbacksAndMessages(null)
        if (intent != null) {
            val file = File(intent.getStringExtra(KEY_PLAY_DIRECTORY))
            mCurrentDirectory = file
            mPlayList = listAudioFiles(file, false)
            if (mPlayList != null && mPlayList!!.size > 0) {
                gotoPosition(intent.getIntExtra(KEY_PLAY_POSITION, 0))
            }
        }
        mDelayedStopHandler.removeCallbacksAndMessages(null)
        val msg = mDelayedStopHandler.obtainMessage()
        mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY.toLong())
        return Service.START_STICKY
    }

    private fun openCurrentAndNext() {
        synchronized(this) {
            if (mPlayListLen == 0) {
                return
            }
            stop(false)
            setNextTrack()
        }
    }

    fun pause() {
        synchronized(this) {
            mMediaplayerHandler.removeMessages(FADEUP)
            if (isPlaying) {
                mPlayer!!.pause()
                gotoIdleState()
                isPlaying = false
            }
        }
    }

    fun play() {
        mAudioManager!!.requestAudioFocus(
                mOnAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        if (mPlayer!!.isInitialized) {
            mPlayer!!.start()
            mMediaplayerHandler.removeMessages(FADEDOWN)
            mMediaplayerHandler.sendEmptyMessage(FADEUP)
            updateNotification()
            if (!isPlaying) {
                isPlaying = true
            }
        }
    }

    fun seek(pos: Long): Long {
        var pos = pos
        if (mPlayer!!.isInitialized) {
            if (pos < 0) pos = 0
            if (pos > mPlayer!!.duration()) pos = mPlayer!!.duration()
            return mPlayer!!.seek(pos)
        }
        return -1
    }

    private fun setNextTrack() {
        if (mPlayPos >= 0) {
            mFileToPlay = mCurrentDirectory!!.absolutePath + "/" + mPlayList!![mPlayPos]
            mPlayer!!.setDataSource(mFileToPlay)
        } else {
            mPlayer!!.setDataSource(null)
        }
    }

    private fun stop(remove_status_icon: Boolean) {
        if (mPlayer != null && mPlayer!!.isInitialized) {
            mPlayer!!.stop()
        }
        mFileToPlay = null
        if (remove_status_icon) {
            gotoIdleState()
        } else {
            stopForeground(false)
        }
        if (remove_status_icon) {
            isPlaying = false
        }
    }

    private fun updateNotification() {
        val views = RemoteViews(packageName, R.layout.statusbar)
        views.setImageViewResource(R.id.icon, R.mipmap.ic_play_arrow_black_36dp)
        views.setTextViewText(R.id.trackname, getFileName(mFileToPlay))
        mNotificationBuilder = NotificationCompat.Builder(this, CHANNEL_ACTIVE)
                .setSmallIcon(R.mipmap.ic_play_arrow_black_36dp)
                .setContentTitle("音乐")
                .setCustomContentView(views)
                //.setContentText(title)
                .setWhen(System.currentTimeMillis())
//        val status = Notification()
//        status.contentView = views
//        status.flags = status.flags or Notification.FLAG_ONGOING_EVENT
//        status.icon = R.mipmap.ic_play_arrow_black_36dp
        startForeground(101, mNotificationBuilder.build())
    }

    private inner class MultiPlayer {
        private var mCurrentMediaPlayer = MediaPlayer()
        private var mHandler: Handler? = null
        var isInitialized = false
            private set
        internal var errorListener: MediaPlayer.OnErrorListener = MediaPlayer.OnErrorListener { mp, what, extra ->
            when (what) {
                MediaPlayer.MEDIA_ERROR_SERVER_DIED -> {
                    isInitialized = false
                    mCurrentMediaPlayer.release()
                    mCurrentMediaPlayer = CompatMediaPlayer()
                    mCurrentMediaPlayer.setWakeMode(
                            this@MediaPlaybackService, PowerManager.PARTIAL_WAKE_LOCK)
                    mHandler!!.sendMessageDelayed(mHandler!!.obtainMessage(SERVER_DIED), 2000)
                    return@OnErrorListener true
                }
                else -> Log.d("MultiPlayer", "Error: $what,$extra")
            }
            false
        }
        internal var listener: MediaPlayer.OnCompletionListener = MediaPlayer.OnCompletionListener {
            mWakeLock!!.acquire(30000)
            mHandler!!.sendEmptyMessage(TRACK_ENDED)
            mHandler!!.sendEmptyMessage(RELEASE_WAKELOCK)
        }
        var audioSessionId: Int
            get() = mCurrentMediaPlayer.audioSessionId
            set(sessionId) {
                mCurrentMediaPlayer.audioSessionId = sessionId
            }

        fun duration(): Long {
            return mCurrentMediaPlayer.duration.toLong()
        }

        fun pause() {
            mCurrentMediaPlayer.pause()
        }

        fun position(): Long {
            return mCurrentMediaPlayer.currentPosition.toLong()
        }

        fun release() {
            stop()
            mCurrentMediaPlayer.release()
        }

        fun seek(whereto: Long): Long {
            mCurrentMediaPlayer.seekTo(whereto.toInt())
            return whereto
        }

        fun setDataSource(path: String?) {
            isInitialized = setDataSourceImpl(mCurrentMediaPlayer, path!!)
            if (isInitialized) {
                setNextDataSource(null)
            }
        }

        fun setHandler(handler: Handler) {
            mHandler = handler
        }

        fun setNextDataSource(path: String?) {
            if (path == null) {
                return
            }
            setDataSourceImpl(mCurrentMediaPlayer, path)
        }

        fun setVolume(vol: Float) {
            mCurrentMediaPlayer.setVolume(vol, vol)
        }

        fun start() {
            mCurrentMediaPlayer.start()
        }

        fun stop() {
            mCurrentMediaPlayer.reset()
            isInitialized = false
        }

        init {
            mCurrentMediaPlayer.setWakeMode(
                    this@MediaPlaybackService, PowerManager.PARTIAL_WAKE_LOCK)
        }

        private fun setDataSourceImpl(player: MediaPlayer, path: String): Boolean {
            try {
                player.reset()
                player.setOnPreparedListener(null)
                if (path.startsWith("content://")) {
                    player.setDataSource(this@MediaPlaybackService, Uri.parse(path))
                } else {
                    player.setDataSource(path)
                }
                player.setAudioStreamType(AudioManager.STREAM_MUSIC)
                player.prepare()
            } catch (ex: IOException) {
                return false
            } catch (ex: IllegalArgumentException) {
                return false
            }
            player.setOnCompletionListener(listener)
            player.setOnErrorListener(errorListener)
            val i = Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)
            i.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
            i.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
            sendBroadcast(i)
            return true
        }
    }

    internal class CompatMediaPlayer : MediaPlayer(), MediaPlayer.OnCompletionListener {
        private var mCompatMode = true
        private var mCompletion: MediaPlayer.OnCompletionListener? = null
        private var mNextPlayer: MediaPlayer? = null
        override fun setNextMediaPlayer(next: MediaPlayer) {
            if (mCompatMode) {
                mNextPlayer = next
            } else {
                super.setNextMediaPlayer(next)
            }
        }

        override fun setOnCompletionListener(listener: MediaPlayer.OnCompletionListener) {
            if (mCompatMode) {
                mCompletion = listener
            } else {
                super.setOnCompletionListener(listener)
            }
        }

        init {
            try {
                MediaPlayer::class.java.getMethod("setNextMediaPlayer", MediaPlayer::class.java)
                mCompatMode = false
            } catch (e: NoSuchMethodException) {
                mCompatMode = true
                super.setOnCompletionListener(this)
            }
        }

        override fun onCompletion(mp: MediaPlayer) {
            if (mNextPlayer != null) {
                SystemClock.sleep(50)
                mNextPlayer!!.start()
            }
            mCompletion!!.onCompletion(this)
        }
    }

    companion object {
        private const val FADEDOWN = 5
        private const val FADEUP = 6
        private const val FOCUSCHANGE = 4
        private const val IDLE_DELAY = 60000
        private const val LOGTAG = "MediaPlaybackService"
        private const val RELEASE_WAKELOCK = 2
        private const val SERVER_DIED = 3
        private const val TRACK_ENDED = 1
        private const val TRACK_WENT_TO_NEXT = 7
        const val KEY_PLAY_DIRECTORY = "play_directory"
        const val KEY_PLAY_POSITION = "play_position"
        const val REPEAT_ALL = 2
        const val REPEAT_CURRENT = 1
        const val REPEAT_NONE = 0
        const val PLAYBACKSERVICE_STATUS = 1


        private const val TAG = "MediaPlaybackService"
        private const val CHANNEL_ACTIVE = "active"


    }
}