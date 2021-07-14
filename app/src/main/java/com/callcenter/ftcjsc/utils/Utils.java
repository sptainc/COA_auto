package com.callcenter.ftcjsc.utils;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import androidx.core.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;

public class Utils {
    public static String getDeviceGeneration(Context context) {
        TelephonyManager mTelephonyManager = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }

        int networkType = mTelephonyManager.getNetworkType();
        return getDeviceGeneration(networkType);
    }

    private static String getDeviceGeneration(int networkType) {
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
                return "GPRS";
            case TelephonyManager.NETWORK_TYPE_EDGE:
                return "EDGE";
            case TelephonyManager.NETWORK_TYPE_CDMA:
                return "CDMA";
            case TelephonyManager.NETWORK_TYPE_1xRTT:
                return "1xRTT";
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return "2G";
            default:
                return "3G";
        }
    }

    public static void updateConstants(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int iType = preferences.getInt(StorageKeys.device_type.toString(), 1);
        int iDelay = preferences.getInt(StorageKeys.delay_time.toString(), 30);
        String userInput = preferences.getString(StorageKeys.user_input.toString(), "");

        TelephonyManager mTelephonyManager = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        GsmCellLocation gsmCellLocation = (GsmCellLocation) mTelephonyManager.getCellLocation();

        String imei = mTelephonyManager.getDeviceId();
        String gen = getDeviceGeneration(mTelephonyManager.getNetworkType());
        String mcc = mTelephonyManager.getNetworkOperator().substring(0,3);
        String mnc = mTelephonyManager.getNetworkOperator().substring(3);
        String ids = mTelephonyManager.getSubscriberId();
        String idm = mTelephonyManager.getDeviceId();

        int lac = gsmCellLocation.getLac();
        int cid = gsmCellLocation.getCid();
        int psc = gsmCellLocation.getPsc();

        Constants.setValues(imei, gen, iType, iDelay, mcc, mnc, lac ,cid, psc, ids, idm, userInput);
    };
}
