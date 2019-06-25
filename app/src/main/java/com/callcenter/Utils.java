package com.callcenter;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.telephony.CellLocation;

public class Utils {
    public static int DELAY_TIME_TO_ANSWER = 1; // seconds
    public static int COUNTDOWN_TIMER = 15; // seconds
    public static int DEVICE_TYPE = 1; // receiver

    public static  final int RQC_MAKE_NEW_CALL = 1;
    public static  final int RQC_LISTEN_INCOMMING_CALL = 2;
    public static  final int RQC_ON_CONFIGURSTIONS_SUCCESS = 2;



    public static String getSimSerialNumber(Context context) {
        TelephonyManager mTelephonyManager = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);

        return mTelephonyManager.getSimSerialNumber();
    }

    public static String getSimPhoneNumber(Context context) {
        TelephonyManager mTelephonyManager = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);

        return mTelephonyManager.getLine1Number();
    }


    public static String getDeviceGeneration(Context context) {
        TelephonyManager mTelephonyManager = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);
        int networkType = mTelephonyManager.getNetworkType();
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
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
                return "3G";
            case TelephonyManager.NETWORK_TYPE_LTE:
                return "4G";
            default:
                return "Unknown";
        }
    }

}
