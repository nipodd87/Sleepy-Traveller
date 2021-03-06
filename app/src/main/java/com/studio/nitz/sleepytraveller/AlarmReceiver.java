package com.studio.nitz.sleepytraveller;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Vibrator;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import java.io.IOException;

/**
 * Created by nitinpoddar on 4/29/16.
 */
public class AlarmReceiver extends Activity {
    private MediaPlayer mMediaPlayer;
    private PowerManager.WakeLock mWakeLock;
    public int userSelectedTone;
    private Vibrator vibrator;
    private long[] pattern = {0, 4000, 1000};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vibrator = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "My Wake Lock");
        mWakeLock.acquire();
        this.requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_FULLSCREEN |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        setContentView(R.layout.receiver_alarm);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.window_layout);
        playSound(this);
    }
    public void stopAlarm(View v){
        mMediaPlayer.stop();
        if (mWakeLock.isHeld()){
            mWakeLock.release();
        }
        vibrator.cancel();
        finish();
    }
    protected void playSound(Context context){
        vibrator.vibrate(pattern, 0);
        mMediaPlayer = new MediaPlayer();
        final AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            try {
                int selectedItem = MainActivity.mSharedPreferences.getInt(Constants.ALARM_TONE_KEY, 0);
                userSelectedTone = getSelectedAlarmTone(selectedItem);
                mMediaPlayer.setDataSource(context, Uri.parse("android.resource://com.studio.nitz.sleepytraveller/" + userSelectedTone));
                mMediaPlayer.setLooping(true);
                mMediaPlayer.prepare();
                mMediaPlayer.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public static int getSelectedAlarmTone(int selectedItem) {
        int userSelectedTone;
        switch (selectedItem){
            case 0:
                userSelectedTone = R.raw.alarm;
                break;
            case 1:
                userSelectedTone = R.raw.kalinka;
                break;
            case 2:
                userSelectedTone = R.raw.synth_dreams;
                break;
            case 3:
                userSelectedTone = R.raw.final_destination;
                break;
            case 4:
                userSelectedTone = R.raw.piano_dreams;
                break;
            case 5:
                userSelectedTone = R.raw.wakeup;
                break;
            default:
                userSelectedTone = R.raw.alarm;
        }
        return userSelectedTone;
    }
    protected void onStop(){
        super.onStop();
        if (mWakeLock.isHeld())
            mWakeLock.release();
    }
}
