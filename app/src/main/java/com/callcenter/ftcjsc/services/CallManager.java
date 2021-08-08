package com.callcenter.ftcjsc.services;

import java.util.Date;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.annotation.RequiresApi;

import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;

import org.greenrobot.eventbus.EventBus;

public class CallManager extends BroadcastReceiver {
    public static boolean IS_IDLE = true;
    private static int lastState = TelephonyManager.CALL_STATE_IDLE;
    private static Date callStartTime;
    private static boolean isIncoming;
    private static String savedNumber;  //because the passed incoming is only valid in ringing

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onReceive(final Context context, Intent intent) {
        Log.v("onReceive", "ACTION = " + intent.getAction());
        String action = intent.getAction();
        Bundle extras = intent.getExtras();
        if(action == null || extras == null) {
            return;
        }

        //We listen to two intents.  The new outgoing call only tells us of an outgoing call.  We use it to get the number.
        if (action.equals("android.intent.action.NEW_OUTGOING_CALL")) {
            savedNumber = extras.getString("android.intent.extra.PHONE_NUMBER");
        } else {
            String stateStr = extras.getString(TelephonyManager.EXTRA_STATE);
            String number = extras.getString(TelephonyManager.EXTRA_INCOMING_NUMBER);

            int state = TelephonyManager.CALL_STATE_IDLE;
            if (stateStr != null && stateStr.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                state = TelephonyManager.CALL_STATE_OFFHOOK;
            } else if (stateStr != null && stateStr.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                state = TelephonyManager.CALL_STATE_RINGING;
            }
            onCallStateChanged(context, state, number);
        }
    }

    //Derived classes should override these to respond to specific events of interest
    protected void onIncomingCallStarted(final Context ctx, String number)  {
        TimerService.addProcess("IncomingCall: " + number);
        Log.d("IncomingCallStarted", "number = " + number);
        Intent buttonUp = new Intent(Intent.ACTION_MEDIA_BUTTON);
        buttonUp.putExtra(Intent.EXTRA_KEY_EVENT,
                new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_HEADSETHOOK));
        try {
            ctx.sendOrderedBroadcast(buttonUp, "android.permission.CALL_PRIVILEGED");
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("AutoAnswerFailure","throw error on auto answer: " + e.getMessage());
            TimerService.getInstance().startRunnable(ctx);
        }
    }

    protected void onIncomingCallEnded(Context ctx, String number, Date start, Date end) {
        Log.d("IncomingCallEnded", "number =" + number +", send call report");
        double duration = end.getTime() - start.getTime();
        int iDuration = (int) (duration / 1000);

        TimerService.addProcess("IncomingCallInfo: phone = " + number + ", duration = " + iDuration);
        TimerService.getInstance().sendCallReport(ctx, number, iDuration);
    }

    protected void onOutgoingCallStarted(Context ctx, String number) {
        EventBus.getDefault().post("str");
        Log.d("OutgoingCallStarted", "number = " + number);
        TimerService.addProcess("OutgoingCall: " + number);
    }

    protected void onOutgoingCallEnded(Context ctx, String number, Date start, Date end) {
        double duration = end.getTime() - start.getTime();
        int iDuration = (int) (duration / 1000);
        Log.d("OutgoingCallEnded", "number = " + number + ", duration = " + iDuration);
        TimerService.addProcess("OutgoingCallInfo: phone = " + number + ", duration = " + iDuration);
        TimerService.getInstance().sendCallReport(ctx, number, iDuration);
    }

    protected void onMissedCall(String number) {
        TimerService.addProcess("MissedCallInfo: phone = " + number);
        Log.d("MissCall", "number = " + number);
    }

    //Deals with actual events
    //Incoming call-  goes from IDLE to RINGING when it rings, to OFFHOOK when it's answered, to IDLE when its hung up
    //Outgoing call-  goes from IDLE to OFFHOOK when it dials out, to IDLE when hung up
    public void onCallStateChanged(final Context context, int state, String number) {
        if (lastState == state) {
            //No change, debounce extras
            return;
        }
        switch (state) {
            case TelephonyManager.CALL_STATE_RINGING:
                isIncoming = true;
                callStartTime = new Date();
                savedNumber = number;
                onIncomingCallStarted(context, number);
                IS_IDLE = false;
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                //Transition of ringing->offhook are pickups of incoming calls.  Nothing done on them
                if (lastState != TelephonyManager.CALL_STATE_RINGING) {
                    isIncoming = false;
                    callStartTime = new Date();
                    onOutgoingCallStarted(context, savedNumber);
                    IS_IDLE = false;
                }
                break;
            case TelephonyManager.CALL_STATE_IDLE:
                IS_IDLE = true;
                //Went to idle-  this is the end of a call.  What type depends on previous state(s)
                if (lastState == TelephonyManager.CALL_STATE_RINGING) {
                    //Ring but no pickup-  a miss
                    onMissedCall(savedNumber);
                } else if (isIncoming) {
                    onIncomingCallEnded(context, savedNumber, callStartTime, new Date());
                } else {
                    onOutgoingCallEnded(context, savedNumber, callStartTime, new Date());
                }
                break;
            default:
                IS_IDLE = true;
        }
        lastState = state;
    }
}