package com.callcenter;

import android.os.Handler;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class CallReceiver extends CallManager {
    private long currentTime;

    public void acceptCall(Context context)
    {
        Intent buttonUp = new Intent(Intent.ACTION_MEDIA_BUTTON);
        buttonUp.putExtra(Intent.EXTRA_KEY_EVENT,
                new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_HEADSETHOOK));
        context.sendOrderedBroadcast(buttonUp, "android.permission.CALL_PRIVILEGED");
    }

    @Override
    protected void onIncomingCallStarted(final Context ctx, String number, Date start) {
        Log.d("AAAAAA", "call start");

        int milliseconds = Utils.DELAY_TIME_TO_ANSWER * 1000;

        if(Utils.DEVICE_TYPE == 1) {
            final Runnable r = new Runnable() {
                public void run() {
                    acceptCall(ctx);
                }
            };
            final Handler handler = new Handler();
            handler.postDelayed(r, milliseconds);
        }
    }

    @Override
    protected void onOutgoingCallStarted(Context ctx, String number, Date start) {
    }

    @Override
    protected void onIncomingCallEnded(Context ctx, String number, Date start, Date end) {
        Log.d("AAAAAA", "call end");


        if(Utils.DEVICE_TYPE == 1) {
            sendReceiverReport(ctx, start);
        }
    }

    @Override
    protected void onOutgoingCallEnded(Context ctx, String number, Date start, Date end) {

    }

    @Override
    protected void onMissedCall(Context ctx, String number, Date start) {
    }

    private void sendReceiverReport(final Context context, final Date d)  {
        RequestQueue requestQueue = new RequestQueue();
        GPSTracker gps = new GPSTracker(context);
        double latitude = gps.getLatitude();
        double longitude = gps.getLongitude();

        String imei = Utils.getSimSerialNumber(context);
        String gen = Utils.getDeviceGeneration(context);

        int coa = Utils.DEVICE_TYPE;

        final String url = "http://aacomcorp.vn/Erl/active?imei=" + imei + "&gen="+gen +"&coa="+coa+"&lat="+ latitude +"&long=" + longitude;

        StringRequest strRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("AAAAAA", "report success");
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        final Runnable r = new Runnable() {
                            public void run() {
                                sendReceiverReport(context, d);
                                Log.d("AAAAAA", "report error" + d.getTime());
                            }
                        };
                        final Handler handler = new Handler();
                        handler.postDelayed(r, 3000);
                    }
                }
        );

        strRequest.setRetryPolicy(new DefaultRetryPolicy(
                15000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(strRequest);
    }

}
