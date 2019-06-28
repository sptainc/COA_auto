package com.callcenter;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TimerService extends Service {
    public static int DEVICE_TYPE = 0;
    public static int DELAY_TIME = 15000; // 15 seconds

    private Context mContext;

    private static TimerService instance = null;

    private static Timer timer;
    private static TimerTask timerTask;


    public TimerService() {
    }

    ;

    public static TimerService getInstance() {
        if (instance == null) {
            instance = new TimerService();
        }
        return instance;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        startTimer();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //TODO do something useful
        return Service.START_NOT_STICKY;
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private TimerTask initializeTimerTask() {
        return new TimerTask() {
            public void run() {
                if (CallManager.IS_OUTGOING_CALL || CallManager.IS_INCOMING_CALL) {
                    return;
                }

                if (DEVICE_TYPE == 0) {
                    sendRequestNumber();
                } else {
                    sendReceiverReport();
                }
            }
        };
    }

    public void startTimer() {
        timer = new Timer();
        timerTask = initializeTimerTask();
        timer.schedule(timerTask, DELAY_TIME, DELAY_TIME);
    }

    public void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public void resetTimer() {
        stopTimer();
        startTimer();
    }


    public void sendRequestNumber() {
        Log.v("AAAAAA", "send request number start");

        HelperService helperService = new HelperService(mContext);

        double latitude = helperService.getLatitude();
        double longitude = helperService.getLongitude();
        String imei = helperService.getSimSerialNumber();
        String gen = helperService.getDeviceGeneration();

        String url = "?imei=" + imei + "&gen=" + gen + "&coa=0&lat=" + latitude + "&long=" + longitude + "&t=0";

        Call<String> stringCall = ApiUtils.getAPIService().sendReceiverReport(url);

        stringCall.enqueue((new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                Log.v("AAAAAA", "send request number success" + response.body());

                String result = response.body();

                // if server throws number and call time
                // result is format: $s1:09xxxxxxxx;t1:540;t2:30
                if (!TextUtils.isEmpty(result)) {
                    try {
                        String _trimmed = result.replace("$s1:", "")
                                .replace(";t1:", ";")
                                .replace(";t2:", ";");

                        String arr[] = _trimmed.split(";");

                        if (arr.length >= 2) {
                            String number = arr[0];
                            String time = arr[1];

                            if (arr.length >= 3) {
                                String _delay = arr[2];
                                int delay = TextUtils.isEmpty(_delay) || _delay == null ? 15 : Integer.valueOf(_delay);
                                DELAY_TIME = delay;
                            }


                            final Intent intent = new Intent(Intent.ACTION_CALL);
                            intent.setData(Uri.parse("tel:" + number));
                            mContext.startActivity(intent);

                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CallHandler.endCall(mContext);
                                }
                            }, Integer.valueOf(time));

                            stopTimer();
                        }
                    } catch (Exception e) {
                    }

                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Log.v("AAAAAA", "send request number failure");
            }
        }));
    }


    public void sendCallerReport(final Context context, final double start, final double end) {
        mContext = context;

        HelperService helperService = new HelperService(mContext);
        double latitude = helperService.getLatitude();
        double longitude = helperService.getLongitude();
        String imei = helperService.getSimSerialNumber();
        String gen = helperService.getDeviceGeneration();

        double time = (end - start) / 1000;

        String url = "?imei=" + imei + "&gen=" + gen + "&coa=0&lat=" + latitude + "&long=" + longitude + "&t=" + time;

        Call<String> stringCall = ApiUtils.getAPIService().sendReceiverReport(url);

        stringCall.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                Log.v("AAAAAA", "send caller report success");
                startTimer();
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Log.v("AAAAAA", "send caller report failure");
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        sendCallerReport(mContext, start, end);
                    }
                }, DELAY_TIME / 5);
            }
        });
    }

    public void sendReceiverReport() {
        HelperService helperService = new HelperService(mContext);
        double latitude = helperService.getLatitude();
        double longitude = helperService.getLongitude();
        String imei = helperService.getSimSerialNumber();
        String gen = helperService.getDeviceGeneration();

        String url = "?imei=" + imei + "&gen=" + gen + "&coa=1&lat=" + latitude + "&long=" + longitude;

        Call<String> stringCall = ApiUtils.getAPIService().sendReceiverReport(url);

        stringCall.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                Log.v("AAAAAA", "send receiver report success");
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Log.v("AAAAAA", "send receiver report failure");
            }
        });
    }
}
