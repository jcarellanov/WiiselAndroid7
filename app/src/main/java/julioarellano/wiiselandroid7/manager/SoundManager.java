package julioarellano.wiiselandroid7.manager;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;

import com.android.wiisel.R;
import com.android.wiisel.activity.Dialog;
import com.android.wiisel.activity.interfaces.SoundManagerListener;

public class SoundManager {

    private static volatile SoundManager instance;
    private SoundManagerListener soundManagerListener;
    private Context context;
    private MediaPlayer mPlayer = null;
    private AudioManager audioManager = null;

    private SoundManager(Context context, SoundManagerListener soundManagerListener) {
        this.context = context;
        this.soundManagerListener = soundManagerListener;
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        setVolume(10);
    }

    public static SoundManager getInstance(Context context, SoundManagerListener soundManagerListener) {
        SoundManager localInstance = instance;
        if (localInstance == null) {
            synchronized (SoundManager.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new SoundManager(context.getApplicationContext(), soundManagerListener);
                }
            }
        }
        return localInstance;
    }

    public static SoundManager getSoundManagerWithoutCreating() {
        return instance;
    }

    public void start() {
        Intent intent = new Intent(context, Dialog.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);

        mPlayer = MediaPlayer.create(context, R.raw.alarm_was_detected);
        mPlayer.start();
        mPlayer.setOnCompletionListener(new OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                finishUserFell();
            }
        });
    }

    public void stop() {
        if (mPlayer != null) {
            if (mPlayer.isPlaying()) {
                mPlayer.stop();
            }
            mPlayer.release();
            mPlayer = null;
        }
        soundManagerListener.spoped();

        if (Dialog.activity != null) {
            Dialog dialog = Dialog.activity.get();
            if (dialog != null) {
                dialog.finish();
            }
        }

        soundManagerListener = null;
        instance = null;
    }

    public void finishUserFell() {
        if (soundManagerListener != null) {
            soundManagerListener.finished();
        }
        stop();
    }

    public void setVolume(float value) {
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (int) value, 0);
    }

}
