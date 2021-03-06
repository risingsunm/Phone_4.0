/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.telephony.PhoneNumberUtils;
import android.text.Editable;
import android.text.Spannable;
import android.text.method.DialerKeyListener;
import android.text.method.MovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.android.internal.telephony.CallManager;
import com.android.internal.telephony.Phone;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;


/**
 * Dialer class that encapsulates the DTMF twelve key behaviour.
 * This model backs up the UI behaviour in DTMFTwelveKeyDialerView.java.
 */
public class DTMFTwelveKeyDialer implements View.OnTouchListener, View.OnKeyListener {
    private static final String LOG_TAG = "DTMFTwelveKeyDialer";
    private static final boolean DBG = (PhoneApp.DBG_LEVEL >= 2);

    // events
    private static final int PHONE_DISCONNECT = 100;
    private static final int DTMF_SEND_CNF = 101;

    private CallManager mCM;
    private ToneGenerator mToneGenerator;
    private Object mToneGeneratorLock = new Object();

    // indicate if we want to enable the local tone playback.
    private boolean mLocalToneEnabled;

    // indicates that we are using automatically shortened DTMF tones
    boolean mShortTone;

    // indicate if the confirmation from TelephonyFW is pending.
    private boolean mDTMFBurstCnfPending = false;

    // Queue to queue the short dtmf characters.
    private Queue<Character> mDTMFQueue = new LinkedList<Character>();

    //  Short Dtmf tone duration
    private static final int DTMF_DURATION_MS = 120;


    /** Hash Map to map a character to a tone*/
    private static final HashMap<Character, Integer> mToneMap =
        new HashMap<Character, Integer>();
    /** Hash Map to map a view id to a character*/
    private static final HashMap<Integer, Character> mDisplayMap =
        new HashMap<Integer, Character>();
    /** Set up the static maps*/
    static {
        // Map the key characters to tones
        mToneMap.put('1', ToneGenerator.TONE_DTMF_1);
        mToneMap.put('2', ToneGenerator.TONE_DTMF_2);
        mToneMap.put('3', ToneGenerator.TONE_DTMF_3);
        mToneMap.put('4', ToneGenerator.TONE_DTMF_4);
        mToneMap.put('5', ToneGenerator.TONE_DTMF_5);
        mToneMap.put('6', ToneGenerator.TONE_DTMF_6);
        mToneMap.put('7', ToneGenerator.TONE_DTMF_7);
        mToneMap.put('8', ToneGenerator.TONE_DTMF_8);
        mToneMap.put('9', ToneGenerator.TONE_DTMF_9);
        mToneMap.put('0', ToneGenerator.TONE_DTMF_0);
        mToneMap.put('#', ToneGenerator.TONE_DTMF_P);
        mToneMap.put('*', ToneGenerator.TONE_DTMF_S);

        // Map the buttons to the display characters
        mDisplayMap.put(R.id.one, '1');
        mDisplayMap.put(R.id.two, '2');
        mDisplayMap.put(R.id.three, '3');
        mDisplayMap.put(R.id.four, '4');
        mDisplayMap.put(R.id.five, '5');
        mDisplayMap.put(R.id.six, '6');
        mDisplayMap.put(R.id.seven, '7');
        mDisplayMap.put(R.id.eight, '8');
        mDisplayMap.put(R.id.nine, '9');
        mDisplayMap.put(R.id.zero, '0');
        mDisplayMap.put(R.id.pound, '#');
        mDisplayMap.put(R.id.star, '*');
    }

    // EditText field used to display the DTMF digits sent so far.
    // Note this is null in some modes (like during the CDMA OTA call,
    // where there's no onscreen "digits" display.)
    private EditText mDialpadDigits;

    // InCallScreen reference.
    private InCallScreen mInCallScreen;

    // The DTMFTwelveKeyDialerView we use to display the dialpad.
    private DTMFTwelveKeyDialerView mDialerView;

    // KeyListener used with the "dialpad digits" EditText widget.
    private DTMFKeyListener mDialerKeyListener;

