/*
 * Copyright (c) 2012, Code Aurora Forum. All rights reserved.
Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the following
      disclaimer in the documentation and/or other materials provided
      with the distribution.
    * Neither the name of Code Aurora Forum, Inc. nor the names of its
      contributors may be used to endorse or promote products derived
      from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES;LOSS OF USE, DATA, OR PROFITS;OR
BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

***************ren_kang.hoperun 2012.6.20******************
*add this class for callwaitingoption

*/

package com.android.phone;

import com.android.internal.telephony.CommandsInterface;

import android.database.Cursor;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.PreferenceActivity;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.ArrayList;
import static com.android.internal.telephony.MSimConstants.SUBSCRIPTION_KEY;


public class CdmaCallWaitingOptions extends PreferenceActivity {
    private static final String LOG_TAG = "CdmaCallWaitingOptions";
    private final boolean DBG = true;

    public static final String SUBSCRIPTION = "Subscription";
    public static final String CDMA_SUPP_CALL = "Cdma_Supp";

    private static final String NUM_PROJECTION[] = {Phone.NUMBER};

    private static final int CATEGORY_NORMAL = 1;

    private static final String BUTTON_CW_DEACT = "button_cw_deact_key";
    private static final String BUTTON_CW_KEY   = "button_cw_act_key";
    private PreferenceScreen mButtonCW;
    private PreferenceScreen mCwDeactPref;

    private CdmaCallOptionsSetting mCallOptionSettings;

    private final ArrayList<CdmaCallForwardEditPreference> mPreferences =
            new ArrayList<CdmaCallForwardEditPreference> ();
    private final ArrayList<PreferenceScreen> mDeactPreScreens =
            new ArrayList<PreferenceScreen> ();


    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.callwait_cdma_option);

        PreferenceScreen prefSet = getPreferenceScreen();
        mButtonCW   = (PreferenceScreen) prefSet.findPreference(BUTTON_CW_KEY);
        mCwDeactPref = (PreferenceScreen)prefSet.findPreference(BUTTON_CW_DEACT);
        mDeactPreScreens.add(mButtonCW);
        mDeactPreScreens.add(mCwDeactPref);
        // getting selected subscription

    }

    @Override
    public void onResume() {
        super.onResume();
        mDeactPreScreens.get(0).getIntent().setData(Uri.fromParts("tel", "*74", null));
        mDeactPreScreens.get(0).setSummary("*74");
                 mDeactPreScreens.get(1).getIntent().setData(Uri.fromParts("tel", "*740", null));
                 mDeactPreScreens.get(1).setSummary("*740");

    }
}

/*ren_kang.hoperun 2012.6.20 END*/