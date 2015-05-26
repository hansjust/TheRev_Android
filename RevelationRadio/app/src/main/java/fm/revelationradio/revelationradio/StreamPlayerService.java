package fm.revelationradio.revelationradio;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

public class StreamPlayerService  extends Service
        implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, AudioManager.OnAudioFocusChangeListener
{
    private static final String ACTION_PLAY = "fm.revelationradio.revelationradio.StreamPlayerService.PLAY";
    private static String mURL;
    private static StreamPlayerService mInstance = null;

    private MediaPlayer mMediaPlayer = null;
    private int mBufferPosition;
    private static String mArtistName;
    private static String mSongTitle;
    private static String mSongPicUrl;

    NotificationManager mNotificationManager;
    Notification mNotification = null;
    final int NOTIFICATION_ID = 1;

    enum State {
        Retrieving,         // Retrieving music
        Stopped,            // Stopped, not prepared
        Preparing,          // Preparing, not ready to play
        Playing,            // Playback active (ready)
        Paused,              // Paused (ready)
        Error
    }

    State mState = State.Retrieving;

    @Override
    public void onCreate() {
        mInstance = this;
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.d("RevelationRadio", "Starting onStartCommand()");
        if (intent.getAction().equals(ACTION_PLAY)) {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnErrorListener(this);
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            initMediaPlayer();
        }
        return START_STICKY;
    }

    private boolean initMediaPlayer() {
        String streamURL = getString(R.string.media_URL);
        Log.v("RevelationRadio", "Initializing player with URL" + streamURL);
        try {
            mMediaPlayer.setDataSource(streamURL);
            mMediaPlayer.prepareAsync();
        } catch (Exception e) {
            Log.e("RevelationRadio", "Exception caught preparing stream!" + e.getMessage());
            e.printStackTrace();
            mState = State.Stopped;
            mMediaPlayer = null;
            return false;
        }
        Log.v("RevelationRadio", "Waiting for prepareAsync() to complete");
        mState = State.Preparing;

        return true;
   }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.v("RevelationRadio", "MediaPlayer is prepared");
        mState = State.Paused;
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mp.reset();
        mState = State.Error;
        return false;
    }

    @Override
    public void onDestroy() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        mState = State.Retrieving;
    }

    public MediaPlayer getMediaPlayer() {
        return mMediaPlayer;
    }

    public void pauseMusic() {
        if (mState.equals(State.Playing)) {
            mMediaPlayer.pause();
            mState = State.Paused;
            updateNotification(mSongTitle + "(paused)");
        }
    }

    public void startMusic() {
        Log.v("RevelationRadio", "MusicStart() called");

        if (!mState.equals(State.Preparing) &&!mState.equals(State.Retrieving)) {
            Log.v("RevelationRadio", "starting media player. State was " + mState);
            mMediaPlayer.start();
            mState = State.Playing;
            updateNotification(mSongTitle + "(playing)");
            setUpAsForeground("Playing");
        } else {
            Log.v("RevelationRadio", "NOT starting media player. State was " + mState);
        }
    }

    public boolean isPlaying() {
        if (mState.equals(State.Playing)) {
            return true;
        }
        return false;
    }



    public static StreamPlayerService getInstance() {
        return mInstance;
    }

    public static void setSong(String url, String title, String artist, String songPicUrl) {
        mURL = url;
        mSongTitle = title;
        mArtistName = artist;
        mSongPicUrl = songPicUrl;
    }

    void updateNotification(String text) {
        // Notify Notification Manager
        /*
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
                new Intent(getApplicationContext(), ListenLive.class),
                PendingIntent.FLAG_UPDATE_CURRENT);
        mNotification = new Notification();
        mNotification.tickerText = text;
        mNotification.icon = R.drawable.play0;
        mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
        mNotification.setLatestEventInfo(getApplicationContext(), "MusicPlayerSample",
                "Playing: " + songName, pi);
        startForeground(NOTIFICATION_ID, notification);
        */
    }

    void setUpAsForeground(String text) {
        PendingIntent pi =
                PendingIntent.getActivity(getApplicationContext(), 0, new Intent(getApplicationContext(), ListenLive.class),
                        PendingIntent.FLAG_UPDATE_CURRENT);
        mNotification = new Notification();
        mNotification.tickerText = text;
        mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
        mNotification.setLatestEventInfo(getApplicationContext(), getResources().getString(R.string.app_name), text, pi);
        startForeground(NOTIFICATION_ID, mNotification);
    }

    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                if (mMediaPlayer == null) initMediaPlayer();
                else if (!mMediaPlayer.isPlaying()) mMediaPlayer.start();
                mMediaPlayer.setVolume(1.0f, 1.0f);
                break;

            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and release media player
                if (mMediaPlayer.isPlaying()) mMediaPlayer.stop();
                mMediaPlayer.release();
                mMediaPlayer = null;
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (mMediaPlayer.isPlaying()) mMediaPlayer.pause();
                break;

            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (mMediaPlayer.isPlaying()) mMediaPlayer.setVolume(0.1f, 0.1f);
                break;
        }
    }


}
