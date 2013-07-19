/*
 * Copyright (C) 2008 The Android Open Source Project
 * Copyright (c) 2011-2012, Code Aurora Forum. All rights reserved.
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

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.cdma.TtyIntent;
import com.android.internal.telephony.SubscriptionManager;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.telephony.MSimTelephonyManager;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;

import android.net.sip.SipManager;
import android.preference.PreferenceGroup;
import com.android.phone.sip.SipSharedPreferences;

import com.qrd.plugin.feature_query.FeatureQuery;
import com.android.internal.telephony.CardSubscriptionManager;
/*Begin: Modified by sunrise for CallSet 2012/08/06*/
import android.os.Handler;
import android.os.Message;
import android.os.AsyncResult;
import android.view.View;
import android.widget.EditText;
import android.app.ProgressDialog;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.preference.PreferenceCategory;
import android.telephony.TelephonyManager;
import android.telephony.MSimTelephonyManager;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.MSimPhoneFactory;
import com.android.internal.telephony.SubscriptionData;
import com.android.internal.telephony.Subscription.SubscriptionStatus;
import static com.android.internal.telephony.MSimConstants.SUBSCRIPTION_KEY;
/*End: Modified by sunrise for CallSet 2012/08/06*/

/**
 * Top level "Call settings" UI; see res/xml/call_feature_setting.xml
 *
 * This preference screen is the root of the "Call settings" hierarchy
 * available from the Phone app; the settings here let you control various
 * features related to phone calls (including voicemail settings, SIP
 * settings, the "Respond via SMS" feature, and others.)  It's used only
 * on voice-capable phone devices.
 *
 * Note that this activity is part of the package com.android.phone, even
 * though you reach it from the "Phone" app (i.e. DialtactsActivity) which
 * is from the package com.android.contacts.
 *
 * For the "Mobile network settings" screen under the main Settings app,
 * see apps/Phone/src/com/android/phone/Settings.java.
 */
