package com.example.myxposedmod;

import android.app.Service;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.IBinder;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Created by fg607 on 15-7-6.
 */
public class VoiceRecordService extends Service  implements MediaRecorder.OnErrorListener {

    public static final int BITRATE_AMR = 2 * 1024 * 8; // bits/sec

    public static final int BITRATE_3GPP = 20 * 1024 * 8; // bits/sec

    public final static String ACTION_TOGGLE_VOICERECORD = "myxposedmod.intent.action.TOGGLE_VOICERECORD";


    public static final int TORCH_STATUS_OFF = 0;
    public static final int TORCH_STATUS_ON = 1;
    private int mTorchStatus = TORCH_STATUS_OFF;

    private static MediaRecorder mRecorder = null;
    private DateFormat mDateFormat = DateFormat.getDateInstance();


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mRecorder = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null &&
                intent.getAction().equals(ACTION_TOGGLE_VOICERECORD)) {
            toggleVoiceRecord();
            return START_STICKY;
        }

        stopSelf();
        return START_NOT_STICKY;
    }

    private  void toggleVoiceRecord() {
        if (mTorchStatus != TORCH_STATUS_ON) {
            startRecord();
        } else {
            stopRecord();
        }
    }


    private void startRecord() {

        if (mRecorder == null) {

            int outputfileformat = MediaRecorder.OutputFormat.AMR_WB;
            String sdcard = Environment.getExternalStorageDirectory().getAbsolutePath();
            String  date =  mDateFormat.format(new    java.util.Date());
            String path = sdcard+"/"+date+".amr";
            boolean highQuality = true;
            mRecorder = new MediaRecorder();
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            if (outputfileformat == MediaRecorder.OutputFormat.THREE_GPP) {
                mRecorder.setAudioSamplingRate(highQuality ? 44100 : 22050);
                mRecorder.setOutputFormat(outputfileformat);
                mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            } else {
                mRecorder.setAudioSamplingRate(highQuality ? 16000 : 8000);
                mRecorder.setOutputFormat(outputfileformat);
                mRecorder.setAudioEncoder(highQuality ? MediaRecorder.AudioEncoder.AMR_WB
                        : MediaRecorder.AudioEncoder.AMR_NB);
            }
            mRecorder.setOutputFile(path);
            mRecorder.setOnErrorListener(this);

            // Handle IOException
            try {
                mRecorder.prepare();
            } catch (IOException exception) {
                mRecorder.reset();
                mRecorder.release();
                mRecorder = null;
                return;
            }
            // Handle RuntimeException if the recording couldn't start
            try {
                mRecorder.start();
            } catch (RuntimeException exception) {
                mRecorder.reset();
                mRecorder.release();
                mRecorder = null;
                return;
            }
            mTorchStatus = TORCH_STATUS_ON;
        }
    }

    private void stopRecord() {
        if (mRecorder != null) {
            try {
                mRecorder.stop();
                mTorchStatus = TORCH_STATUS_OFF;
            } catch (RuntimeException e) {
            }
            mRecorder.release();
            mRecorder = null;
        }
        stopSelf();
    }

    @Override
    public void onError(MediaRecorder mediaRecorder, int i, int i1) {
        stopRecord();
    }
}

