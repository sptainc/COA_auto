package com.callcenter.ftcjsc.utils;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.preference.PreferenceManager;
import androidx.core.app.ActivityCompat;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils {
    public static String getDeviceGeneration(Context context) {
        try {
            TelephonyManager mTelephonyManager = (TelephonyManager)
                    context.getSystemService(Context.TELEPHONY_SERVICE);
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return null;
            }
            int networkType = mTelephonyManager.getNetworkType();
            return getDeviceGeneration(networkType);
        }catch(Exception e) {
            e.printStackTrace();
            return Constants.getGeneration();
        }
    }

    private static String getDeviceGeneration(int networkType) {
        Log.d("NETWORK_TYPE", "value = " + networkType);
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
            case TelephonyManager.NETWORK_TYPE_GSM:
                return "2G";
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
            case TelephonyManager.NETWORK_TYPE_TD_SCDMA:
                return "3G";
            case TelephonyManager.NETWORK_TYPE_LTE:
            case TelephonyManager.NETWORK_TYPE_IWLAN:
                return "4G";
            case TelephonyManager.NETWORK_TYPE_UNKNOWN:
                return "Unknown";
            default:
                // TelephonyManager.NETWORK_TYPE_NR
                return "5G";
        }
    }

    public static String getTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd 'at' HH:mm:ss");
        return sdf.format(new Date());
    }

    public static void updateConstants(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int iType = preferences.getInt(StorageKeys.device_type.toString(), 1);
        int iDelay = preferences.getInt(StorageKeys.delay_time.toString(), 30000);
        String userInput = preferences.getString(StorageKeys.user_input.toString(), "");

        TelephonyManager mTelephonyManager = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        String imei = Build.VERSION.SDK_INT >= 26 ? mTelephonyManager.getImei() : mTelephonyManager.getDeviceId();
        String gen = getDeviceGeneration(mTelephonyManager.getNetworkType());
        String mcc = mTelephonyManager.getNetworkOperator().substring(0, 3);
        String mnc = mTelephonyManager.getNetworkOperator().substring(3);
        String ids = mTelephonyManager.getSubscriberId();
        String idm = Settings.Secure.getString(context.getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        String lac = null;
        String cid = null;
        String psc = null;

        GsmCellLocation gsmCellLocation = (GsmCellLocation) mTelephonyManager.getCellLocation();
        if (gsmCellLocation != null) {
            lac = gsmCellLocation.getLac() + "";
            cid = gsmCellLocation.getCid() + "";
            psc = gsmCellLocation.getPsc() + "";
        }
        Constants.setValues(imei, gen, iType, iDelay, mcc, mnc, lac, cid, psc, ids, idm, userInput);
    }
}
