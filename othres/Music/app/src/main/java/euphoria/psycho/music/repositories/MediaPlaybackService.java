package euphoria.psycho.music.repositories;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.*;
import android.util.Log;
import android.widget.RemoteViews;
import euphoria.psycho.music.R;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MediaPlaybackService extends Service {

    private static final int FADEDOWN = 5;
    private static final int FADEUP = 6;
    private static final int FOCUSCHANGE = 4;
    private static final int IDLE_DELAY = 60000;
    private static final String LOGTAG = "MediaPlaybackService";
    private static final int RELEASE_WAKELOCK = 2;
    private static final int SERVER_DIED = 3;
    private static final int TRACK_ENDED = 1;
    private static final int TRACK_WENT_TO_NEXT = 7;
    private AudioManager mAudioManager;
    private File mCurrentDirectory;
    private String mFileToPlay;
    private boolean mIsSupposedToBePlaying = false;
    private AudioManager.OnAudioFocusChangeListener mOnAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {
            mMediaplayerHandler.obtainMessage(FOCUSCHANGE, focusChange, 0).sendToTarget();
        }
    };
    private boolean mPausedByTransientLossOfFocus;
    private List<String> mPlayList;
    private int mPlayListLen = 0;
    private int mPlayPos = -1;
    private MultiPlayer mPlayer;
    private int mRepeatMode = REPEAT_NONE;
    private int mServiceStartId = -1;
    private Handler mDelayedStopHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            if (isPlaying() || mPausedByTransientLossOfFocus) {
                return;
            }
            stopSelf(mServiceStartId);
        }
    };
    private PowerManager.WakeLock mWakeLock;
    private Handler mMediaplayerHandler = new Handler() {
        float mCurrentVolume = 1.0f;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case FADEDOWN:
                    mCurrentVolume -= .05f;
                    if (mCurrentVolume > .2f) {
                        mMediaplayerHandler.sendEmptyMessageDelayed(FADEDOWN, 10);
                    } else {
                        mCurrentVolume = .2f;
                    }
                    mPlayer.setVolume(mCurrentVolume);
                    break;
                case FADEUP:
                    mCurrentVolume += .01f;
                    if (mCurrentVolume < 1.0f) {
                        mMediaplayerHandler.sendEmptyMessageDelayed(FADEUP, 10);
                    } else {
                        mCurrentVolume = 1.0f;
                    }
                    mPlayer.setVolume(mCurrentVolume);
                    break;
                case TRACK_WENT_TO_NEXT:
                    mPlayPos = getNextPosition(true);
                    setNextTrack();
                    break;
                case TRACK_ENDED:
                    if (mRepeatMode == REPEAT_CURRENT) {
                        seek(0);
                        play();
                    } else {
                        gotoNext(false);
                    }
                    break;
                case RELEASE_WAKELOCK:
                    mWakeLock.release();
                    break;

                case FOCUSCHANGE:
                    // This code is here so we can better synchronize it with the code that
                    // handles fade-in
                    switch (msg.arg1) {
                        case AudioManager.AUDIOFOCUS_LOSS:
                            Log.v(LOGTAG, "AudioFocus: received AUDIOFOCUS_LOSS");
                            if (isPlaying()) {
                                mPausedByTransientLossOfFocus = false;
                            }
                            pause();
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                            mMediaplayerHandler.removeMessages(FADEUP);
                            mMediaplayerHandler.sendEmptyMessage(FADEDOWN);
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                            Log.v(LOGTAG, "AudioFocus: received AUDIOFOCUS_LOSS_TRANSIENT");
                            if (isPlaying()) {
                                mPausedByTransientLossOfFocus = true;
                            }
                            pause();
                            break;
                        case AudioManager.AUDIOFOCUS_GAIN:
                            Log.v(LOGTAG, "AudioFocus: received AUDIOFOCUS_GAIN");
                            if (!isPlaying() && mPausedByTransientLossOfFocus) {
                                mPausedByTransientLossOfFocus = false;
                                mCurrentVolume = 0f;
                                mPlayer.setVolume(mCurrentVolume);
                                play(); // also queues a fade-in
                            } else {
                                mMediaplayerHandler.removeMessages(FADEDOWN);
                                mMediaplayerHandler.sendEmptyMessage(FADEUP);
                            }
                            break;
                        default:
                            Log.e(LOGTAG, "Unknown audio focus change code");
                    }
                    break;

                default:
                    break;
            }

        }
    };
    public static final String KEY_PLAY_DIRECTORY = "play_directory";
    public static final String KEY_PLAY_POSITION = "play_position";
    public static final int REPEAT_ALL = 2;
    public static final int REPEAT_CURRENT = 1;
    public static final int REPEAT_NONE = 0;


    public void gotoNext(boolean force) {
        synchronized (this) {
            if (mPlayList.size() <= 0) {
                Log.d(LOGTAG, "No play queue");
                return;
            }

            int pos = getNextPosition(force);
            if (pos < 0) {
                gotoIdleState();
                if (mIsSupposedToBePlaying) {
                    mIsSupposedToBePlaying = false;

                }
                return;
            }
            mPlayPos = pos;
            stop(false);

            setNextTrack();
            play();
        }
    }

    public void gotoPosition(int position) {
        synchronized (this) {

            mPlayPos = position;
            stop(false);

            setNextTrack();
            play();
        }
    }

    public boolean isPlaying() {
        return mIsSupposedToBePlaying;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initializeAudioManager();
        initializeMediaPlayer();
        initializeWakeLock();

        // If the service was idle, but got killed before it stopped itself, the
        // system will relaunch it. Make sure it gets stopped again in that case.
        Message msg = mDelayedStopHandler.obtainMessage();
        mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mServiceStartId = startId;
        mDelayedStopHandler.removeCallbacksAndMessages(null);

        if (intent != null) {


            File file = new File(intent.getStringExtra(KEY_PLAY_DIRECTORY));

            mCurrentDirectory = file;
            mPlayList = AppHelper.listAudioFiles(file, false);
            if (mPlayList != null && mPlayList.size() > 0) {
                gotoPosition(intent.getIntExtra(KEY_PLAY_POSITION, 0));
            }

        }

        // make sure the service will shut down on its own if it was
        // just started but not bound to and nothing is playing
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        Message msg = mDelayedStopHandler.obtainMessage();
        mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
        /*
        Constant to return from onStartCommand(Intent, int, int):
        if this service's process is killed while it is started (after returning from onStartCommand(Intent, int, int)),
        then leave it in the started state but don't retain this delivered intent.
        Later the system will try to re-create the service.
        Because it is in the started state,
        it will guarantee to call onStartCommand(Intent, int, int) after creating
        the new service instance; if there are not any pending start commands to
        be delivered to the service, it will be called with a null intent object,
        so you must take care to check for this.
        */
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        disposeMediaPlayer();
        disposeAudioManager();
        disposeWakeLock();
        super.onDestroy();

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void pause() {
        synchronized (this) {
            mMediaplayerHandler.removeMessages(FADEUP);
            if (isPlaying()) {
                mPlayer.pause();
                gotoIdleState();
                mIsSupposedToBePlaying = false;

            }
        }
    }

    public void play() {
        mAudioManager.requestAudioFocus(
                mOnAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        if (mPlayer.isInitialized()) {

            mPlayer.start();
            mMediaplayerHandler.removeMessages(FADEDOWN);
            mMediaplayerHandler.sendEmptyMessage(FADEUP);
            updateNotification();
            if (!mIsSupposedToBePlaying) {
                mIsSupposedToBePlaying = true;
            }
        }
    }

    public long seek(long pos) {
        if (mPlayer.isInitialized()) {
            if (pos < 0) pos = 0;
            if (pos > mPlayer.duration()) pos = mPlayer.duration();
            return mPlayer.seek(pos);
        }
        return -1;
    }

    private int getNextPosition(boolean force) {
        if (mPlayPos < 0) return 0;

        if (mPlayPos + 1 < mPlayList.size()) {
            return ++mPlayPos;
        } else {
            return 0;
        }
    }

    private void setNextTrack() {

        if (mPlayPos >= 0) {
            mFileToPlay = mCurrentDirectory.getAbsolutePath() + "/" + mPlayList.get(mPlayPos);
            mPlayer.setDataSource(mFileToPlay);
        } else {
            mPlayer.setDataSource(null);

        }
    }

    private void openCurrentAndNext() {
        synchronized (this) {

            if (mPlayListLen == 0) {
                return;
            }
            stop(false);


            setNextTrack();
        }
    }

    private void stop(boolean remove_status_icon) {
        if (mPlayer != null && mPlayer.isInitialized()) {
            mPlayer.stop();
        }
        mFileToPlay = null;
        if (remove_status_icon) {
            gotoIdleState();
        } else {
            stopForeground(false);
        }
        if (remove_status_icon) {
            mIsSupposedToBePlaying = false;
        }
    }

    private void gotoIdleState() {
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        Message msg = mDelayedStopHandler.obtainMessage();
        mDelayedStopHandler.sendMessageDelayed(msg, IDLE_DELAY);
        stopForeground(true);
    }

    private void disposeAudioManager() {
        mAudioManager.abandonAudioFocus(mOnAudioFocusChangeListener);
    }

    private void disposeMediaPlayer() {
        mPlayer.release();
        mPlayer = null;
    }

    private void initializeMediaPlayer() {
        mPlayer = new MultiPlayer();
        mPlayer.setHandler(mMediaplayerHandler);

    }

    public static final int PLAYBACKSERVICE_STATUS = 1;

    private void updateNotification() {
        RemoteViews views = new RemoteViews(getPackageName(), R.layout.statusbar);
        views.setImageViewResource(R.id.icon, R.drawable.ic_play_arrow_black_36dp);
        views.setTextViewText(R.id.trackname, AppHelper.getFileName(mFileToPlay));

        Notification status = new Notification();
        status.contentView = views;
        status.flags |= Notification.FLAG_ONGOING_EVENT;
        status.icon = R.drawable.ic_play_arrow_black_36dp;
        startForeground(101, status);
    }

    private void disposeWakeLock() {
        mWakeLock.release();
    }

    private void initializeAudioManager() {
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

    }

    private void initializeWakeLock() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.getClass().getName());
        mWakeLock.setReferenceCounted(false);

    }

    private class MultiPlayer {
        private MediaPlayer mCurrentMediaPlayer = new MediaPlayer();
        private Handler mHandler;
        private boolean mIsInitialized = false;
        MediaPlayer.OnErrorListener errorListener = new MediaPlayer.OnErrorListener() {
            public boolean onError(MediaPlayer mp, int what, int extra) {
                switch (what) {
                    case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                        mIsInitialized = false;
                        mCurrentMediaPlayer.release();
                        // Creating a new MediaPlayer and settings its wakemode does not
                        // require the media service, so it's OK to do this now, while the
                        // service is still being restarted
                        mCurrentMediaPlayer = new CompatMediaPlayer();
                        mCurrentMediaPlayer.setWakeMode(
                                MediaPlaybackService.this, PowerManager.PARTIAL_WAKE_LOCK);
                        mHandler.sendMessageDelayed(mHandler.obtainMessage(SERVER_DIED), 2000);
                        return true;
                    default:
                        Log.d("MultiPlayer", "Error: " + what + "," + extra);
                        break;
                }
                return false;
            }
        };
        MediaPlayer.OnCompletionListener listener = new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {

                // Acquire a temporary wakelock, since when we return from
                // this callback the MediaPlayer will release its wakelock
                // and allow the device to go to sleep.
                // This temporary wakelock is released when the RELEASE_WAKELOCK
                // message is processed, but just in case, put a timeout on it.
                mWakeLock.acquire(30000);
                mHandler.sendEmptyMessage(TRACK_ENDED);
                mHandler.sendEmptyMessage(RELEASE_WAKELOCK);
            }
        };

        public long duration() {
            return mCurrentMediaPlayer.getDuration();
        }

        public int getAudioSessionId() {
            return mCurrentMediaPlayer.getAudioSessionId();
        }

        public void setAudioSessionId(int sessionId) {
            mCurrentMediaPlayer.setAudioSessionId(sessionId);
        }

        public boolean isInitialized() {
            return mIsInitialized;
        }

        public void pause() {
            mCurrentMediaPlayer.pause();
        }

        public long position() {
            return mCurrentMediaPlayer.getCurrentPosition();
        }

        /**
         * You CANNOT use this player anymore after calling release()
         */
        public void release() {
            stop();
            mCurrentMediaPlayer.release();
        }

        public long seek(long whereto) {
            mCurrentMediaPlayer.seekTo((int) whereto);
            return whereto;
        }

        public void setDataSource(String path) {
            mIsInitialized = setDataSourceImpl(mCurrentMediaPlayer, path);
            if (mIsInitialized) {
                setNextDataSource(null);
            }
        }

        public void setHandler(Handler handler) {
            mHandler = handler;
        }

        public void setNextDataSource(String path) {

            if (path == null) {
                return;
            }
            setDataSourceImpl(mCurrentMediaPlayer, path);
        }

        public void setVolume(float vol) {
            mCurrentMediaPlayer.setVolume(vol, vol);
        }

        public void start() {
            mCurrentMediaPlayer.start();
        }

        public void stop() {
            mCurrentMediaPlayer.reset();
            mIsInitialized = false;
        }

        public MultiPlayer() {
            mCurrentMediaPlayer.setWakeMode(
                    MediaPlaybackService.this, PowerManager.PARTIAL_WAKE_LOCK);
        }

        private boolean setDataSourceImpl(MediaPlayer player, String path) {
            try {
                player.reset();
                player.setOnPreparedListener(null);
                if (path.startsWith("content://")) {
                    player.setDataSource(MediaPlaybackService.this, Uri.parse(path));
                } else {
                    player.setDataSource(path);
                }
                player.setAudioStreamType(AudioManager.STREAM_MUSIC);
                player.prepare();
            } catch (IOException ex) {
                // TODO: notify the user why the file couldn't be opened
                return false;
            } catch (IllegalArgumentException ex) {
                // TODO: notify the user why the file couldn't be opened
                return false;
            }
            player.setOnCompletionListener(listener);
            player.setOnErrorListener(errorListener);
            Intent i = new Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
            i.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, getAudioSessionId());
            i.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getPackageName());
            sendBroadcast(i);
            return true;
        }
    }

    static class CompatMediaPlayer extends MediaPlayer implements MediaPlayer.OnCompletionListener {
        private boolean mCompatMode = true;
        private OnCompletionListener mCompletion;
        private MediaPlayer mNextPlayer;

        public void setNextMediaPlayer(MediaPlayer next) {
            if (mCompatMode) {
                mNextPlayer = next;
            } else {
                // Set the MediaPlayer to start when this MediaPlayer finishes playback (i.e. reaches the end of the stream).
                // The media framework will attempt to transition from this player to the next as seamlessly as possible.
                // The next player can be set at any time before completion, but shall be after setDataSource has been called successfully.
                // The next player must be prepared by the app, and the application should not call start() on it.
                // The next MediaPlayer must be different from 'this'. An exception will be thrown if next == this.
                // The application may call setNextMediaPlayer(null) to indicate no next player should be started at the end of playback.
                // If the current player is looping, it will keep looping and the next player will not be started.

                super.setNextMediaPlayer(next);
            }
        }

        @Override
        public void setOnCompletionListener(OnCompletionListener listener) {
            if (mCompatMode) {
                mCompletion = listener;
            } else {
                super.setOnCompletionListener(listener);
            }
        }

        public CompatMediaPlayer() {
            try {
                MediaPlayer.class.getMethod("setNextMediaPlayer", MediaPlayer.class);
                mCompatMode = false;
            } catch (NoSuchMethodException e) {
                mCompatMode = true;
                super.setOnCompletionListener(this);
            }
        }

        @Override
        public void onCompletion(MediaPlayer mp) {
            if (mNextPlayer != null) {
                // as it turns out, starting a new MediaPlayer on the completion
                // of a previous player ends up slightly overlapping the two
                // playbacks, so slightly delaying the start of the next player
                // gives a better user experience
                SystemClock.sleep(50);
                mNextPlayer.start();
            }
            mCompletion.onCompletion(this);
        }
    }

}
