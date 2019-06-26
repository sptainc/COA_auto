package com.callcenter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import android.os.Handler;

import io.reactivex.annotations.NonNull;
import retrofit2.Call;

public class ApiUtils {
    public static final String BASE_URL = "http://acomcorp.vn/Erl/active/";

    public static APIService getAPIService() {
        return RetrofitClient.getClient(BASE_URL).create(APIService.class);
    }

    public static void sendReceiverReport(Context ctx) {

        String imei = Utils.getSimSerialNumber(ctx);
        String gen = Utils.getDeviceGeneration(ctx);

        GPSTracker gps = new GPSTracker(ctx);
        double latitude = gps.getLatitude();
        double longitude = gps.getLongitude();

        Call<String> stringCall = getAPIService().sendReceiverReport("?imei=" + imei + "&gen=" + gen + "&coa=1&lat=" + latitude + "&long=" + longitude);

        stringCall.enqueue(new CallbackWithRetry<String>(stringCall) {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull retrofit2.Response<String> response) {
                CallbackWithRetry.cancelRetry();
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                Log.d("RETRY",  " count");
                CallbackWithRetry.startRetry();
                super.onFailure(call, t);
            }
        });
    }


    public static void sendRequestNumber (final Context ctx) {
        String imei = Utils.getSimSerialNumber(ctx);
        String gen = Utils.getDeviceGeneration(ctx);

        GPSTracker gps = new GPSTracker(ctx);
        double latitude = gps.getLatitude();
        double longitude = gps.getLongitude();


        Call<String> stringCall = getAPIService().sendReceiverReport("?imei=" + imei + "&gen=" + gen + "&coa=0&lat=" + latitude + "&long=" + longitude + "&t=0");

        stringCall.enqueue(new CallbackWithRetry<String>(stringCall) {

            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull retrofit2.Response<String> response) {
                String[] s = response.toString().replace("$s1:", "").replace("s2:", "").split(";");
                if(s.length < 2) {
                    return;
                }
                String number = s[0];
                String sTime = s[1];
                int time = Integer.parseInt(sTime);

                Intent intent = new Intent(Intent.ACTION_CALL);

                intent.setData(Uri.parse("tel:" + number));
                ctx.startActivity(intent);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        CallHandler.endCall(ctx);
                    }
                }, time);
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                super.onFailure(call, t);
                Log.d("AAAAAA", "send request number failed, retrying ...");
            }
        });
    }


    public static void sendDispatcherReport(final Context ctx, String number, double start, double end) {
        String imei = Utils.getSimSerialNumber(ctx);
        String gen = Utils.getDeviceGeneration(ctx);

        GPSTracker gps = new GPSTracker(ctx);
        double latitude = gps.getLatitude();
        double longitude = gps.getLongitude();

        double time = (end - start) / 1000;

        Call<String> stringCall = getAPIService().sendReceiverReport("?imei=" + imei + "&gen=" + gen + "&coa=0&lat=" + latitude + "&long=" + longitude + "&t=" + time);

        stringCall.enqueue(new CallbackWithRetry<String>(stringCall) {

            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull retrofit2.Response<String> response) {
                //do what you want
                Log.d("AAAAAA", "send caller report success");
                TimerService.getInstance().startTimer();
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                super.onFailure(call, t);
                Log.d("AAAAAA", "send caller report failed, retrying ...");
            }
        });
    }
}
