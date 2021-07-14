package com.callcenter.ftcjsc.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.callcenter.ftcjsc.utils.Constants;
import com.callcenter.ftcjsc.utils.Utils;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TimerService extends Service {
    private Context mContext;
    private final int delay = 30000;

    private static TimerService instance = null;
    private static Handler requestHandler = new Handler();
    private static Handler checkerHandler = new Handler();
    private static Runnable requestRunnable;
    private static Runnable checkerRunnable;
    private double lastRequestTime = 0;

    public static TimerService getInstance() {
        if (instance == null) {
            instance = new TimerService();
        }
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        lastRequestTime = new Date().getTime() + delay;
        mContext = this;
        startRunnable(null);
        addChecker();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // Check if there are any agents blocked this runnable restart process, this process will be ensure that it always running
    private void addChecker() {
        final int duration = 300000;
        checkerRunnable = new Runnable() {
            @Override
            public void run() {
                if (new Date().getTime() - lastRequestTime > duration / 2) {
                    startRunnable(null);
                }
                checkerHandler.postDelayed(checkerRunnable, duration);
            }
        };
        checkerHandler.postDelayed(checkerRunnable, duration);
    }

    public void startRunnable(Context ctx) {
        if (ctx != null) {
            mContext = ctx;
        }
        if (requestRunnable != null) {
            requestHandler.removeCallbacks(requestRunnable);
        }
        requestRunnable = new Runnable() {
            @Override
            public void run() {
                if (CallManager.IS_IDLE) {
                    sendRequest();
                    lastRequestTime = new Date().getTime();
                } else {
                    requestHandler.postDelayed(requestRunnable, delay);
                }
            }
        };
        requestHandler.postDelayed(requestRunnable, delay);
    }

    private void endCall() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(CallManager.IS_IDLE) {
                    return;
                }

                Log.d("CallingOutGoing", "the call has not ended for any other reason, force end call");
                try {
                    TelephonyManager tm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
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
                    startRunnable(null);
                }
            }
        }, delay);
    }

    public void sendRequest() {
        String url = "?IDS=" + Constants.getIds() + "&IDM=" + Constants.getIdm() + "&G=" + Utils.getDeviceGeneration(mContext) + "&D=" + android.os.Build.VERSION.SDK_INT + "&M=" + Constants.getDeviceName() + "&P=" + Constants.getUserInput();

        Call<String> stringCall = ApiClient.getAPIService().sendRequest(url);
        Log.d("SendRequest start", url);

        stringCall.enqueue((new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                String result = response.body();

                if (!TextUtils.isEmpty(result)) {
                    Log.d("SendRequest result", result);
                    if (result.contains("$ok;")) {
                        Log.d("SendRequest success", "start waiting incoming call");
                        startRunnable(null);
                    } else if (result.contains("$c:[") && result.contains("];$t:[];")) {
                        int phoneStartIndex = result.indexOf("[");
                        int phoneEndIndex = result.indexOf("]");
                        int durationStartIndex = result.indexOf("[");
                        int durationEndIndex = result.indexOf("]");

                        String phone = result.substring(phoneStartIndex, phoneEndIndex);
                        String duration = result.substring(durationStartIndex, durationEndIndex);
                        try {
                            int iDuration = Integer.parseInt(duration);
                            Pattern pattern = Pattern.compile("/(84|0)+([0-9]{9,10})\b/");
                            if (iDuration > 0 && pattern.matcher(phone).find() && CallManager.IS_IDLE) {
                                Intent intent = new Intent(Intent.ACTION_CALL);
                                intent.setData(Uri.parse("tel:" + phone));
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                mContext.startActivity(intent);
                                endCall();
                                Log.d("SendRequest success", "start outgoing call to " + phone + ", duration = " + duration);
                            } else {
                                Log.d("SendRequest success", "cannot start outgoing call to " + phone + " because parameters invalid");
                                startRunnable(null);
                            }
                        } catch (Exception e) {
                            Log.d("SendRequest success", "throw to catch block" + e.getMessage());
                            startRunnable(null);
                        }
                    } else if(result.contains("$f:[") && result.contains(".zip];")){
                        Log.d("SendRequest success", "download/upload file");
                        //download and upload file
                        startRunnable(null);
                    } else {
                        startRunnable(null);
                    }
                }else {
                    startRunnable(null);
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Log.d("SendRequest failure", "throwable: " + t.getMessage());
                startRunnable(null);
            }
        }));
    }

    public void sendCallReport(Context context, double duration) {
        mContext = context;
        String url = "?IDS=" + Constants.getIds() + "&IDM=" + Constants.getIdm() + "&G=" + Utils.getDeviceGeneration(mContext) + "&D=" + android.os.Build.VERSION.SDK_INT + "&M=" + Constants.getDeviceName() + "&t=" + duration;
        Call<String> stringCall = ApiClient.getAPIService().sendRequest(url);
        Log.d("SendReport start", url);

        stringCall.enqueue((new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                Log.d("SendReport success", "resend runnable");
                startRunnable(null);
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Log.d("SendReport failure", "throwable: " + t.getMessage());
                startRunnable(null);
            }
        }));
    }
}
