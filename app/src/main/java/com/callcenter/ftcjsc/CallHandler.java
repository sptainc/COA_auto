package com.callcenter.ftcjsc;

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
        try {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
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
    protected void onIncomingCallStarted(final Context ctx, String number, Date start) {
        Log.v("AAAAAA", "incoming call started, deviceType: " + Constants.DEVICE_TYPE);

        timerService.stopInterval();

        if (Constants.DEVICE_TYPE == 1) {
            acceptCall(ctx);
        }
    }


    @Override
    protected void onIncomingCallEnded(Context ctx, String number, final Date start, final Date end) {
        Log.v("AAAAAA", "incoming call ended");
        if (Constants.DEVICE_TYPE == 1) {
            timerService.startInterval();
        }
    }

    @Override
    protected void onOutgoingCallStarted(final Context ctx, String number, Date start) {
        Log.v("AAAAAA", "outgoing call started");
        timerService.stopInterval();
    }

    @Override
    protected void onOutgoingCallEnded(final Context ctx, String number, final Date start, final Date end) {
        Log.v("AAAAAA", "outgoing call ended ");

        if (Constants.DEVICE_TYPE == 0) {
            timerService.sendCallerReport(ctx, start.getTime(), end.getTime());
        }
    }

    @Override
    protected void onMissedCall(Context ctx, String number, Date start) {
        // missed incoming call
        Log.v("AAAAAA", "missed call");
        timerService.startInterval();
    }


}
