package com.android.phone;

import android.content.Context;
import android.content.Intent;
import android.telephony.PhoneNumberUtils;
import android.text.Layout;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.telephony.MSimTelephonyManager;
/* Begin: Modified by zxiaona for call_end_screen 2012/04/01 */

public class CallEnd extends RelativeLayout implements View.OnClickListener {

    /**
     * Reference to the InCallScreen activity that owns us. This may be null if
     * we haven't been initialized yet *or* after the InCallScreen activity has
     * been destroyed.
     */

    private InCallScreen mInCallScreen;

    // Phone app instance
    private PhoneApp mApp;
    private Button mNewContactButton;
    private Button mRefresh;
    private Button mc_callButton;
    private Button mg_callButton;
    private Button mSmsButton;
    private View mCallingInfo;

    private static final String LOG_TAG = "CallEnd";
    private static final boolean DBG = (PhoneApp.DBG_LEVEL >= 2);

    public CallEnd(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (DBG)
            log("CallCard constructor...");
        if (DBG)
            log("- this = " + this);
        if (DBG)
            log("- context " + context + ", attrs " + attrs);

        // Inflate the contents of this CallCard, and add it (to ourself) as a
        // child.
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(
                R.layout.call_end, // resource
                this, // root
                true);
        mApp = PhoneApp.getInstance();
    }

    void setInCallEndInstance(InCallScreen inCallScreen) {
        mInCallScreen = inCallScreen;
    }

    void setInCallScreenInstance(InCallScreen inCallScreen) {
        mInCallScreen = inCallScreen;
    }

    @Override
    public void onClick(View view) {
        // TODO Auto-generated method stub
        int id = view.getId();
        switch (id) {
            case R.id.editNewContact:
            case R.id.refresh:
            case R.id.c_callButton:
            case R.id.g_callButton:
            case R.id.smsButton:
                mInCallScreen.handleOnscreenButtonClick(id);
                break;
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mCallingInfo = findViewById(R.id.callingInformation);
        mNewContactButton = (Button) mCallingInfo.findViewById(R.id.editNewContact);
        mNewContactButton.setOnClickListener(this);
        mRefresh = (Button) mCallingInfo.findViewById(R.id.refresh);
        mRefresh.setOnClickListener(this);
        mc_callButton = (Button) findViewById(R.id.c_callButton);
        mc_callButton.setOnClickListener(this);
        mg_callButton = (Button) findViewById(R.id.g_callButton);
        mg_callButton.setOnClickListener(this);
        mSmsButton = (Button) findViewById(R.id.smsButton);
        mSmsButton.setOnClickListener(this);
    }

    // Debugging / testing code

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

    void updateCallEnd(boolean displayButton) {
        if (displayButton == false) {
            mCallingInfo.setVisibility(View.VISIBLE);
            mCallingInfo.setEnabled(true);
        } else {
            mCallingInfo.setVisibility(View.GONE);
        }
        /* Begin: Modified by zxiaona for bug for C . G call 2012/08/14 */
        String cCardState = MSimTelephonyManager.getTelephonyProperty("gsm.sim.state",0,"");
        String gCardState = MSimTelephonyManager.getTelephonyProperty("gsm.sim.state",1,"");
        if(gCardState.equals("ABSENT")) {
            mg_callButton.setEnabled(false);
        }
        if(cCardState.equals("ABSENT")) {
            mc_callButton.setEnabled(false);
        }
        if(gCardState.equals("ABSENT") && cCardState.equals("ABSENT")) {
            boolean isEmergencyNumber = mInCallScreen.getNumberType();
            if(isEmergencyNumber == true) {
                mg_callButton.setEnabled(false);
                mc_callButton.setEnabled(false);
            } else {
            mg_callButton.setEnabled(true);
            mc_callButton.setEnabled(true);
            }
        }
        /* End: Modified by zxiaona for bug for C . G call 2012/08/14 */
    }
}
/* End: Modified by zxiaona for call_end_screen 2012/04/01 */
