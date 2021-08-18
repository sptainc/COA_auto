package com.callcenter.ftcjsc.utils;

import android.os.Build;

public class Constants {
    public static int OUTGOING_CALL_TIME = 300;
    private static final String caller = "Caller Device (Type = 0)";
    private static final String receiver = "Listener Device (Type = 1)";

    private static String imei;
    private static String generation;
    private static int deviceType;
    private static int delayTime = 30000;
    private static String mcc;
    private static String mnc;
    private static String lac;
    private static String cid;
    private static String psc;
    private static String ids;
    private static String idm;
    private static String userInput;

    private static String genLabel(int iType) {
        return iType == 0 ? Constants.caller : Constants.receiver;
    }

    public static String getUserInput() {
        return userInput;
    }
    public static String getIds() {
        return ids;
    }
    public static void setIds(String _ids) {
        ids = _ids;
    }
    public static String getIdm() {
        return idm;
    }
    public static String getImei() { return imei; }
    public static String getGeneration() { return generation; }
    public static int getDeviceType() { return deviceType; }
    public static int getDelayTime() { return delayTime; }
    public static String getMcc() { return mcc; }
    public static String getMnc() { return mnc; }
    public static String getLac() { return lac; }
    public static String getCid() { return cid; }
    public static String getPsc() { return psc; }
    public static String getLabel(int iType) { return genLabel((iType)); }
    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.toLowerCase().startsWith(manufacturer.toLowerCase())) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + "-" + model;
        }
    }

    public static void setDelayTime(int delayTime) { Constants.delayTime = delayTime; }
    public static void setDeviceType(int deviceType) { Constants.deviceType = deviceType; }
    public static void setUserInput(String userInput) {
        Constants.userInput = userInput;
    }

    public static void setValues(String sImei, String sGen, int type, int delay, String mcc, String mnc, String lac, String cid, String psc, String ids, String idm, String userInput) {
        Constants.imei = sImei;
        Constants.generation = sGen;
//        Constants.deviceType = type;
//        Constants.delayTime = delay;
        Constants.mnc = mnc;
        Constants.mcc = mcc;
        Constants.lac = lac;
        Constants.cid = cid;
        Constants.psc = psc;
        Constants.ids = ids;
        Constants.idm = idm;
        Constants.userInput = userInput;
    }

    private static String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }
};