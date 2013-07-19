package com.android.phone;

/* Begin: Modified by zxiaona for recording 2012/06/20 */

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class PhoneRecorderServices extends Service {
    private static final String LOG_TAG = "RecorderServices";
    private PhoneRecorder mPhoneRecorder = null;
    private boolean mMount = true;
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_MEDIA_EJECT.equals(intent.getAction()) ||
                    Intent.ACTION_MEDIA_UNMOUNTED.equals(intent.getAction())) {
                mMount = false;
                stopSelf();
            }
        }
    };

    public IBinder onBind(Intent arg0) {
        return null;
    }

    public void onCreate() {
        super.onCreate();
        log("onCreate");
        mPhoneRecorder = PhoneRecorder.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
        intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        intentFilter.addDataScheme("file");
        registerReceiver(mBroadcastReceiver, intentFilter);
    }

    public void onDestroy() {
        super.onDestroy();
        log("onDestroy");
        mPhoneRecorder.stopRecord(mMount);
        mPhoneRecorder = null;
        unregisterReceiver(mBroadcastReceiver);
    }

    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        log("onStart");
        mPhoneRecorder.startRecord();
    }

    public void log(String msg) {
        Log.d(LOG_TAG, msg);
    }
}
/* End: Modified by zxiaona for recording 2012/06/20 */