public class MSimCallFeaturesSetting extends PreferenceActivity
        implements Preference.OnPreferenceChangeListener {

    // debug data
    private static final String LOG_TAG = "MSimCallFeaturesSetting";
    private static final boolean DBG = (PhoneApp.DBG_LEVEL >= 2);

    // String keys for preference lookup
    // TODO: Naming these "BUTTON_*" is confusing since they're not actually buttons(!)
    private static final String BUTTON_DTMF_KEY   = "button_dtmf_settings";
    private static final String BUTTON_RETRY_KEY  = "button_auto_retry_key";
    private static final String BUTTON_PROXIMITY_KEY = "button_proximity_key";    // add for new feature: proximity sensor
    private static final String BUTTON_TTY_KEY    = "button_tty_mode_key";
    private static final String BUTTON_HAC_KEY    = "button_hac_key";
    private static final String BUTTON_SELECT_SUB_KEY = "button_call_independent_serv";
    private static final String BUTTON_XDIVERT_KEY = "button_xdivert";
    /* Begin: Modified by zxiaona for display home local 2012/06/13 */
    private static final String SCREEN_STTINGS_KEY = "settingsScreen";
    /* End: Modified by zxiaona for display home local 2012/06/13 */
  /*Begin: Modified by zxiaona for reversal mute 2012/07/31*/
  private static final String BUTTON_REVERSAL_MUTE_KEY = "button_reversalmute_key";
  /*End: Modified by zxiaona for reversal mute 2012/07/31*/

    private static final String DISPLAY_HOME_LOCATION_KEY   = "display_home_location_key";

    private static final String BUTTON_SIP_CALL_OPTIONS = "sip_call_options_key";
    private static final String BUTTON_SIP_CALL_OPTIONS_WIFI_ONLY = "sip_call_options_wifi_only_key";
    private static final String SIP_SETTINGS_CATEGORY_KEY = "sip_settings_category_key";

    private static final String SPEED_DIAL_SETTINGS_KEY = "speed_dial_settings";
    // preferred TTY mode
    // Phone.TTY_MODE_xxx
    static final int preferredTtyMode = Phone.TTY_MODE_OFF;

    // Dtmf tone types
    static final int DTMF_TONE_TYPE_NORMAL = 0;
    static final int DTMF_TONE_TYPE_LONG   = 1;

    private static final int SUB1 = 0;
    private static final int SUB2 = 1;

    public static final String HAC_KEY = "HACSetting";
    public static final String HAC_VAL_ON = "ON";
    public static final String HAC_VAL_OFF = "OFF";

    protected Phone mPhone;

    private AudioManager mAudioManager;

    private CheckBoxPreference mButtonAutoRetry;
    private CheckBoxPreference mButtonHAC;
    /*Begin: Modified by zxiaona for reversal mute 2012/07/31*/
    private CheckBoxPreference mReversalMute;
    /*End: Modified by zxiaona for reversal mute 2012/07/31*/

    private CheckBoxPreference mButtonProximity;
    private ListPreference mButtonDTMF;
    private ListPreference mButtonTTY;
    private PreferenceScreen mButtonXDivert;
    private XDivertCheckBoxPreference mXDivertCheckbox;
    private CheckBoxPreference mDisplayHomeLocation;
    private Phone mPhoneObj[];
    private int mPhoneType[];
    private int mNumPhones;
    private String mRawNumber[];
    private String mLine1Number[];
    private boolean mIsSubActive[];

    private SubscriptionManager mSubManager;

    private SipManager mSipManager;
    private ListPreference mButtonSipCallOptions;
    private SipSharedPreferences mSipSharedPreferences;

    /* Begin: Modified by sunrise for CallSet 2012/08/06 */
    private static final String       BUTTON_DTMF_TONE_KEY              = "button_dtmf_tone_key";
    private static final String       BUTTON_HOOK_VIBRATE_HINT_KEY      = "button_hook_vibrate_hint_key";
    private static final String       BUTTON_GSM_IS_ENABLE_KEY          = "button_gsm_is_enable_key";
    private static final String       BUTTON_CDMA_IS_ENABLE_KEY         = "button_cdma_is_enable_key";
    private static final String       BUTTON_GSM_IPPREFIX_KEY           = "button_gsm_ipprefix_key";
    private static final String       BUTTON_CDMA_IPPREFIX_KEY          = "button_cdma_ipprefix_key";
    private static final String       BUTTON_GSM_CATEGORY_KEY           = "button_gsm_category_key";
    private static final String       BUTTON_CDMA_CATEGORY_KEY          = "button_cdma_category_key";
    private static final String       BUTTON_CDMA_CF_EXPADN_KEY         = "button_cdma_cf_expand_key";
    private static final String       BUTTON_CDMA_CW_KEY                = "button_cdma_cw_key";
    private static final String       BUTTON_GSM_CF_EXPADN_KEY          = "button_gsm_cf_expand_key";
    private static final String       BUTTON_GSM_MORE_EXPAND_KEY        = "button_gsm_more_expand_key";
    private static final String       DIALPAD_TONE_ENABLE               = "dialpad_tone_enable";
    private static final String       HOOK_VIBRATE_HINT_ENABLE          = "hook_vibrate_hint_enable";
    private static final String       SIM_NAME_CDMA                     = "CDMA";
    private static final String       SIM_NAME_GSM                      = "GSM";

    private CheckBoxPreference        mBtnDialpadToneIsEnable           = null;
    private CheckBoxPreference        mBtnHookVibreateHintIsEnable      = null;
    private PreferenceCategory        mCategoryGsm                      = null;
    private PreferenceCategory        mCategoryCdma                     = null;
    private CheckBoxPreference        mBtnGsmIsEnable                   = null;
    private CheckBoxPreference        mBtnCdmaIsEnable                  = null;
    private PreferenceScreen          mSubscriptionGsmIPPrefix          = null;
    private PreferenceScreen          mSubscriptionCdmaIPPrefix         = null;
    private PreferenceScreen          mScreenCdmaCF                     = null;
    private PreferenceScreen          mScreenCdmaCW                     = null;
    private PreferenceScreen          mScreenGsmCF                      = null;
    private PreferenceScreen          mScreenGsmMoreExpand              = null;

    private int                       mSubscription                     = 0;
    private final int                 CARD1_INDEX                       = 0;
    private final int                 CARD2_INDEX                       = 1;
    private final int                 MAX_SUBSCRIPTIONS                 = 2;
    private static final int          SUBSCRIPTION_INDEX_INVALID        = 99999;

    private static final int          EVENT_SIM_STATE_CHANGED           = 1;
    private static final int          EVENT_SET_SUBSCRIPTION_DONE       = 2;
    private static final int          EVENT_SIM_DEACTIVATE_DONE         = 3;
    private static final int          EVENT_SIM_ACTIVATE_DONE           = 4;
    private boolean                   isCard1Absent                     = true;
    private boolean                   isCard2Absent                     = true;
    private boolean                   isCard1Activated                  = false;
    private boolean                   isCard2Activated                  = false;
    private SubscriptionManager       mSubscriptionManager              = null;
    private ProgressDialog            mProgressDialog                   = null;
    private AlertDialog               mDlg                              = null;

    private IntentFilter mIntentFilter  = new IntentFilter(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
    private final String INTENT_SIM_DISABLED = "com.android.sim.INTENT_SIM_DISABLED";

    private final BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if (TelephonyIntents.ACTION_SIM_STATE_CHANGED.equals(action) ||
                Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action) ||
                action.equals(INTENT_SIM_DISABLED))
            {
                AhongResume();
            }
        }
    };

    private Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            AsyncResult ar;
            switch (msg.what) {
                case EVENT_SIM_STATE_CHANGED:
                case EVENT_SET_SUBSCRIPTION_DONE:
                    break;
                case EVENT_SIM_DEACTIVATE_DONE:
                    System.out.println("receive EVENT_SIM_DEACTIVATE_DONE");
                    mSubscriptionManager.unregisterForSubscriptionDeactivated(mSubscription, this);
                    AhongResume();
                    closeDialog();
                    break;
                case EVENT_SIM_ACTIVATE_DONE:
                    System.out.println("receive EVENT_SIM_ACTIVATE_DONE");
                    mSubscriptionManager.unregisterForSubscriptionActivated(mSubscription, this);
                    AhongResume();
                    closeDialog();
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * @param cardIndex
     * 0 is card1 cdma, 1 is card2 gsm
     */
    private boolean isCardAbsent(int cardIndex)
    {
        /*Begin: Modified by sliangqi for card_absent 2012-8-25*/
        //MSimTelephonyManager telManager = MSimTelephonyManager.getDefault();
        //return telManager.getSimState(cardIndex) == TelephonyManager.SIM_STATE_ABSENT;
        CardSubscriptionManager cardSubMgr = CardSubscriptionManager.getInstance();
        if (cardSubMgr != null && cardSubMgr.getCardSubscriptions(cardIndex) != null) {
            return false;
        }
        return true;
        /*End: Modified by sliangqi for card_absent 2012-8-25*/
    }

    private String getMultiSimName(int subscription)
    {
        return Settings.System.getString(this.getContentResolver(),
                Settings.System.MULTI_SIM_NAME[subscription]);
    }

    private boolean isSubActivated(int cardIndex)
    {
        return mSubscriptionManager.isSubActive(cardIndex);
    }

    private String getMultiPhoneName(int subscription)
    {
        String SimName = MSimPhoneFactory.getPhone(subscription).getPhoneName().toUpperCase();

        if (SimName.contains(SIM_NAME_CDMA))
        {
            return SIM_NAME_CDMA;
        }
        else if (SimName.contains(SIM_NAME_GSM))
        {
            return SIM_NAME_GSM;
        }
        else
        {
            return SIM_NAME_GSM;
        }

    }

    private void setCdmaCategoryStatus(boolean status)
    {
        mBtnCdmaIsEnable.setChecked(status);
        mSubscriptionCdmaIPPrefix.setEnabled(status);
        mScreenCdmaCF.setEnabled(status);
        mScreenCdmaCW.setEnabled(status);
    }

    private void setGsmCategoryStatus(boolean status)
    {
        mBtnGsmIsEnable.setChecked(status);
        mSubscriptionGsmIPPrefix.setEnabled(status);
        mScreenGsmCF.setEnabled(status);
        mScreenGsmMoreExpand.setEnabled(status);
    }

    private void setSIMstatus(final int mSubscriptionId, final boolean enabled)
    {
        displayDialog(enabled, mSubscriptionId);
        SubscriptionData subData = new SubscriptionData(MAX_SUBSCRIPTIONS);
        mSubscription = mSubscriptionId;

        for (int i = 0; i < MAX_SUBSCRIPTIONS; i++)
        {
            subData.subscription[i].copyFrom(mSubscriptionManager
                    .getCurrentSubscription(i));
        }
        if (enabled)
        {
            subData.subscription[mSubscriptionId].slotId = mSubscriptionId;
            subData.subscription[mSubscriptionId].subId = mSubscriptionId;
            mSubscriptionManager
                    .setDefaultAppIndex(subData.subscription[mSubscriptionId]);
            subData.subscription[mSubscriptionId].subStatus = SubscriptionStatus.SUB_ACTIVATE;
            mSubscriptionManager.registerForSubscriptionActivated(
            mSubscriptionId, mHandler, EVENT_SIM_ACTIVATE_DONE, null);
        }
        else
        {
            subData.subscription[mSubscriptionId].slotId = SUBSCRIPTION_INDEX_INVALID;
            subData.subscription[mSubscriptionId].m3gppIndex = SUBSCRIPTION_INDEX_INVALID;
            subData.subscription[mSubscriptionId].m3gpp2Index = SUBSCRIPTION_INDEX_INVALID;
            subData.subscription[mSubscriptionId].subId = mSubscriptionId;
            subData.subscription[mSubscriptionId].subStatus = SubscriptionStatus.SUB_DEACTIVATE;
            mSubscriptionManager.registerForSubscriptionDeactivated(
            mSubscriptionId, mHandler, EVENT_SIM_DEACTIVATE_DONE, null);
        }

        mSubscriptionManager.setSubscription(subData);
    }

    private void closeDialog()
    {
        if (mProgressDialog != null)
        {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }

        if (mDlg != null)
        {
            System.out.println("mDlg dismiss.......");
            mDlg.dismiss();
            mDlg = null;
        }
    }

    private void displayDialog(boolean enabled, int SubscriptionId)
    {
        String title = Settings.System.getString(getContentResolver(),Settings.System.MULTI_SIM_NAME[SubscriptionId]);
        String msg = this.getString(enabled?R.string.call_setting_sim_enableing:R.string.call_setting_sim_closing);
        System.out.println(title + " " + msg);
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setTitle(title);
        mProgressDialog.setMessage(msg);
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
//        mDlg = new AlertDialog.Builder(this)
//        .setTitle(title)
//        .setMessage(msg)
//        .setCancelable(false)
//        .show();
    }

    private void updateCdmaCategoryStatus()
    {
        isCard1Absent = isCardAbsent(CARD1_INDEX);
        isCard1Activated = isSubActivated(CARD1_INDEX);
        //System.out.println("isCard1Absent:" + isCard1Absent + "  isCard1Activated:" + isCard1Activated);
        if (isCard1Absent)
        {
            mBtnCdmaIsEnable.setSummary(R.string.call_setting_sim_disable);
            setCdmaCategoryStatus(false);
            mBtnCdmaIsEnable.setEnabled(false);

        }
        else if (!isCard1Activated)
        {
            mBtnCdmaIsEnable.setSummary(R.string.call_setting_sim_close);
            setCdmaCategoryStatus(false);
            mBtnCdmaIsEnable.setEnabled(true);
        }
        else
        {
            mBtnCdmaIsEnable.setSummary(R.string.call_setting_sim_enable);
            setCdmaCategoryStatus(true);
            mBtnCdmaIsEnable.setEnabled(true);
            if (mSubscriptionCdmaIPPrefix != null)
            {
                if (true)
                { // TODO: "true" need change to feature query.
                    String ip_prefix = Settings.System.getString(
                            getContentResolver(),
                            Settings.System.IPCALL_PREFIX[CARD1_INDEX]);
                    if (TextUtils.isEmpty(ip_prefix))
                    {
                        mSubscriptionCdmaIPPrefix
                                .setSummary(R.string.ipcall_sub_summery);
                    }
                    else
                    {
                        mSubscriptionCdmaIPPrefix.setSummary(ip_prefix);
                    }
                }
                else
                {
                    getPreferenceScreen().removePreference(mSubscriptionCdmaIPPrefix);
                    mSubscriptionCdmaIPPrefix = null;
                }
            }
        }
    }

    private void updateGsmCategoryStatus()
    {
        isCard2Absent = isCardAbsent(CARD2_INDEX);
        isCard2Activated = isSubActivated(CARD2_INDEX);
       // System.out.println("isCard2Absent:" + isCard2Absent + "  isCard2Activated:" + isCard2Activated);
        if (isCard2Absent)
        {
            mBtnGsmIsEnable.setSummary(R.string.call_setting_sim_disable);
            setGsmCategoryStatus(false);
            mBtnGsmIsEnable.setEnabled(false);
        }
        else if (!isCard2Activated)
        {
            mBtnGsmIsEnable.setSummary(R.string.call_setting_sim_close);
            setGsmCategoryStatus(false);
            mBtnGsmIsEnable.setEnabled(true);
        }
        else
        {
            setGsmCategoryStatus(true);
            mBtnGsmIsEnable.setEnabled(true);
            mBtnGsmIsEnable.setSummary(R.string.call_setting_sim_enable);

            if (mSubscriptionGsmIPPrefix != null)
            {
                if (true)
                { // TODO: "true" need change to feature query.
                    String ip_prefix = Settings.System.getString(
                            getContentResolver(),
                            Settings.System.IPCALL_PREFIX[CARD2_INDEX]);
                    if (TextUtils.isEmpty(ip_prefix))
                    {
                        mSubscriptionGsmIPPrefix
                                .setSummary(R.string.ipcall_sub_summery);
                    }
                    else
                    {
                        mSubscriptionGsmIPPrefix.setSummary(ip_prefix);
                    }
                }
                else
                {
                    getPreferenceScreen().removePreference(mSubscriptionGsmIPPrefix);
                    mSubscriptionGsmIPPrefix = null;
                }
            }
        }
    }

    private void initWidget()
    {
        //get widget tag
        mBtnDialpadToneIsEnable = (CheckBoxPreference) findPreference(BUTTON_DTMF_TONE_KEY);
        mBtnHookVibreateHintIsEnable = (CheckBoxPreference) findPreference(BUTTON_HOOK_VIBRATE_HINT_KEY);
        mCategoryGsm = (PreferenceCategory) findPreference(BUTTON_GSM_CATEGORY_KEY);
        mCategoryCdma = (PreferenceCategory) findPreference(BUTTON_CDMA_CATEGORY_KEY);
        mBtnGsmIsEnable = (CheckBoxPreference) findPreference(BUTTON_GSM_IS_ENABLE_KEY);
        mBtnCdmaIsEnable = (CheckBoxPreference) findPreference(BUTTON_CDMA_IS_ENABLE_KEY);
        mSubscriptionGsmIPPrefix = (PreferenceScreen) findPreference(BUTTON_GSM_IPPREFIX_KEY);
        mSubscriptionCdmaIPPrefix = (PreferenceScreen) findPreference(BUTTON_CDMA_IPPREFIX_KEY);
        mScreenCdmaCF = (PreferenceScreen) findPreference(BUTTON_CDMA_CF_EXPADN_KEY);
        mScreenCdmaCW = (PreferenceScreen) findPreference(BUTTON_CDMA_CW_KEY);
        mScreenGsmCF = (PreferenceScreen) findPreference(BUTTON_GSM_CF_EXPADN_KEY);
        mScreenGsmMoreExpand = (PreferenceScreen) findPreference(BUTTON_GSM_MORE_EXPAND_KEY);

        //update checkbox status
        if (mBtnDialpadToneIsEnable != null)
        {
            mBtnDialpadToneIsEnable.setPersistent(false);
            mBtnDialpadToneIsEnable.setOnPreferenceChangeListener(this);
        }
        else
        {
            getPreferenceScreen().removePreference(mBtnDialpadToneIsEnable);
            mBtnDialpadToneIsEnable = null;
        }

        if (mBtnHookVibreateHintIsEnable != null)
        {
            mBtnHookVibreateHintIsEnable.setPersistent(false);
            mBtnHookVibreateHintIsEnable.setOnPreferenceChangeListener(this);
        }
        else
        {
            getPreferenceScreen().removePreference(mBtnHookVibreateHintIsEnable);
            mBtnHookVibreateHintIsEnable = null;
        }
    }

    private void AhongResume()
    {
        /* Begin: Modified by zxiaona for connection_vibrator 2012/08/24 */
        if (mBtnDialpadToneIsEnable != null)
        {
            boolean checked = Settings.System.getInt(getContentResolver(),
                    DIALPAD_TONE_ENABLE, 0) != 0;
            mBtnDialpadToneIsEnable.setChecked(checked);
            android.provider.Settings.System.putInt(getContentResolver(),
                    DIALPAD_TONE_ENABLE, checked ? 1 : 0);
        }
        if (mBtnHookVibreateHintIsEnable != null)
        {
            boolean checked = Settings.System.getInt(getContentResolver(),
                    HOOK_VIBRATE_HINT_ENABLE, 1) == 1;
            mBtnHookVibreateHintIsEnable.setChecked(checked);
            android.provider.Settings.System.putInt(getContentResolver(),
                    HOOK_VIBRATE_HINT_ENABLE, checked ? 1 : 0);
        }
        /* End: Modified by zxiaona for connection_vibrator 2012/08/24 */

        updateCdmaCategoryStatus();
        updateGsmCategoryStatus();
//        closeDialog();
//        registerReceiver(mReceiver, mIntentFilter);
    }

    private void AhongCallSetChanged(Preference preference, Object objValue)
    {
        if (preference == mBtnDialpadToneIsEnable)
        {
            boolean isDialpadTone = Boolean.parseBoolean(objValue.toString());
            log("isDialpadTone: " + (isDialpadTone ? "true" : "false"));
            android.provider.Settings.System.putInt(getContentResolver(),
                    DIALPAD_TONE_ENABLE, isDialpadTone ? 1 : 0);
        }
        else if (preference == mBtnHookVibreateHintIsEnable)
        {
            boolean isHookVibrateHint = Boolean.parseBoolean(objValue
                    .toString());
            log("isHookVibrateHint: " + (isHookVibrateHint ? "true" : "false"));
            android.provider.Settings.System.putInt(getContentResolver(),
                    HOOK_VIBRATE_HINT_ENABLE, isHookVibrateHint ? 1 : 0);
        }
        else
        {
            return;
        }

    }

    private boolean AhongPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference)
    {
        System.out.println("AhongPreferenceTreeClick " + preference.getKey() + " " + preference.toString());
        if (preference == mBtnDialpadToneIsEnable)
        {
            boolean checked = mBtnDialpadToneIsEnable.isChecked();
            Settings.System.putInt(mPhone.getContext().getContentResolver(),
                    android.provider.Settings.System.DTMF_TONE_WHEN_DIALING,
                    checked ? 1 : 0);
            //Settings.System.putInt(mPhone.getContext().getContentResolver(),
            //        DIALPAD_TONE_ENABLE, checked ? 1 : 0);
            return true;
        }
        else if (preference == mBtnHookVibreateHintIsEnable)
        {
            boolean checked = mBtnHookVibreateHintIsEnable.isChecked();
            Settings.System.putInt(mPhone.getContext().getContentResolver(),
                    HOOK_VIBRATE_HINT_ENABLE, checked ? 1 : 0);

            return true;
        }
        else if (preference == mBtnCdmaIsEnable)
        {
            boolean checked = mBtnCdmaIsEnable.isChecked();
            setSIMstatus(CARD1_INDEX, checked);

            return true;
        }
        else if (preference == mBtnGsmIsEnable)
        {
            boolean checked = mBtnGsmIsEnable.isChecked();
            System.out.println("checked:" + checked);
            setSIMstatus(CARD2_INDEX, checked);
            return true;
        }
        else if (preference == mSubscriptionGsmIPPrefix)
        {
            View v = getLayoutInflater().inflate(R.layout.ip_prefix, null);
            final EditText edit = (EditText) v
                    .findViewById(R.id.ip_prefix_dialog_edit);
            String ip_prefix = Settings.System.getString(getContentResolver(),
                    Settings.System.IPCALL_PREFIX[CARD2_INDEX]);
            edit.setText(ip_prefix);

            new AlertDialog.Builder(this)
                    .setTitle(R.string.ipcall_dialog_title)
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .setView(v)
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener()
                            {
                                public void onClick(DialogInterface dialog,
                                        int which)
                                {
                                    String ip_prefix = edit.getText()
                                            .toString();
                                    Settings.System
                                            .putString(
                                                    getContentResolver(),
                                                    Settings.System.IPCALL_PREFIX[CARD2_INDEX],
                                                    ip_prefix);
                                    if (TextUtils.isEmpty(ip_prefix))
                                    {
                                        mSubscriptionGsmIPPrefix
                                                .setSummary(R.string.ipcall_sub_summery);
                                    }
                                    else
                                    {
                                        mSubscriptionGsmIPPrefix
                                                .setSummary(edit.getText());
                                    }
                                    onResume();
                                }
                            }).setNegativeButton(android.R.string.cancel, null)
                    .show();
            return true;
        }
        else if (preference == mSubscriptionCdmaIPPrefix)
        {
            View v = getLayoutInflater().inflate(R.layout.ip_prefix, null);
            final EditText edit = (EditText) v
                    .findViewById(R.id.ip_prefix_dialog_edit);
            String ip_prefix = Settings.System.getString(getContentResolver(),
                    Settings.System.IPCALL_PREFIX[CARD1_INDEX]);
            edit.setText(ip_prefix);

            new AlertDialog.Builder(this)
                    .setTitle(R.string.ipcall_dialog_title)
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .setView(v)
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener()
                            {
                                public void onClick(DialogInterface dialog,
                                        int which)
                                {
                                    String ip_prefix = edit.getText()
                                            .toString();
                                    Settings.System
                                            .putString(
                                                    getContentResolver(),
                                                    Settings.System.IPCALL_PREFIX[CARD1_INDEX],
                                                    ip_prefix);
                                    if (TextUtils.isEmpty(ip_prefix))
                                    {
                                        mSubscriptionCdmaIPPrefix
                                                .setSummary(R.string.ipcall_sub_summery);
                                    }
                                    else
                                    {
                                        mSubscriptionCdmaIPPrefix
                                                .setSummary(edit.getText());
                                    }
                                    onResume();
                                }
                            }).setNegativeButton(android.R.string.cancel, null)
                    .show();
            return true;
        }
        else
        {
            return false;
        }
    }

    @Override
    protected void onPause()
    {
        System.out.println("onPause");
        //closeDialog();
//        unregisterReceiver(mReceiver);
        super.onPause();
    }
    /* End: Modified by sunrise for CallSet 2012/08/06 */

    /*
     * Click Listeners, handle click based on objects attached to UI.
     */

    // Click listener for all toggle events
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mButtonDTMF) {
            return true;
        } else if (preference == mButtonTTY) {
            return true;
        } else if (preference == mButtonAutoRetry) {
            android.provider.Settings.System.putInt(mPhone.getContext().getContentResolver(),
                    android.provider.Settings.System.CALL_AUTO_RETRY,
                    mButtonAutoRetry.isChecked() ? 1 : 0);
            return true;
        } else if (preference == mButtonHAC) {
            int hac = mButtonHAC.isChecked() ? 1 : 0;
            // Update HAC value in Settings database
            Settings.System.putInt(mPhone.getContext().getContentResolver(),
                    Settings.System.HEARING_AID, hac);

            // Update HAC Value in AudioManager
            mAudioManager.setParameter(HAC_KEY, hac != 0 ? HAC_VAL_ON : HAC_VAL_OFF);
            return true;
        } else if (preference == mButtonXDivert) {
             preProcessXDivert();
             processXDivert();
             return true;
        } else if (preference == mButtonProximity) {
            boolean checked = mButtonProximity.isChecked();
            Settings.System.putInt(mPhone.getContext().getContentResolver(),
                    android.provider.Settings.System.PROXIMITY_SENSOR,
                    checked ? 1 : 0);
            mButtonProximity.setSummary(checked ? R.string.proximity_on_summary : R.string.proximity_off_summary);
            return true;
        }
        /*Begin: Modified by zxiaona for reversal mute 2012/07/31*/
        else if (preference == mReversalMute) {
            boolean checked = mReversalMute.isChecked();
            Settings.System.putInt(mPhone.getContext().getContentResolver(),
                    "reversal_mute",
                    checked ? 1 : 0);
            mReversalMute.setSummary(checked ? R.string.reversal_off_summary : R.string.reversal_on_summary);
            return true;
        }
        /*End: Modified by zxiaona for reversal mute 2012/07/31*/

        /* Begin: Modified by sunrise for CallSet 2012/08/06 */
        AhongPreferenceTreeClick(preferenceScreen, preference);
        /* End: Modified by sunrise for CallSet 2012/08/06 */

        return false;
    }

    /**
     * Implemented to support onPreferenceChangeListener to look for preference
     * changes.
     *
     * @param preference is the preference to be changed
     * @param objValue should be the value of the selection, NOT its localized
     * display value.
     */
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mButtonDTMF) {
            int index = mButtonDTMF.findIndexOfValue((String) objValue);
            Settings.System.putInt(mPhone.getContext().getContentResolver(),
                    Settings.System.DTMF_TONE_TYPE_WHEN_DIALING, index);
        } else if (preference == mButtonTTY) {
            handleTTYChange(preference, objValue);
        } else if (preference == mDisplayHomeLocation) {
            handleDisplayHomeLocationChange(preference, objValue);
        } else if (preference == mButtonProximity) {
            boolean checked = mButtonProximity.isChecked();
            Settings.System.putInt(mPhone.getContext().getContentResolver(),
                    android.provider.Settings.System.PROXIMITY_SENSOR, checked ? 1 : 0);
            mButtonProximity.setSummary(checked ? R.string.proximity_on_summary : R.string.proximity_off_summary);
        } else if(preference == mButtonSipCallOptions) {
            handleSipCallOptionsChange(objValue);
        }
        /*Begin: Modified by zxiaona for reversal mute 2012/07/31*/
        else if (preference == mReversalMute) {
            handleReversalMute(preference, objValue);
        }
        /*End: Modified by zxiaona for reversal mute 2012/07/31*/

        /* Begin: Modified by sunrise for CallSet 2012/08/06 */
        AhongCallSetChanged(preference, objValue);
        /* End: Modified by sunrise for CallSet 2012/08/06 */

        // always let the preference setting proceed.
        return true;
    }

    /*
     * Activity class methods
     */

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        if (DBG) log("Creating activity");
        mPhone = MSimPhoneApp.getInstance().getPhone();

        /*Begin: Modified by sunrise for CallSet 2012/08/06*/
        //addPreferencesFromResource(R.xml.msim_call_feature_setting);
        addPreferencesFromResource(R.xml.msim_call_feature_setting_ahong);
        /*End: Modified by sunrise for CallSet 2012/08/06*/

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // get buttons
        PreferenceScreen prefSet = getPreferenceScreen();
        mSubManager = SubscriptionManager.getInstance();

        /*Begin: Modified by sunrise for CallSet 2012/08/06*/
        mSubscription = getIntent().getIntExtra(SUBSCRIPTION_KEY, 0);
        mSubscriptionManager = SubscriptionManager.getInstance();
        mIntentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        mIntentFilter.addAction(INTENT_SIM_DISABLED);
        initWidget();
        /* End: Modified by sunrise for CallSet 2012/08/06 */

        mButtonDTMF = (ListPreference) findPreference(BUTTON_DTMF_KEY);
        mButtonAutoRetry = (CheckBoxPreference) findPreference(BUTTON_RETRY_KEY);
        mButtonProximity = (CheckBoxPreference) findPreference(BUTTON_PROXIMITY_KEY);
        mButtonHAC = (CheckBoxPreference) findPreference(BUTTON_HAC_KEY);
        mButtonTTY = (ListPreference) findPreference(BUTTON_TTY_KEY);
        mButtonXDivert = (PreferenceScreen) findPreference(BUTTON_XDIVERT_KEY);
        /*Begin: Modified by zxiaona for reversal mute 2012/07/31*/
        mReversalMute = (CheckBoxPreference) findPreference(BUTTON_REVERSAL_MUTE_KEY);
        /*End: Modified by zxiaona for reversal mute 2012/07/31*/

        mDisplayHomeLocation = (CheckBoxPreference)findPreference(DISPLAY_HOME_LOCATION_KEY);
        /*Begin: Modified by siliangqi for remove_unused_function 2012-7-7*/
        PreferenceGroup sipSettings = (PreferenceGroup)findPreference(SIP_SETTINGS_CATEGORY_KEY);
        /*End: Modified by siliangqi for remove_unused_function 2012-7-7*/
        /* Begin: Modified by zxiaona for display home local 2012/06/13 */
        ((PreferenceScreen) findPreference(SCREEN_STTINGS_KEY))
                .removePreference(mDisplayHomeLocation);
        /* End: Modified by zxiaona for display home local 2012/06/13 */
        /*Begin: Modified by siliangqi for remove_unused_function 2012-7-7*/
        ((PreferenceScreen) findPreference(SCREEN_STTINGS_KEY))
                .removePreference(mButtonTTY);
        ((PreferenceScreen) findPreference(SCREEN_STTINGS_KEY))
                .removePreference(mButtonDTMF);
        ((PreferenceScreen) findPreference(SCREEN_STTINGS_KEY))
                .removePreference(sipSettings);
        /*End: Modified by siliangqi for remove_unused_function 2012-7-7*/
        if (mDisplayHomeLocation != null ) {
            mDisplayHomeLocation.setPersistent(false);
            mDisplayHomeLocation.setOnPreferenceChangeListener(this);
        }
        if (mButtonDTMF != null) {
            if (getResources().getBoolean(R.bool.dtmf_type_enabled)) {
                mButtonDTMF.setOnPreferenceChangeListener(this);
            } else {
                prefSet.removePreference(mButtonDTMF);
                mButtonDTMF = null;
            }
        }

        if (mButtonAutoRetry != null) {
            if (getResources().getBoolean(R.bool.auto_retry_enabled)) {
                mButtonAutoRetry.setOnPreferenceChangeListener(this);
            } else {
                prefSet.removePreference(mButtonAutoRetry);
                mButtonAutoRetry = null;
            }
        }

        if (mButtonProximity != null) {
            if (true) { // TODO: need change to feature query
                mButtonProximity.setOnPreferenceChangeListener(this);
            } else {
                prefSet.removePreference(mButtonProximity);
                mButtonProximity = null;
            }
        }

        /*Begin: Modified by zxiaona for reversal mute 2012/07/31*/
        if (mReversalMute != null) {
            mReversalMute.setPersistent(false);
            mReversalMute.setOnPreferenceChangeListener(this);
        } else {
            prefSet.removePreference(mReversalMute);
            mReversalMute = null;
        }
        /*End: Modified by zxiaona for reversal mute 2012/07/31*/


        if (mButtonHAC != null) {
            if (getResources().getBoolean(R.bool.hac_enabled)) {

                mButtonHAC.setOnPreferenceChangeListener(this);
            } else {
                prefSet.removePreference(mButtonHAC);
                mButtonHAC = null;
            }
        }

        if (mButtonTTY != null) {
            if (getResources().getBoolean(R.bool.tty_enabled)) {
                mButtonTTY.setOnPreferenceChangeListener(this);
            } else {
                prefSet.removePreference(mButtonTTY);
                mButtonTTY = null;
            }
        }

        PreferenceScreen selectSub = (PreferenceScreen) findPreference(BUTTON_SELECT_SUB_KEY);
        if (selectSub != null) {
            Intent intent = selectSub.getIntent();
            intent.putExtra(SelectSubscription.PACKAGE, "com.android.phone");
            intent.putExtra(SelectSubscription.TARGET_CLASS,
                    "com.android.phone.MSimCallFeaturesSubSetting");
        }

        if (mButtonXDivert != null) {
            if (!getResources().getBoolean(R.bool.xdivert_enabled)) {
                prefSet.removePreference(mButtonXDivert);
                mButtonXDivert = null;
            }
        }

        if (mButtonXDivert != null) {
            mNumPhones = MSimTelephonyManager.getDefault().getPhoneCount();
            if (mNumPhones < 1) {
                prefSet.removePreference(mButtonXDivert);
                mButtonXDivert = null;
            } else {
                mButtonXDivert.setOnPreferenceChangeListener(this);
                preProcessXDivert();
            }
        }

        if (!FeatureQuery.FEATURE_CONTACTS_SPEED_DIAL) {
            PreferenceScreen spdSettings = (PreferenceScreen)findPreference(SPEED_DIAL_SETTINGS_KEY);
            prefSet.removePreference(spdSettings);
        }

       //for internet call settings
       if (!FeatureQuery.FEATURE_PHONE_RESTRICT_VOIP && PhoneUtils.isVoipSupported()) {
            /*Begin: Modified by siliangqi for remove_unused_function 2012-7-7*/
           //initSipSettingsPref();
           /*End: Modified by siliangqi for remove_unused_function 2012-7-7*/
       } else {
           PreferenceGroup sipSettingPref = (PreferenceGroup)findPreference(SIP_SETTINGS_CATEGORY_KEY);
           prefSet.removePreference(sipSettingPref);
       }
    }

    public void preProcessXDivert() {
        mPhoneObj = new Phone[mNumPhones];
        mRawNumber = new String[mNumPhones];
        mLine1Number = new String[mNumPhones];
        mPhoneType = new int[mNumPhones];
        mIsSubActive = new boolean[mNumPhones];
        for (int i = 0; i < mNumPhones; i++) {
            mPhoneObj[i] = MSimPhoneApp.getInstance().getPhone(i);
            mRawNumber[i] = null;
            mRawNumber[i] = mPhoneObj[i].getLine1Number();
            mLine1Number[i] = null;
            if (!TextUtils.isEmpty(mRawNumber[i])) {
                mLine1Number[i] = PhoneNumberUtils.formatNumber(mRawNumber[i]);
            }
            mPhoneType[i] = mPhoneObj[i].getPhoneType();
            mIsSubActive[i] = mSubManager.isSubActive(i);
            Log.d(LOG_TAG,"phonetype = " + mPhoneType[i] + "mIsSubActive = " + mIsSubActive[i]
                    + "mLine1Number = " + mLine1Number[i]);
        }
    }

    public void processXDivert() {
        if ((mIsSubActive[SUB1] == false) || (mIsSubActive[SUB2] == false)) {
            //Is a subscription is deactived/or only one SIM is present,
            //dialog would be displayed stating the same.
            displayAlertDialog(R.string.xdivert_sub_absent);
        } else if (mPhoneType[SUB1] == Phone.PHONE_TYPE_CDMA ||
                mPhoneType[SUB2] == Phone.PHONE_TYPE_CDMA) {
            //X-Divert is not supported for CDMA phone.Hence for C+G / C+C,
            //dialog would be displayed stating the same.
            displayAlertDialog(R.string.xdivert_not_supported);
        } else if ((mLine1Number[SUB1] == null) || (mLine1Number[SUB2] == null)) {
            //SIM records does not have msisdn, hence ask user to enter
            //the phone numbers.
            Intent intent = new Intent();
            intent.setClass(this, XDivertPhoneNumbers.class);
            startActivity(intent);
        } else {
            //SIM records have msisdn.Hence directly process
            //XDivert feature
            processXDivertCheckBox();
        }
    }

    public void displayAlertDialog(int resId) {
        new AlertDialog.Builder(this).setMessage(resId)
            .setTitle(R.string.xdivert_title)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Log.d(LOG_TAG, "X-Divert onClick");
                    }
                })
            .show()
            .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    public void onDismiss(DialogInterface dialog) {
                        Log.d(LOG_TAG, "X-Divert onDismiss");
                    }
            });
    }

    public void processXDivertCheckBox() {
        Log.d(LOG_TAG,"processXDivertCheckBox line1 = " + mLine1Number[SUB1] +
            "line2 = " + mLine1Number[SUB2]);
        Intent intent = new Intent();
        intent.setClass(this, XDivertSetting.class);
        intent.putExtra("Sub1_Line1Number" ,mLine1Number[SUB1]);
        intent.putExtra("Sub2_Line1Number" ,mLine1Number[SUB2]);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mButtonDTMF != null) {
            int dtmf = Settings.System.getInt(getContentResolver(),
                    Settings.System.DTMF_TONE_TYPE_WHEN_DIALING, DTMF_TONE_TYPE_NORMAL);
            mButtonDTMF.setValueIndex(dtmf);
        }

        if (mButtonAutoRetry != null) {
            int autoretry = Settings.System.getInt(getContentResolver(),
                    Settings.System.CALL_AUTO_RETRY, 0);
            mButtonAutoRetry.setChecked(autoretry != 0);
        }

        if (mButtonProximity != null) {
            int proximity = Settings.System.getInt(getContentResolver(), Settings.System.PROXIMITY_SENSOR, 1);
            boolean checked = (proximity == 1);
            mButtonProximity.setChecked(checked);
            mButtonProximity.setSummary(checked ? R.string.proximity_on_summary : R.string.proximity_off_summary);
        }

        if (mButtonHAC != null) {
            int hac = Settings.System.getInt(getContentResolver(), Settings.System.HEARING_AID, 0);
            mButtonHAC.setChecked(hac != 0);
        }

        if (mButtonTTY != null) {
            int settingsTtyMode = Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.PREFERRED_TTY_MODE,
                    Phone.TTY_MODE_OFF);
            mButtonTTY.setValue(Integer.toString(settingsTtyMode));
            updatePreferredTtyModeSummary(settingsTtyMode);
        }

        if (mDisplayHomeLocation != null) {
            updateHomeLocationCheckbox();
        }

        /*Begin: Modified by zxiaona for reversal mute 2012/07/31*/
         if (mReversalMute != null) {
             boolean checked =Settings.System.getInt(getContentResolver(), "reversal_mute",0) != 0;
            mReversalMute.setChecked(checked);
            mReversalMute.setSummary(checked ? R.string.reversal_off_summary : R.string.reversal_on_summary);
        }
        /*End: Modified by zxiaona for reversal mute 2012/07/31*/

         /* Begin: Modified by sunrise for CallSet 2012/08/06 */
         AhongResume();
         /* End: Modified by sunrise for CallSet 2012/08/06 */

    }

    private void updateHomeLocationCheckbox() {
        log("updateHomeLocationCheckbox() should check " + ((Settings.System.getInt(
                getContentResolver(),
                Settings.System.DISPLAY_HOME_LOCATION, 1) != 0 )? "true" : "false"));
        mDisplayHomeLocation.setChecked(Settings.System.getInt(
                getContentResolver(),
                Settings.System.DISPLAY_HOME_LOCATION, 1) != 0);
    }

    private void handleTTYChange(Preference preference, Object objValue) {
        int buttonTtyMode;
        buttonTtyMode = Integer.valueOf((String) objValue).intValue();
        int settingsTtyMode = android.provider.Settings.Secure.getInt(
                getContentResolver(),
                android.provider.Settings.Secure.PREFERRED_TTY_MODE, preferredTtyMode);
        if (DBG) log("handleTTYChange: requesting set TTY mode enable (TTY) to" +
                Integer.toString(buttonTtyMode));

        if (buttonTtyMode != settingsTtyMode) {
            switch(buttonTtyMode) {
            case Phone.TTY_MODE_OFF:
            case Phone.TTY_MODE_FULL:
            case Phone.TTY_MODE_HCO:
            case Phone.TTY_MODE_VCO:
                android.provider.Settings.Secure.putInt(getContentResolver(),
                        android.provider.Settings.Secure.PREFERRED_TTY_MODE, buttonTtyMode);
                break;
            default:
                buttonTtyMode = Phone.TTY_MODE_OFF;
            }

            mButtonTTY.setValue(Integer.toString(buttonTtyMode));
            updatePreferredTtyModeSummary(buttonTtyMode);
            Intent ttyModeChanged = new Intent(TtyIntent.TTY_PREFERRED_MODE_CHANGE_ACTION);
            ttyModeChanged.putExtra(TtyIntent.TTY_PREFFERED_MODE, buttonTtyMode);
            sendBroadcast(ttyModeChanged);
        }
    }

    /*Begin: Modified by zxiaona for reversal mute 2012/08/02*/
    private void handleReversalMute(Preference preference,Object objValue) {
        boolean isMute = Boolean.parseBoolean(objValue.toString());
        log("handleReversalMute() display is mute " + (isMute? "true" : "false"));
        android.provider.Settings.System.putInt(getContentResolver(),
                "reversal_mute", isMute ? 1 : 0);
        mReversalMute.setSummary(isMute ? R.string.reversal_off_summary : R.string.reversal_on_summary);
    }
    /*End: Modified by zxiaona for reversal mute 2012/08/02*/

    private void handleDisplayHomeLocationChange(Preference preference,Object objValue) {
        boolean isEnabled = Boolean.parseBoolean(objValue.toString());
        log("handleDisplayHomeLocationChange() display is enabled " + (isEnabled? "true" : "false"));
        android.provider.Settings.System.putInt(getContentResolver(),
                Settings.System.DISPLAY_HOME_LOCATION, isEnabled ? 1 : 0);

    }

    private void updatePreferredTtyModeSummary(int TtyMode) {
        String [] txts = getResources().getStringArray(R.array.tty_mode_entries);
        switch(TtyMode) {
            case Phone.TTY_MODE_OFF:
            case Phone.TTY_MODE_HCO:
            case Phone.TTY_MODE_VCO:
            case Phone.TTY_MODE_FULL:
                mButtonTTY.setSummary(txts[TtyMode]);
                break;
            default:
                mButtonTTY.setEnabled(false);
                mButtonTTY.setSummary(txts[Phone.TTY_MODE_OFF]);
        }
    }

    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

    private void initSipSettingsPref() {
        mSipManager = SipManager.newInstance(this);
        mSipSharedPreferences = new SipSharedPreferences(this);
        mButtonSipCallOptions = getSipCallOptionPreference();
        mButtonSipCallOptions.setOnPreferenceChangeListener(this);
        mButtonSipCallOptions.setValueIndex(
             mButtonSipCallOptions.findIndexOfValue(
                    mSipSharedPreferences.getSipCallOption()));
        mButtonSipCallOptions.setSummary(mButtonSipCallOptions.getEntry());

    }

    // Gets the call options for SIP depending on whether SIP is allowed only
    // on Wi-Fi only; also make the other options preference invisible.
    private ListPreference getSipCallOptionPreference() {
        ListPreference wifiAnd3G = (ListPreference)findPreference(BUTTON_SIP_CALL_OPTIONS);
        ListPreference wifiOnly = (ListPreference)findPreference(BUTTON_SIP_CALL_OPTIONS_WIFI_ONLY);
        PreferenceGroup sipSettings = (PreferenceGroup)findPreference(SIP_SETTINGS_CATEGORY_KEY);
        if (SipManager.isSipWifiOnly(this)) {
            sipSettings.removePreference(wifiAnd3G);
            return wifiOnly;
        } else {
            sipSettings.removePreference(wifiOnly);
            return wifiAnd3G;
        }
    }

    private void handleSipCallOptionsChange(Object objValue) {
        String option = objValue.toString();
        mSipSharedPreferences.setSipCallOption(option);
        mButtonSipCallOptions.setValueIndex(
                mButtonSipCallOptions.findIndexOfValue(option));
        mButtonSipCallOptions.setSummary(mButtonSipCallOptions.getEntry());
    }

}
