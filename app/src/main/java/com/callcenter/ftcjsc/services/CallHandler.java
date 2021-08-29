package com.callcenter.ftcjsc.services;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;

import androidx.annotation.RequiresApi;

import java.lang.reflect.Method;
import java.util.Date;


public class CallHandler extends CallManager {
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
            TimerService.getInstance().startRunnable(context);
        }
    }
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

    protected void onOutgoingCallStarted(final Context ctx, String number) {
        Log.d("OutgoingCallStarted", "number = " + number + ", duration = " + TimerService.OUTGOING_CALL_TIME);
        TimerService.addProcess("OutgoingCall: " + number);
        new Handler().postDelayed(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void run() {
                Log.d("OutgoingCallEnded", "duration = " + TimerService.OUTGOING_CALL_TIME);
                CallHandler.endCall(ctx);
            }
        }, TimerService.OUTGOING_CALL_TIME * 1000);
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
}