    /**
     * Our own key listener, specialized for dealing with DTMF codes.
     *   1. Ignore the backspace since it is irrelevant.
     *   2. Allow ONLY valid DTMF characters to generate a tone and be
     *      sent as a DTMF code.
     *   3. All other remaining characters are handled by the superclass.
     *
     * This code is purely here to handle events from the hardware keyboard
     * while the DTMF dialpad is up.
     */
    private class DTMFKeyListener extends DialerKeyListener {

        private DTMFKeyListener() {
            super();
        }

        /**
         * Overriden to return correct DTMF-dialable characters.
         */
        @Override
        protected char[] getAcceptedChars(){
            return DTMF_CHARACTERS;
        }

        /** special key listener ignores backspace. */
        @Override
        public boolean backspace(View view, Editable content, int keyCode,
                KeyEvent event) {
            return false;
        }

        /**
         * Return true if the keyCode is an accepted modifier key for the
         * dialer (ALT or SHIFT).
         */
        private boolean isAcceptableModifierKey(int keyCode) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_ALT_LEFT:
                case KeyEvent.KEYCODE_ALT_RIGHT:
                case KeyEvent.KEYCODE_SHIFT_LEFT:
                case KeyEvent.KEYCODE_SHIFT_RIGHT:
                    return true;
                default:
                    return false;
            }
        }

        /**
         * Overriden so that with each valid button press, we start sending
         * a dtmf code and play a local dtmf tone.
         */
        @Override
        public boolean onKeyDown(View view, Editable content,
                                 int keyCode, KeyEvent event) {
            // if (DBG) log("DTMFKeyListener.onKeyDown, keyCode " + keyCode + ", view " + view);

            // find the character
            char c = (char) lookup(event, content);

            // if not a long press, and parent onKeyDown accepts the input
            if (event.getRepeatCount() == 0 && super.onKeyDown(view, content, keyCode, event)) {

                boolean keyOK = ok(getAcceptedChars(), c);

                // if the character is a valid dtmf code, start playing the tone and send the
                // code.
                if (keyOK) {
                    if (DBG) log("DTMFKeyListener reading '" + c + "' from input.");
                    processDtmf(c);
                } else if (DBG) {
                    log("DTMFKeyListener rejecting '" + c + "' from input.");
                }
                return true;
            }
            return false;
        }

        /**
         * Overriden so that with each valid button up, we stop sending
         * a dtmf code and the dtmf tone.
         */
        @Override
        public boolean onKeyUp(View view, Editable content,
                                 int keyCode, KeyEvent event) {
            // if (DBG) log("DTMFKeyListener.onKeyUp, keyCode " + keyCode + ", view " + view);

            super.onKeyUp(view, content, keyCode, event);

            // find the character
            char c = (char) lookup(event, content);

            boolean keyOK = ok(getAcceptedChars(), c);

            if (keyOK) {
                if (DBG) log("Stopping the tone for '" + c + "'");
                stopTone();
                return true;
            }

            return false;
        }

        /**
         * Handle individual keydown events when we DO NOT have an Editable handy.
         */
        public boolean onKeyDown(KeyEvent event) {
            char c = lookup(event);
            if (DBG) log("DTMFKeyListener.onKeyDown: event '" + c + "'");

            // if not a long press, and parent onKeyDown accepts the input
            if (event.getRepeatCount() == 0 && c != 0) {
                // if the character is a valid dtmf code, start playing the tone and send the
                // code.
                if (ok(getAcceptedChars(), c)) {
                    if (DBG) log("DTMFKeyListener reading '" + c + "' from input.");
                    processDtmf(c);
                    return true;
                } else if (DBG) {
                    log("DTMFKeyListener rejecting '" + c + "' from input.");
                }
            }
            return false;
        }

        /**
         * Handle individual keyup events.
         *
         * @param event is the event we are trying to stop.  If this is null,
         * then we just force-stop the last tone without checking if the event
         * is an acceptable dialer event.
         */
        public boolean onKeyUp(KeyEvent event) {
            if (event == null) {
                //the below piece of code sends stopDTMF event unnecessarily even when a null event
                //is received, hence commenting it.
                /*if (DBG) log("Stopping the last played tone.");
                stopTone();*/
                return true;
            }

            char c = lookup(event);
            if (DBG) log("DTMFKeyListener.onKeyUp: event '" + c + "'");

            // TODO: stopTone does not take in character input, we may want to
            // consider checking for this ourselves.
            if (ok(getAcceptedChars(), c)) {
                if (DBG) log("Stopping the tone for '" + c + "'");
                stopTone();
                return true;
            }

            return false;
        }

        /**
         * Find the Dialer Key mapped to this event.
         *
         * @return The char value of the input event, otherwise
         * 0 if no matching character was found.
         */
        private char lookup(KeyEvent event) {
            // This code is similar to {@link DialerKeyListener#lookup(KeyEvent, Spannable) lookup}
            int meta = event.getMetaState();
            int number = event.getNumber();

            if (!((meta & (KeyEvent.META_ALT_ON | KeyEvent.META_SHIFT_ON)) == 0) || (number == 0)) {
                int match = event.getMatch(getAcceptedChars(), meta);
                number = (match != 0) ? match : number;
            }

            return (char) number;
        }

        /**
         * Check to see if the keyEvent is dialable.
         */
        boolean isKeyEventAcceptable (KeyEvent event) {
            return (ok(getAcceptedChars(), lookup(event)));
        }

        /**
         * Overrides the characters used in {@link DialerKeyListener#CHARACTERS}
         * These are the valid dtmf characters.
         */
        public final char[] DTMF_CHARACTERS = new char[] {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '#', '*'
        };
    }

    /**
     * Our own handler to take care of the messages from the phone state changes
     */
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                // disconnect action
                // make sure to close the dialer on ALL disconnect actions.
                case PHONE_DISCONNECT:
                    if (DBG) log("disconnect message recieved, shutting down.");
                    // unregister since we are closing.
                    mCM.unregisterForDisconnect(this);
                    closeDialer(false);
                    break;
                case DTMF_SEND_CNF:
                    if (DBG) log("dtmf confirmation received from FW.");
                    // handle burst dtmf confirmation
                    handleBurstDtmfConfirmation();
                    break;
            }
        }
    };


    /**
     * DTMFTwelveKeyDialer constructor.
     *
     * @param parent the InCallScreen instance that owns us.
     * @param dialerView the DTMFTwelveKeyDialerView we should use to display the dialpad.
     */
    public DTMFTwelveKeyDialer(InCallScreen parent,
                               DTMFTwelveKeyDialerView dialerView) {
        if (DBG) log("DTMFTwelveKeyDialer constructor... this = " + this);

        mInCallScreen = parent;
        mCM = PhoneApp.getInstance().mCM;

        // The passed-in DTMFTwelveKeyDialerView *should* always be
        // non-null, now that the in-call UI uses only portrait mode.
        if (dialerView == null) {
            Log.e(LOG_TAG, "DTMFTwelveKeyDialer: null dialerView!", new IllegalStateException());
            // ...continue as best we can, although things will
            // be pretty broken without the mDialerView UI elements!
        }
        mDialerView = dialerView;
        if (DBG) log("- Got passed-in mDialerView: " + mDialerView);

        if (mDialerView != null) {
            mDialerView.setDialer(this);

            // In the normal in-call DTMF dialpad, mDialpadDigits is an
            // EditText used to display the digits the user has typed so
            // far.  But some other modes (like the OTA call) have no
            // "digits" display at all, in which case mDialpadDigits will
            // be null.
            mDialpadDigits = (EditText) mDialerView.findViewById(R.id.dtmfDialerField);
            if (mDialpadDigits != null) {
                mDialerKeyListener = new DTMFKeyListener();
                mDialpadDigits.setKeyListener(mDialerKeyListener);

                // remove the long-press context menus that support
                // the edit (copy / paste / select) functions.
                mDialpadDigits.setLongClickable(false);
            }

            // Hook up touch / key listeners for the buttons in the onscreen
            // keypad.
            setupKeypad(mDialerView);
        }
    }

    /**
     * Null out our reference to the InCallScreen activity.
     * This indicates that the InCallScreen activity has been destroyed.
     * At the same time, get rid of listeners since we're not going to
     * be valid anymore.
     */
    /* package */ void clearInCallScreenReference() {
        if (DBG) log("clearInCallScreenReference()...");
        mInCallScreen = null;
        mDialerKeyListener = null;
        mHandler.removeMessages(DTMF_SEND_CNF);
        synchronized (mDTMFQueue) {
            mDTMFBurstCnfPending = false;
            mDTMFQueue.clear();
        }
        closeDialer(false);
    }

    /**
     * Dialer code that runs when the dialer is brought up.
     * This includes layout changes, etc, and just prepares the dialer model for use.
     */
    private void onDialerOpen() {
        if (DBG) log("onDialerOpen()...");

        // Any time the dialer is open, listen for "disconnect" events (so
        // we can close ourself.)
        mCM.registerForDisconnect(mHandler, PHONE_DISCONNECT, null);

        // On some devices the screen timeout is set to a special value
        // while the dialpad is up.
        PhoneApp.getInstance().updateWakeState();

        // Give the InCallScreen a chance to do any necessary UI updates.
        mInCallScreen.onDialerOpen();
    }

    /**
     * Allocates some resources we keep around during a "dialer session".
     *
     * (Currently, a "dialer session" just means any situation where we
     * might need to play local DTMF tones, which means that we need to
     * keep a ToneGenerator instance around.  A ToneGenerator instance
     * keeps an AudioTrack resource busy in AudioFlinger, so we don't want
     * to keep it around forever.)
     *
     * Call {@link stopDialerSession} to release the dialer session
     * resources.
     */
    public void startDialerSession() {
        if (DBG) log("startDialerSession()... this = " + this);

        // see if we need to play local tones.
        if (PhoneApp.getInstance().mContext.getResources().
                getBoolean(R.bool.allow_local_dtmf_tones)) {
            mLocalToneEnabled = Settings.System.getInt(mInCallScreen.getContentResolver(),
                    Settings.System.DTMF_TONE_WHEN_DIALING, 1) == 1;
        } else {
            mLocalToneEnabled = false;
        }
        if (DBG) log("- startDialerSession: mLocalToneEnabled = " + mLocalToneEnabled);

        // create the tone generator
        // if the mToneGenerator creation fails, just continue without it.  It is
        // a local audio signal, and is not as important as the dtmf tone itself.
        if (mLocalToneEnabled) {
            synchronized (mToneGeneratorLock) {
                if (mToneGenerator == null) {
                    try {
                        mToneGenerator = new ToneGenerator(AudioManager.STREAM_DTMF, 80);
                    } catch (RuntimeException e) {
                        if (DBG) log("Exception caught while creating local tone generator: " + e);
                        mToneGenerator = null;
                    }
                }
            }
        }
    }

    /**
     * Dialer code that runs when the dialer is closed.
     * This releases resources acquired when we start the dialer.
     */
    private void onDialerClose() {
        if (DBG) log("onDialerClose()...");

        // reset back to a short delay for the poke lock.
        PhoneApp app = PhoneApp.getInstance();
        app.updateWakeState();

        mCM.unregisterForDisconnect(mHandler);

        // Give the InCallScreen a chance to do any necessary UI updates.
        if (mInCallScreen != null) {
            mInCallScreen.onDialerClose();
        }
    }

    /**
     * Releases resources we keep around during a "dialer session"
     * (see {@link startDialerSession}).
     *
     * It's safe to call this even without a corresponding
     * startDialerSession call.
     */
    public void stopDialerSession() {
        // release the tone generator.
        synchronized (mToneGeneratorLock) {
            if (mToneGenerator != null) {
                mToneGenerator.release();
                mToneGenerator = null;
            }
        }
    }

    /**
     * Called externally (from InCallScreen) to play a DTMF Tone.
     */
    public boolean onDialerKeyDown(KeyEvent event) {
        if (DBG) log("Notifying dtmf key down.");
        return mDialerKeyListener.onKeyDown(event);
    }

    /**
     * Called externally (from InCallScreen) to cancel the last DTMF Tone played.
     */
    public boolean onDialerKeyUp(KeyEvent event) {
        if (DBG) log("Notifying dtmf key up.");
        return mDialerKeyListener.onKeyUp(event);
    }

    /**
     * setup the keys on the dialer activity, using the keymaps.
     */
    private void setupKeypad(DTMFTwelveKeyDialerView dialerView) {
        // for each view id listed in the displaymap
        View button;
        for (int viewId : mDisplayMap.keySet()) {
            // locate the view
            button = dialerView.findViewById(viewId);
            // Setup the listeners for the buttons
            button.setOnTouchListener(this);
            button.setClickable(true);
            button.setOnKeyListener(this);
        }
    }

    /**
     * catch the back and call buttons to return to the in call activity.
     */
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // if (DBG) log("onKeyDown:  keyCode " + keyCode);
        switch (keyCode) {
            // finish for these events
            case KeyEvent.KEYCODE_BACK:
            case KeyEvent.KEYCODE_CALL:
                if (DBG) log("exit requested");
                closeDialer(true);  // do the "closing" animation
                return true;
        }
        return mInCallScreen.onKeyDown(keyCode, event);
    }

    /**
     * catch the back and call buttons to return to the in call activity.
     */
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // if (DBG) log("onKeyUp:  keyCode " + keyCode);
        return mInCallScreen.onKeyUp(keyCode, event);
    }

    /**
     * Implemented for the TouchListener, process the touch events.
     */
    public boolean onTouch(View v, MotionEvent event) {
        int viewId = v.getId();

        // if the button is recognized
        if (mDisplayMap.containsKey(viewId)) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // Append the character mapped to this button, to the display.
                    // start the tone
                    processDtmf(mDisplayMap.get(viewId));
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    // stop the tone on ANY other event, except for MOVE.
                    stopTone();
                    break;
            }
            // do not return true [handled] here, since we want the
            // press / click animation to be handled by the framework.
        }
        return false;
    }

    /**
     * Implements View.OnKeyListener for the DTMF buttons.  Enables dialing with trackball/dpad.
     */
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        // if (DBG) log("onKey:  keyCode " + keyCode + ", view " + v);

        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            int viewId = v.getId();
            if (mDisplayMap.containsKey(viewId)) {
                switch (event.getAction()) {
                case KeyEvent.ACTION_DOWN:
                    if (event.getRepeatCount() == 0) {
                        processDtmf(mDisplayMap.get(viewId));
                    }
                    break;
                case KeyEvent.ACTION_UP:
                    stopTone();
                    break;
                }
                // do not return true [handled] here, since we want the
                // press / click animation to be handled by the framework.
            }
        }
        return false;
    }

    /**
     * @return true if the dialer is currently visible onscreen.
     */
    // TODO: clean up naming inconsistency of "opened" vs. "visible".
    // This should be called isVisible(), and open/closeDialer() should
    // be "show" and "hide".
    public boolean isOpened() {
        // Return whether or not the dialer view is visible.
        // (Note that if we're in the middle of a fade-out animation, that
        // also counts as "not visible" even though mDialerView itself is
        // technically still VISIBLE.)
        return ((mDialerView.getVisibility() == View.VISIBLE)
                && !CallCard.Fade.isFadingOut(mDialerView));
    }

    /**
     * Forces the dialer into the "open" state.
     * Does nothing if the dialer is already open.
     *
     * @param animate if true, open the dialer with an animation.
     */
    public void openDialer(boolean animate) {
        if (DBG) log("openDialer()...");

        if (!isOpened()) {
            // Make the dialer view visible.
            if (animate) {
                CallCard.Fade.show(mDialerView);
            } else {
                mDialerView.setVisibility(View.VISIBLE);
            }
            onDialerOpen();
        }
    }

    /**
     * Forces the dialer into the "closed" state.
     * Does nothing if the dialer is already closed.
     *
     * @param animate if true, close the dialer with an animation.
     */
    public void closeDialer(boolean animate) {
        if (DBG) log("closeDialer()...");

        if (isOpened()) {
            // Hide the dialer view.
            if (animate) {
                CallCard.Fade.hide(mDialerView, View.GONE);
            } else {
                mDialerView.setVisibility(View.GONE);
            }
            onDialerClose();
        }
    }

    /**
     * Processes the specified digit as a DTMF key, by playing the
     * appropriate DTMF tone, and appending the digit to the EditText
     * field that displays the DTMF digits sent so far.
     */
    private final void processDtmf(char c) {
        // if it is a valid key, then update the display and send the dtmf tone.
        if (PhoneNumberUtils.is12Key(c)) {
            if (DBG) log("updating display and sending dtmf tone for '" + c + "'");

            // Append this key to the "digits" widget.
            if (mDialpadDigits != null) {
                // TODO: maybe *don't* manually append this digit if
                // mDialpadDigits is focused and this key came from the HW
                // keyboard, since in that case the EditText field will
                // get the key event directly and automatically appends
                // whetever the user types.
                // (Or, a cleaner fix would be to just make mDialpadDigits
                // *not* handle HW key presses.  That seems to be more
                // complicated than just setting focusable="false" on it,
                // though.)
                mDialpadDigits.getText().append(c);
            }

            // Play the tone if it exists.
            if (mToneMap.containsKey(c)) {
                // begin tone playback.
                startTone(c);
            }
        } else if (DBG) {
            log("ignoring dtmf request for '" + c + "'");
        }

        // Any DTMF keypress counts as explicit "user activity".
        PhoneApp.getInstance().pokeUserActivity();
    }

    /**
     * Clears out the display of "DTMF digits typed so far" that's kept in
     * mDialpadDigits.
     *
     * The InCallScreen is responsible for calling this method any time a
     * new call becomes active (or, more simply, any time a call ends).
     * This is how we make sure that the "history" of DTMF digits you type
     * doesn't persist from one call to the next.
     *
     * TODO: it might be more elegent if the dialpad itself could remember
     * the call that we're associated with, and clear the digits if the
     * "current call" has changed since last time.  (This would require
     * some unique identifier that's different for each call.  We can't
     * just use the foreground Call object, since that's a singleton that
     * lasts the whole life of the phone process.  Instead, maybe look at
     * the Connection object that comes back from getEarliestConnection()?
     * Or getEarliestConnectTime()?)
     *
     * Or to be even fancier, we could keep a mapping of *multiple*
     * "active calls" to DTMF strings.  That way you could have two lines
     * in use and swap calls multiple times, and we'd still remember the
     * digits for each call.  (But that's such an obscure use case that
     * it's probably not worth the extra complexity.)
     */
    public void clearDigits() {
        if (DBG) log("clearDigits()...");

        if (mDialpadDigits != null) {
            mDialpadDigits.setText("");
        }
    }

    /**
     * Plays the local tone based the phone type.
     */
    public void startTone(char c) {

        boolean generateTone = true;

        // Only play the tone if it exists.
        if (!mToneMap.containsKey(c)) {
            return;
        }
        // Read the settings as it may be changed by the user during the call
        Phone phone = mCM.getFgPhone();
        mShortTone = TelephonyCapabilities.useShortDtmfTones(phone, phone.getContext());

        if (DBG) log("startDtmfTone()...");

        if (PhoneApp.getInstance().isHeadsetPlugged()) {
            int TTYmode = Settings.Secure.getInt(
                    PhoneApp.getInstance().mContext.getContentResolver(),
                    Settings.Secure.PREFERRED_TTY_MODE,
                    Phone.TTY_MODE_OFF);

            /*
             * In TTY full and and voice carry over modes, DTMF tone should not
             * be played generateTone is false for FULL/VCO mode
             */
            generateTone = !((TTYmode == Phone.TTY_MODE_FULL)
                    || (TTYmode == Phone.TTY_MODE_VCO));
        }

        if (generateTone) {
            // For Short DTMF we need to play the local tone for fixed duration
            if (mShortTone) {
                sendShortDtmfToNetwork(c);
            } else {
                // Pass as a char to be sent to network
                Log.i(LOG_TAG, "send long dtmf for " + c);
                mCM.startDtmf(c);
            }
            startLocalToneIfNeeded(c);
        } else {
            if (DBG)
                log("Not starting local tone. Phone connected to TTY in FULL or VCO mode");
        }
    }

    /**
     * Plays the local tone based the phone type.
     */
    public void startLocalToneIfNeeded(char c) {
        // if local tone playback is enabled, start it.
        // Only play the tone if it exists.
        if (!mToneMap.containsKey(c)) {
            return;
        }
        if (mLocalToneEnabled) {
            synchronized (mToneGeneratorLock) {
                if (mToneGenerator == null) {
                    if (DBG) log("startDtmfTone: mToneGenerator == null, tone: " + c);
                } else {
                    if (DBG) log("starting local tone " + c);
                    int toneDuration = -1;
                    if (mShortTone) {
                        toneDuration = DTMF_DURATION_MS;
                    }
                    mToneGenerator.startTone(mToneMap.get(c), toneDuration);
                }
            }
        }
    }

    /**
     * Check to see if the keyEvent is dialable.
     */
    boolean isKeyEventAcceptable (KeyEvent event) {
        return (mDialerKeyListener != null && mDialerKeyListener.isKeyEventAcceptable(event));
    }

    /**
     * static logging method
     */
    private static void log(String msg) {
        Log.d(LOG_TAG, msg);
    }

    /**
     * Stops the local tone based on the phone type.
     */
    public void stopTone() {
        if (!mShortTone) {
            if (DBG) log("stopping remote tone.");
            mCM.stopDtmf();
            stopLocalToneIfNeeded();
        }
    }

    /**
     * Stops the local tone based on the phone type.
     */
    public void stopLocalToneIfNeeded() {
        if (!mShortTone) {
            if (DBG) log("stopping remote tone.");
            // if local tone playback is enabled, stop it.
            if (DBG) log("trying to stop local tone...");
            if (mLocalToneEnabled) {
                synchronized (mToneGeneratorLock) {
                    if (mToneGenerator == null) {
                        if (DBG) log("stopLocalTone: mToneGenerator == null");
                    } else {
                        if (DBG) log("stopping local tone.");
                        mToneGenerator.stopTone();
                    }
                }
            }
        }
    }

    /**
     * Sends the dtmf character over the network for short DTMF settings
     * When the characters are entered in quick succession,
     * the characters are queued before sending over the network.
     */
    private void sendShortDtmfToNetwork(char dtmfDigit) {
        synchronized (mDTMFQueue) {
            if (mDTMFBurstCnfPending == true) {
                // Insert the dtmf char to the queue
                mDTMFQueue.add(new Character(dtmfDigit));
            } else {
                String dtmfStr = Character.toString(dtmfDigit);
                mCM.sendBurstDtmf(dtmfStr, 0, 0, mHandler.obtainMessage(DTMF_SEND_CNF));
                // Set flag to indicate wait for Telephony confirmation.
                mDTMFBurstCnfPending = true;
            }
        }
    }

    /**
     * Handles Burst Dtmf Confirmation from the Framework.
     */
    void handleBurstDtmfConfirmation() {
        Character dtmfChar = null;
        synchronized (mDTMFQueue) {
            mDTMFBurstCnfPending = false;
            if (!mDTMFQueue.isEmpty()) {
                dtmfChar = mDTMFQueue.remove();
                Log.i(LOG_TAG, "The dtmf character removed from queue" + dtmfChar);
            }
        }
        if (dtmfChar != null) {
            sendShortDtmfToNetwork(dtmfChar);
        }
    }
}
