/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.phone;

import android.content.Context;
import android.os.Debug;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;

import android.util.Log;

import java.io.File;
import java.util.List;

/**
 * Helper class used to keep track of various "elapsed time" indications
 * in the Phone app, and also to start and stop tracing / profiling.
 */
public class CallTime extends Handler {
    private static final String LOG_TAG = "PHONE/CallTime";
    private static final boolean DBG = true;
    /* package */ static final boolean PROFILE = true;

    private static final int PROFILE_STATE_NONE = 0;
    private static final int PROFILE_STATE_READY = 1;
    private static final int PROFILE_STATE_RUNNING = 2;

    private static int sProfileState = PROFILE_STATE_NONE;

    private Call mCall;
    private long mLastReportedTime;
    private boolean mTimerRunning;
    private long mInterval;
    private PeriodicTimerCallback mTimerCallback;
    private OnTickListener mListener;
    static long resetTime = 0;
    interface OnTickListener {
        void onTickForCallTimeElapsed(long timeElapsed);
    }
    /*ren_kang.hoperun 2012.7.11 change for show the right time of call*/
    @Override
    public void handleMessage(Message msg) {
        Log.i(LOG_TAG, "rk_handler");
        super.handleMessage(msg);
        String s = msg.obj.toString();
        long l = Long.parseLong(s);
        mListener.onTickForCallTimeElapsed(l);
        }
    /*ren_kang.hoperun 2012.7.11 end*/

    public CallTime(OnTickListener listener) {
        mListener = listener;
        mTimerCallback = new PeriodicTimerCallback();
    }

    /**
     * Sets the call timer to "active call" mode, where the timer will
     * periodically update the UI to show how long the specified call
     * has been active.
     *
     * After calling this you should also call reset() and
     * periodicUpdateTimer() to get the timer started.
     */
    /* package */ void setActiveCallMode(Call call) {
        if (DBG) log("setActiveCallMode(" + call + ")...");
        mCall = call;

        // How frequently should we update the UI?
        mInterval = 1000;  // once per second
    }

    /* package */ void reset() {
        if (DBG) log("reset()...");
        mLastReportedTime = SystemClock.uptimeMillis() - mInterval;
        /*ren_kang.hoperun 2012.7.11 add for show the right time of call*/
    }

    /* package */ void periodicUpdateTimer() {
        if (!mTimerRunning) {
            mTimerRunning = true;

            long now = SystemClock.uptimeMillis();
            long nextReport = mLastReportedTime + mInterval;

            while (now >= nextReport) {
                nextReport += mInterval;
            }

            if (DBG) log("periodicUpdateTimer() @ " + nextReport);
            postAtTime(mTimerCallback, nextReport);
            mLastReportedTime = nextReport;

            if (mCall != null) {
                Call.State state = mCall.getState();

                if (state == Call.State.ACTIVE) {
                    updateElapsedTime(mCall);
                }
            }

            if (PROFILE && isTraceReady()) {
                Log.i(LOG_TAG, "PROFILE && isTraceReady()");
                startTrace();
            }
        } else {
            if (DBG) log("periodicUpdateTimer: timer already running, bail");
        }
    }

    /* package */ void cancelTimer() {
        if (DBG) log("cancelTimer()...");
        removeCallbacks(mTimerCallback);
        mTimerRunning = false;
    }

    private void updateElapsedTime(Call call) {
        if (mListener != null) {
            long duration = getCallDuration(call);
            mListener.onTickForCallTimeElapsed(duration / 1000);
            /*ren_kang.hoperun 2012.7.11 change for show the right time of call*/
            ResetTimer.interrupted();
            /*ren_kang.hoperun 2012.7.11 end*/
        }
    }

