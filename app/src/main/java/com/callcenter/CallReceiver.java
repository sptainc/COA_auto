package com.callcenter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.ImageView;

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

public class CallReceiver extends CallManager {

    public void acceptCall(Context context)
    {
        Intent buttonUp = new Intent(Intent.ACTION_MEDIA_BUTTON);
        buttonUp.putExtra(Intent.EXTRA_KEY_EVENT,
                new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_HEADSETHOOK));
        context.sendOrderedBroadcast(buttonUp, "android.permission.CALL_PRIVILEGED");
    }

    @Override
    protected void onIncomingCallStarted(Context ctx, String number, Date start) {
        Integer milliseconds = Utils.DELAY_TIME_TO_ANSWER * 1000;
        requestQueue.cancelAll(new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return true;
            }
        });
        if(Utils.DEVICE_TYPE == 1) {
            try {
                Thread.sleep(milliseconds);
                acceptCall(ctx);
            }catch (Exception e){
                e.printStackTrace();
                acceptCall(ctx);
            }
        }
    }

    @Override
    protected void onOutgoingCallStarted(Context ctx, String number, Date start) {
    }

    @Override
    protected void onIncomingCallEnded(Context ctx, String number, Date start, Date end) {
        if(Utils.DEVICE_TYPE == 1) {
            sendReceiverReport(ctx);
        }
    }

    @Override
    protected void onOutgoingCallEnded(Context ctx, String number, Date start, Date end) {
        if(Utils.DEVICE_TYPE == 0) {
            sendCallerReport(ctx);
        }

    }

    @Override
    protected void onMissedCall(Context ctx, String number, Date start) {
    }

    private void sendReceiverReport(final Context context)  {

        GPSTracker gps = new GPSTracker(context);

        double latitude = gps.getLatitude();
        double longitude = gps.getLongitude();

        String imei = Utils.getSimSerialNumber(context);
        String gen = Utils.getDeviceGeneration(context);

        int coa = Utils.DEVICE_TYPE;

        final String url = "http://acomcorp.vn/Erl/active?imei=" + imei + "&gen="+gen +"&coa="+coa+"&lat="+ latitude +"&long=" + longitude;

        StringRequest strRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("send report success: ", response+"");
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        sendReceiverReport(context);
                    }
                }
        );

        strRequest.setRetryPolicy(new DefaultRetryPolicy(
                15000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        requestQueue.add(strRequest);
    }

    private void sendCallerReport(final Context context)  {
        RequestQueue requestQueue = Volley.newRequestQueue(context);

        GPSTracker gps = new GPSTracker(context);

        double latitude = gps.getLatitude();
        double longitude = gps.getLongitude();

        String imei = Utils.getSimSerialNumber(context);
        String gen = Utils.getDeviceGeneration(context);

        int coa = Utils.DEVICE_TYPE;

        final String url = "http://acomcorp.vn/Erl/active?imei=" + imei + "&gen="+gen +"&coa="+coa+"&lat="+ latitude +"&long=" + longitude;

        StringRequest strRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("send report success: ", response+"");

                    }


                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        sendReceiverReport(context);
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
