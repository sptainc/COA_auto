package com.callcenter;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;



public class TimerService extends Service implements LocationListener {
    private Context mContext;
    // flag for GPS status
    boolean isGPSEnabled = false;

    // flag for network status
    boolean isNetworkEnabled = false;

    // flag for GPS status
    boolean canGetLocation = false;

    Location location; // location

    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1; // 1 minute

    // Declaring a Location Manager
    protected LocationManager locationManager;

    private static TimerService instance = null;
    private static Timer timer;
    private static TimerTask timerTask;


    public TimerService() { };

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
        TelephonyManager mTelephonyManager = (TelephonyManager)
                mContext.getSystemService(Context.TELEPHONY_SERVICE);
        Constants.IMEI = mTelephonyManager.getDeviceId();
        Constants.PHONE_NUMBER = mTelephonyManager.getLine1Number();
        Constants.GENERATION = getDeviceGeneration(mTelephonyManager.getNetworkType());

        getLocation();

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

                if (Constants.DEVICE_TYPE == 0) {
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
        timer.schedule(timerTask, Constants.DELAY_TIME, Constants.DELAY_TIME);
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


    private String getDeviceGeneration(int networkType) {
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return "2G";
            default:
                return "3G";
        }
    }


    public void sendRequestNumber() {
        double latitude = Constants.LAT;
        double longitude = Constants.LONG;
        String imei = Constants.IMEI;
        String gen = Constants.GENERATION;

        String url = "?imei=" + imei + "&gen=" + gen + "&coa=0&lat=" + latitude + "&long=" + longitude + "&t=0";

        Log.v("AAAAAA", "send request number start " + url);

        Call<String> stringCall = ApiUtils.getAPIService().sendReceiverReport(url);

        stringCall.enqueue((new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                Log.v("AAAAAA", "send request number success" + response);

                String result = response.body();

                // if server throws number and call time
                // result is format: $s1:09xxxxxxxx;t1:540;t2:30
                if (!TextUtils.isEmpty(result)) {
                    try {
                        String _trimmed = result.trim().replace("$s1:", "")
                                .replace(";t1:", ";")
                                .replace(";t2:", ";")
                                .replace(";t3:", ";")
                                .replace(";s1:", ";")
                                .replace(";s2:", ";")
                                .replace(";s3:", ";");

                        String arr[] = _trimmed.split(";");

                        Log.v("AAAAAA", "data trimmed: " +  Arrays.toString(arr));

                        if (arr.length >= 2) {
                            String number = arr[0];
                            String time = arr[1];

                            if (arr.length >= 3) {
                                String _delay = arr[2];
                                int delay = TextUtils.isEmpty(_delay) || _delay == null ? 15 : Integer.valueOf(_delay) * 1000;
                                Constants.DELAY_TIME = delay;
                            }


                            final Intent intent = new Intent(Intent.ACTION_CALL);
                            intent.setData(Uri.parse("tel:" + number));
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            mContext.startActivity(intent);

                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    CallHandler.endCall(TimerService.this);
                                }
                            }, Integer.valueOf(time) * 1000);

                            stopTimer();
                        }
                    } catch (Exception e) {
                        Log.d("EEEEEEEE", e.toString());
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
        double latitude = Constants.LAT;
        double longitude = Constants.LONG;
        String imei = Constants.IMEI;
        String gen = Constants.GENERATION;

        double time = (end - start) / 1000;

        String url = "?imei=" + imei + "&gen=" + gen + "&coa=0&lat=" + latitude + "&long=" + longitude + "&t=" + time;

        Log.v("AAAAAA", "send caller report start " + url);

        Call<String> stringCall = ApiUtils.getAPIService().sendReceiverReport(url);

        stringCall.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                Log.v("AAAAAA", "send caller report success " + response);
                startTimer();
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Log.v("AAAAAA", "send caller report failure");
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        sendCallerReport(TimerService.this, start, end);
                    }
                }, Constants.DELAY_TIME / 5);
            }
        });
    }

    public void sendReceiverReport() {
        double latitude = Constants.LAT;
        double longitude = Constants.LONG;
        String imei = Constants.IMEI;
        String gen = Constants.GENERATION;

        String url = "?imei=" + imei + "&gen=" + gen + "&coa=1&lat=" + latitude + "&long=" + longitude;

        Log.v("AAAAAA", "send receiver report start " + url);

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

    public Location getLocation() {
        try {
            locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);

            // getting GPS status
            isGPSEnabled = locationManager
                    .isProviderEnabled(LocationManager.GPS_PROVIDER);

            // getting network status
            isNetworkEnabled = locationManager
                    .isProviderEnabled(LocationManager.NETWORK_PROVIDER);


            if (!isGPSEnabled && !isNetworkEnabled) {
                AlertDialog.Builder alt_bld = new AlertDialog.Builder(getApplicationContext());
                alt_bld.setMessage("Could not get current location");
                AlertDialog alert = alt_bld.create();
                alt_bld.setTitle("Get Location error");
                alert.show();
                // no network provider is enabled
            } else {
                this.canGetLocation = true;
                if (isNetworkEnabled) {
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                    Log.d("Network", "Network");
                    if (locationManager != null) {
                        location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        if (location != null) {
                            Constants.LAT = location.getLatitude();
                            Constants.LONG = location.getLongitude();
                        }
                    }
                }
                // if GPS Enabled get lat/long using GPS Services
                if (isGPSEnabled) {
                    if (location == null) {
                        locationManager.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER,
                                MIN_TIME_BW_UPDATES,
                                MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                        Log.d("GPS Enabled", "GPS Enabled");
                        if (locationManager != null) {
                            location = locationManager
                                    .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            if (location != null) {
                                Constants.LAT = location.getLatitude();
                                Constants.LONG = location.getLongitude();
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
        }
        return location;
    }


    @Override
    public void onLocationChanged(Location location) {
        float bestAccuracy = -1f;
        if (location.getAccuracy() != 0.0f
                && (location.getAccuracy() < bestAccuracy) || bestAccuracy == -1f) {
            locationManager.removeUpdates(this);
        }else {
            Constants.LAT = location.getLatitude();
            Constants.LONG = location.getLongitude();
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }


}
