/**
 * Copy Right Ahong
 * @author mzikun
 * 2012-7-11
 *
 */
package com.android.phone;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import android.view.KeyEvent;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;

/**
 * @author mzikun
 *
 */
public class AhongPhoneDisplay extends Activity {
    private static final String TAG = "AhongPhoneDisplay";
    private static Phone mPhone = null;
    private static final String AHONGHARDWAREVER = "0.3";
    private TelephonyManager mTelephonyManager;
    private PhoneStateListener mPhoneStateListener = null;
    private static int mSid = 0;
    private static int mNid = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);

        mPhone = PhoneFactory.getDefaultPhone();

        mTelephonyManager = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
        mPhoneStateListener = new PhoneStateListener(){
            @Override
            public void onCellLocationChanged(CellLocation location) {
                updateLocation(location);
            }
        };

        ahongShowDeviceStatus(AhongPhoneDisplay.this);

    }

    @Override
    protected void onResume() {
        super.onResume();

        updateLocation(mTelephonyManager.getCellLocation());
        mTelephonyManager.listen(mPhoneStateListener,PhoneStateListener.LISTEN_CELL_LOCATION);

    }

    private final void updateLocation(CellLocation location) {
        if (location instanceof GsmCellLocation) {
            GsmCellLocation loc = (GsmCellLocation)location;
            int lac = loc.getLac();
            int cid = loc.getCid();
        } else if (location instanceof CdmaCellLocation) {
            CdmaCellLocation loc = (CdmaCellLocation)location;
            int bid = loc.getBaseStationId();
            int sid = loc.getSystemId();
            int nid = loc.getNetworkId();
            int lat = loc.getBaseStationLatitude();
            int lon = loc.getBaseStationLongitude();
            mSid = sid;
            mNid = nid;
        } else {
        }
    }

    private void ahongShowDeviceStatus(Context context) {
        Log.d(TAG, "ahongShowDeviceStatus()...");
        StringBuilder deviceInfo = new StringBuilder(500);

        String unknow = getResources().getString(R.string.ahong_device_unknow);
        String title = getResources().getString(R.string.ahong_device_title);
        String device_type = getResources().getString(R.string.ahong_device_type);
        String hardware_ver = getResources().getString(R.string.ahong_hardware_ver);
        String sofeware_ver = getResources().getString(R.string.ahong_sofeware_ver);
        String prl_ver = getResources().getString(R.string.ahong_prl_ver);
        String meid = getResources().getString(R.string.ahong_meid);
        String uimid = getResources().getString(R.string.ahong_uimid);
        String sid = getResources().getString(R.string.ahong_sid);
        String nid = getResources().getString(R.string.ahong_nid);

        if(Build.MODEL != null){
            device_type += Build.MODEL;
        }
        else {
            device_type += unknow;
        }

        if(AHONGHARDWAREVER != null){
            hardware_ver += AHONGHARDWAREVER;
        }
        else {
            hardware_ver += unknow;
        }

        if(Build.DISPLAY != null){
            sofeware_ver += Build.DISPLAY;
        }
        else {
            sofeware_ver += unknow;
        }

        if(mPhone.getCdmaPrlVersion() != null){
            prl_ver += mPhone.getCdmaPrlVersion();
        }
        else {
            prl_ver += unknow;
        }

        if(mPhone.getMeid() != null){
            meid += mPhone.getMeid();
        }
        else {
            meid += unknow;
        }

        if(mPhone.getEsn() != null){
            uimid += mPhone.getEsn();
        }
        else {
            uimid += unknow;
        }

        if(mSid != 0){
            sid += mSid;
        }
        else{
            sid += unknow;
        }

        if(mNid != 0){
            nid += mNid;
        }
        else{
            nid += unknow;
        }

        Log.d(TAG, sid + "--" + "nid");

        deviceInfo.append(device_type);
        deviceInfo.append("\n");
        deviceInfo.append(hardware_ver);
        deviceInfo.append("\n");
        deviceInfo.append(sofeware_ver);
        deviceInfo.append("\n");
        deviceInfo.append(prl_ver);
        deviceInfo.append("\n");
        deviceInfo.append(meid);
        deviceInfo.append("\n");
        deviceInfo.append(uimid);
        deviceInfo.append("\n");
        deviceInfo.append(sid);
        deviceInfo.append("\n");
        deviceInfo.append(nid);
        deviceInfo.append("\n");

        AlertDialog dialog = new AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(deviceInfo.toString())
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    AhongPhoneDisplay.this.finish();
                }
            })
            .setCancelable(true)
            .setOnKeyListener(new DialogInterface.OnKeyListener() {
                
                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    Log.d(TAG, "DialogInterface.OnKeyListener1");
                    if ((keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_HOME) && event.getAction() == KeyEvent.ACTION_DOWN)
                    {
                        Log.d(TAG, "DialogInterface.OnKeyListener2");
                        AhongPhoneDisplay.this.finish();
                        Log.d(TAG, "DialogInterface.OnKeyListener3");
                    }
                    return false;
                }
            })
            .create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

}
