package com.callcenter.ftcjsc.services;

import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.annotation.RequiresApi;

import android.os.Bundle;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;

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

    private void answerRingingCallWithIntent(Context context) {
        try {
            Intent localIntent1 = new Intent(Intent.ACTION_HEADSET_PLUG);
            localIntent1.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            localIntent1.putExtra("state", 1);
            localIntent1.putExtra("microphone", 1);
            localIntent1.putExtra("name", "Headset");
            context.sendOrderedBroadcast(localIntent1, "android.permission.CALL_PRIVILEGED");

            Intent localIntent2 = new Intent(Intent.ACTION_MEDIA_BUTTON);
            KeyEvent localKeyEvent1 = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HEADSETHOOK);
            localIntent2.putExtra(Intent.EXTRA_KEY_EVENT, localKeyEvent1);
            context.sendOrderedBroadcast(localIntent2, "android.permission.CALL_PRIVILEGED");

            Intent localIntent3 = new Intent(Intent.ACTION_MEDIA_BUTTON);
            KeyEvent localKeyEvent2 = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_HEADSETHOOK);
            localIntent3.putExtra(Intent.EXTRA_KEY_EVENT, localKeyEvent2);
            context.sendOrderedBroadcast(localIntent3, "android.permission.CALL_PRIVILEGED");

            Intent localIntent4 = new Intent(Intent.ACTION_HEADSET_PLUG);
            localIntent4.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            localIntent4.putExtra("state", 0);
            localIntent4.putExtra("microphone", 1);
            localIntent4.putExtra("name", "Headset");
            context.sendOrderedBroadcast(localIntent4, "android.permission.CALL_PRIVILEGED");
        } catch (Exception e2) {
            e2.printStackTrace();
        }
    }

    //Derived classes should override these to respond to specific events of interest
    protected void onIncomingCallStarted(final Context ctx, String number)  {
        Log.d("IncomingCallStarted", "number = " + number);
//        answerRingingCallWithIntent(ctx);


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
        TimerService.getInstance().sendCallReport(ctx, end.getTime() - start.getTime());
    }

    protected void onOutgoingCallStarted(String number) {
        Log.d("OutgoingCallStarted", "number = " + number);
    }

    protected void onOutgoingCallEnded(Context ctx, String number, Date start, Date end) {
        Log.d("OutgoingCallEnded", "number = " + number + ", duration = " + (end.getTime() - start.getTime()));
        TimerService.getInstance().sendCallReport(ctx, end.getTime() - start.getTime());
    }

    protected void onMissedCall(String number) {
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
                    onOutgoingCallStarted(savedNumber);
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