    /**
     * Returns a "call duration" value for the specified Call, in msec,
     * suitable for display in the UI.
     */
    /* package */ static long getCallDuration(Call call) {
        long duration = 0;
        List connections = call.getConnections();
        int count = connections.size();
        Connection c;

        if (count == 1) {
            Log.i(LOG_TAG, "rk_if (count == 1)");
            c = (Connection) connections.get(0);
            //duration = (state == Call.State.ACTIVE
            //            ? c.getDurationMillis() : c.getHoldDurationMillis());
            duration = c.getDurationMillis();
        } else {
            for (int i = 0; i < count; i++) {
                c = (Connection) connections.get(i);
                //long t = (state == Call.State.ACTIVE
                //          ? c.getDurationMillis() : c.getHoldDurationMillis());
                long t = c.getDurationMillis();
                if (t > duration) {
                    duration = t;
                }
            }
        }

        if (DBG) log("updateElapsedTime, count=" + count + ", duration=" + duration);
        return duration;
    }

    private static void log(String msg) {
        Log.d(LOG_TAG, "[CallTime] " + msg);
    }

    public class PeriodicTimerCallback implements Runnable {
        PeriodicTimerCallback() {
        }

        public void run() {
            if (PROFILE && isTraceRunning()) {
                Log.i(LOG_TAG, "rk_stopTrace");
                stopTrace();
            }
            /*ren_kang.hoperun 2012.7.11 change for show the right time of call*/
            if(CallCard.shouldreset == 0)
            {
                Log.i(LOG_TAG, "rk_CallCard.shouldreset == 0");
                resetTime = 0;
                mTimerRunning = false;
                periodicUpdateTimer();
            }
            if(CallCard.shouldreset == 1)
            {
                Log.i(LOG_TAG, "rk_new ResetTimer().start()");
                new ResetTimer().start();
            }
            /*ren_kang.hoperun 2012.7.11 end*/
        }
    }

    static void setTraceReady() {
        if (sProfileState == PROFILE_STATE_NONE) {
            sProfileState = PROFILE_STATE_READY;
            log("trace ready...");
        } else {
            log("current trace state = " + sProfileState);
        }
    }

    boolean isTraceReady() {
        return sProfileState == PROFILE_STATE_READY;
    }

    boolean isTraceRunning() {
        return sProfileState == PROFILE_STATE_RUNNING;
    }

    void startTrace() {
        if (PROFILE & sProfileState == PROFILE_STATE_READY) {
            // For now, we move away from temp directory in favor of
            // the application's data directory to store the trace
            // information (/data/data/com.android.phone).
            File file = PhoneApp.getInstance().getDir ("phoneTrace", Context.MODE_PRIVATE);
            if (file.exists() == false) {
                file.mkdirs();
            }
            String baseName = file.getPath() + File.separator + "callstate";
            String dataFile = baseName + ".data";
            String keyFile = baseName + ".key";

            file = new File(dataFile);
            if (file.exists() == true) {
                file.delete();
            }

            file = new File(keyFile);
            if (file.exists() == true) {
                file.delete();
            }

            sProfileState = PROFILE_STATE_RUNNING;
            log("startTrace");
            Debug.startMethodTracing(baseName, 8 * 1024 * 1024);
        }
    }

    void stopTrace() {
        if (PROFILE) {
            if (sProfileState == PROFILE_STATE_RUNNING) {
                sProfileState = PROFILE_STATE_NONE;
                log("stopTrace");
                Debug.stopMethodTracing();
            }
        }
    }

    /*ren_kang.hoperun 2012.7.11 change for show the right time of call*/
    class ResetTimer extends Thread{
        @Override
        public void run() {
            while(CallCard.shouldreset==1){
                try {
                        Thread.sleep(1000);
                        Log.i(LOG_TAG, "rk_CallCard.shouldreset == 1"); 
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                resetTime++;
                Log.i(LOG_TAG, "rk_resetTime++="+resetTime);
                obtainMessage(0, resetTime).sendToTarget();
        }
        }
    }
    /*ren_kang.hoperun 2012.7.11 end*/

}
