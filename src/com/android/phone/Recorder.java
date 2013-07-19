package com.android.phone;

/* Begin: Modified by zxiaona for recording 2012/06/20 */

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnErrorListener;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

public class Recorder implements OnErrorListener {
    private static final String TAG = "Recorder";

    static final String SAMPLE_PREFIX = "recording";
    static final String SAMPLE_PATH_KEY = "sample_path";
    static final String SAMPLE_LENGTH_KEY = "sample_length";

    public static final int IDLE_STATE = 0;
    public static final int RECORDING_STATE = 1;
    public static final int PLAYING_STATE = 2;

    int mState = IDLE_STATE;

    public static final int NO_ERROR = 0;
    public static final int SDCARD_ACCESS_ERROR = 1;
    public static final int INTERNAL_ERROR = 2;
    private Context mContext = null;
    public interface OnStateChangedListener {
        public void onStateChanged(int state);

        public void onError(int error);
    }

    OnStateChangedListener mOnStateChangedListener = null;

    long mSampleStart = 0; // time at which latest record or play operation
                           // started
    int mSampleLength = 0; // length of current sample
    File mSampleFile = null;
    MediaRecorder mRecorder = null;

    public Recorder() {
    }

    public void saveState(Bundle recorderState) {
        recorderState.putString(SAMPLE_PATH_KEY, mSampleFile.getAbsolutePath());
        recorderState.putInt(SAMPLE_LENGTH_KEY, mSampleLength);
    }

    public int getMaxAmplitude() {
        if (mState != RECORDING_STATE)
            return 0;
        return mRecorder.getMaxAmplitude();
    }

    public void restoreState(Bundle recorderState) {
        String samplePath = recorderState.getString(SAMPLE_PATH_KEY);
        if (samplePath == null)
            return;
        int sampleLength = recorderState.getInt(SAMPLE_LENGTH_KEY, -1);
        if (sampleLength == -1)
            return;

        File file = new File(samplePath);
        if (!file.exists())
            return;
        if (mSampleFile != null
                && mSampleFile.getAbsolutePath().compareTo(
                        file.getAbsolutePath()) == 0)
            return;

        delete();
        mSampleFile = file;
        mSampleLength = sampleLength;

        signalStateChanged(IDLE_STATE);
    }

    public void setOnStateChangedListener(OnStateChangedListener listener) {
        mOnStateChangedListener = listener;
    }

    public int state() {
        return mState;
    }

    public int progress() {
        if (mState == RECORDING_STATE || mState == PLAYING_STATE)
            return (int) ((System.currentTimeMillis() - mSampleStart) / 1000);
        return 0;
    }

    public int sampleLength() {
        return mSampleLength;
    }

    public File sampleFile() {
        return mSampleFile;
    }

    /**
     * Resets the recorder state. If a sample was recorded, the file is deleted.
     */
    public void delete() {
        stop();

        if (mSampleFile != null)
            mSampleFile.delete();
        mSampleFile = null;
        mSampleLength = 0;

        signalStateChanged(IDLE_STATE);
    }

    /**
     * Resets the recorder state. If a sample was recorded, the file is left on
     * disk and will be reused for a new recording.
     */
    public void clear() {
        stop();

        mSampleLength = 0;

        signalStateChanged(IDLE_STATE);
    }

    public void startRecording(int outputfileformat, String extension) {
        log("startRecording");
        stop();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
        String prefix = dateFormat.format(new Date());
        File sampleDir = Environment.getExternalStorageDirectory();

        if (!sampleDir.canWrite()) {
            Log.i(TAG, "----- file can't write!! ---");
            // Workaround for broken sdcard support on the device.
            sampleDir = new File("/sdcard/sdcard");
        }

        sampleDir = new File(sampleDir.getAbsolutePath() + "/PhoneRecord");
        if (sampleDir.exists() == false) {
            sampleDir.mkdirs();
        }

        try {
            mSampleFile = File.createTempFile(prefix, extension, sampleDir);
        } catch (IOException e) {
            setError(SDCARD_ACCESS_ERROR);
            Log.i(TAG, "----***------- can't access sdcard !!");
            return;
        }

        mRecorder = new MediaRecorder();
        mRecorder.setOnErrorListener(this);
//        mRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_DOWNLINK
//                | MediaRecorder.AudioSource.VOICE_UPLINK);
        mRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
        mRecorder.setOutputFormat(outputfileformat);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mRecorder.setOutputFile(mSampleFile.getAbsolutePath());

        try {
            mRecorder.prepare();
            mRecorder.start();
            mSampleStart = System.currentTimeMillis();
            setState(RECORDING_STATE);
        } catch (IOException exception) {
            log("startRecording, IOException");
            setError(INTERNAL_ERROR);
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;
            return;
        } catch (RuntimeException exception) {
            log("startRecording, RuntimeException");
            setError(INTERNAL_ERROR);
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;
            return;
        }
    }

    public void stopRecording() {
        log("stopRecording");
        if (mRecorder == null) {
            return;
        }
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;

        mSampleLength = (int) ((System.currentTimeMillis() - mSampleStart) / 1000);
        setState(IDLE_STATE);
    }

    public void stop() {
        log("stop");
        stopRecording();
    }

    private void setState(int state) {
        if (state == mState)
            return;

        mState = state;
        signalStateChanged(mState);
    }

    private void signalStateChanged(int state) {
        if (mOnStateChangedListener != null)
            mOnStateChangedListener.onStateChanged(state);
    }

    private void setError(int error) {
        if (mOnStateChangedListener != null)
            mOnStateChangedListener.onError(error);
    }

    /**
     * error listener
     */
    public void onError(MediaRecorder mp, int what, int extra) {
        log("onError");
        if (what == MediaRecorder.MEDIA_RECORDER_ERROR_UNKNOWN) {
            stop();
            // TODO show hint view
        }
        return;
    }

    public void log(String msg) {
        Log.d(TAG, msg);
    }
}
/* End: Modified by zxiaona for recording 2012/06/20 */

