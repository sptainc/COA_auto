package com.callcenter;

import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;

import java.lang.reflect.Method;
import java.util.Date;


public class CallHandler extends CallManager {
    private TimerService timerService = TimerService.getInstance();

    public void acceptCall(Context context) {
        Intent buttonUp = new Intent(Intent.ACTION_MEDIA_BUTTON);
        buttonUp.putExtra(Intent.EXTRA_KEY_EVENT,
                new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_HEADSETHOOK));
        try {
            context.sendOrderedBroadcast(buttonUp, "android.permission.CALL_PRIVILEGED");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void endCall(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        try {
            Class c = Class.forName(tm.getClass().getName());
            Method m = c.getDeclaredMethod("getITelephony");
            m.setAccessible(true);
            Object telephonyService = m.invoke(tm);

            c = Class.forName(telephonyService.getClass().getName());
            m = c.getDeclaredMethod("endCall");
            m.setAccessible(true);
            m.invoke(telephonyService);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onIncomingCallStarted(Context ctx, String number, Date start) {
        Log.d("AAAAAA", "incoming call started");
        if (Utils.DEVICE_TYPE == 1) {
            acceptCall(ctx);
            timerService.stopTimerTask();
        }
    }


    @Override
    protected void onIncomingCallEnded(Context ctx, String number, Date start, Date end) {
        Log.d("AAAAAA", "incoming call ended");

        if (Utils.DEVICE_TYPE == 1) {
            timerService.startTimer();
        }
    }

    @Override
    protected void onOutgoingCallStarted(Context ctx, String number, Date start) {
        Log.d("AAAAAA", "outgoing call started");
        if (Utils.DEVICE_TYPE == 0) {
            timerService.stopTimerTask();
        }
    }

    @Override
    protected void onOutgoingCallEnded(Context ctx, String number, Date start, Date end) {
        Log.d("AAAAAA", "outgoing call ended");

        if (Utils.DEVICE_TYPE == 0) {
            ApiUtils.sendDispatcherReport(ctx, number,  start.getTime(), end.getTime());
        }
    }

    @Override
    protected void onMissedCall(Context ctx, String number, Date start) {
    }


}
