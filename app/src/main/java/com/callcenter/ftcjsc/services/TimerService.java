package com.callcenter.ftcjsc.services;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.callcenter.ftcjsc.BuildConfig;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TimerService extends Service {
    private Context mContext = this;
    private boolean fetchingFile = false;
    private final int DELAY = 30000;

    private static List<String> process = new ArrayList<>();
    private static File file;

    private static TimerService instance = null;
    private static final Handler requestHandler = new Handler();
    private static Runnable requestRunnable;
    private String pathName = "";

    public static String generation;
    public static String ids;
    public static String idm;
    public static String userInput;
    public static String idsInput;
    public static String deviceName;
    public static int OUTGOING_CALL_TIME = 300;

    public static TimerService getInstance() {
        if (instance == null) {
            instance = new TimerService();
        }
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startRunnable(null);
        Log.d("VERSION", Build.VERSION.SDK_INT + "");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
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

    public static String getTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd 'at' HH:mm:ss");
        return sdf.format(new Date());
    }

    public static boolean updateConstants(Context context) {
        try {
            TelephonyManager mTelephonyManager = (TelephonyManager)
                    context.getSystemService(Context.TELEPHONY_SERVICE);

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            String userInput = preferences.getString(StorageKeys.user_input.toString(), "");
            String idsInput = preferences.getString(StorageKeys.ids_input.toString(), "");
            String gen = getDeviceGeneration(mTelephonyManager.getNetworkType());
            String ids = mTelephonyManager.getSubscriberId();
            String idm = Settings.Secure.getString(context.getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);

            String manufacturer = Build.MANUFACTURER;
            String deviceName = "";
            String model = Build.MODEL;
            if (model.toLowerCase().startsWith(manufacturer.toLowerCase())) {
                deviceName = capitalize(model);
            } else {
                deviceName = capitalize(manufacturer) + "-" + model;
            }

            TimerService.generation = gen;
            TimerService.ids = ids;
            TimerService.idm = idm;
            TimerService.userInput = userInput;
            TimerService.idsInput = idsInput;
            TimerService.deviceName = deviceName;

            return true;
        }catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void startRunnable(Context context) {
        if (context != null) {
            mContext = context;
        }
        if (requestRunnable != null) {
            requestHandler.removeCallbacks(requestRunnable);
        }
        addProcess("Waiting for new process ...");

        requestRunnable = new Runnable() {
            @Override
            public void run() {
                if (CallManager.IS_IDLE && !fetchingFile) {
                    sendRequest();
                }
                requestHandler.postDelayed(requestRunnable, DELAY);
            }
        };
        requestHandler.postDelayed(requestRunnable, DELAY);
    }

    public static void clearProcess() {
        process.clear();
        EventBus.getDefault().post(new MessageEvent(""));
    }

    public static void addProcess(String proc) {
        process.add("->  " + proc + " (" + TimerService.getTime() + ")\n\n");
        String str = "";
        for (String s : process) {
            str += s;
        }
        EventBus.getDefault().post(new MessageEvent(str));
    }

    private void logResponse(Response<String> response, String prefix) {
        String body = "";
        String res = response.toString();
        try {
            body = response.body();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d("RESPONSE " + prefix, TextUtils.isEmpty(res) ? "[empty response]" : res);
        Log.d("RESPONSE_BODY " + prefix, TextUtils.isEmpty(body) ? "[empty body]" : body);
    }

    private void logResponseBody(Response<ResponseBody> response, String prefix) {
        String body = "";
        String res = response.toString();
        try {
            body = response.body().toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d("RESPONSE " + prefix, TextUtils.isEmpty(res) ? "[empty response]" : res);
        Log.d("RESPONSE_BODY " + prefix, TextUtils.isEmpty(body) ? "[empty body]" : body);
    }

    private void logFailure(Throwable t, String prefix) {
        String mess = t.toString();
        try {
            String s = t.getMessage();
            if (TextUtils.isEmpty(s)) {
                mess = s;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d("THROWABLE " + prefix, TextUtils.isEmpty(mess) ? "[empty message]" : mess);
    }

    private String genCommonUrl(int t) {
        String url = "?IDS=" + TimerService.ids + "&IDM=" + TimerService.idm + "&G=" + TimerService.generation
                + "&D=" + TimerService.deviceName + "&P=" + TimerService.userInput + "&t=" + t +"&v=" + android.os.Build.VERSION.SDK_INT;
        Log.d("URL", url);
        return url;
    }

    public void sendRequest() {
        clearProcess();
        TimerService.addProcess("Sending request");
        String url = genCommonUrl(0);
        Call<String> stringCall = ApiClient.getInstance().sendRequest(url);
        stringCall.enqueue((new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                logResponse(response, "RQ");
                if (response.isSuccessful() && response.body() != null) {
                    String result = response.body();

                    if (!TextUtils.isEmpty(result)) {
                        TimerService.addProcess("Request => " + result);
                        if (result.contains("$ok;")) {
                            TimerService.addProcess("Waiting for incoming call ...");
                            startRunnable(null);
                        } else if (result.contains("$c:") && result.contains(";$t:")) {
                            int phoneStartIndex = result.indexOf(":") + 1;
                            int phoneEndIndex = result.indexOf(";");
                            int durationStartIndex = result.lastIndexOf(":") + 1;
                            int durationEndIndex = result.lastIndexOf(";");
                            String phone = result.substring(phoneStartIndex, phoneEndIndex);
                            String duration = result.substring(durationStartIndex, durationEndIndex);

                            try {
                                int iDuration = Integer.parseInt(duration);
                                if (iDuration > 0 && CallManager.IS_IDLE) {
                                    TimerService.addProcess("Start outgoing call to " + phone + ", duration = " + duration);

                                    Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phone));
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    TimerService.OUTGOING_CALL_TIME = iDuration;
                                    try {
                                        mContext.startActivity(intent);
                                    }catch (Exception e) {
                                        startActivity(intent);
                                        e.printStackTrace();
                                    }
                                } else {
                                    TimerService.addProcess("Cannot start outgoing call to phone because parameters invalid, restart process");
                                    startRunnable(null);
                                }
                            } catch (Exception e) {
                                TimerService.addProcess("There are errors occurred: " + e.getCause() + ", restart process");
                                startRunnable(null);
                            }
                        } else if (result.contains("$f:") && result.contains(".zip;")) {
                            TimerService.addProcess("Start download file");
                            int fileStartIndex = result.indexOf(":") + 1;
                            int fileEndIndex = result.lastIndexOf(";");
                            String filePath = result.substring(fileStartIndex, fileEndIndex);
                            downloadFile(filePath);
                        } else {
                            TimerService.addProcess("Unhandled, restart process");
                        }
                    } else {
                        TimerService.addProcess("Request empty, restart process");
                    }
                } else {
                    TimerService.addProcess("Request error, restart process");
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                logFailure(t, "RQ");
                TimerService.addProcess("Send request failure: " + t.getMessage() + ", restart process");
            }
        }));
    }

    public void sendCallReport(final Context context, final String number, final double duration) {
        mContext = context;
        TimerService.addProcess("Send call report start, number = " + number + ", duration = " + duration + "s");
        String url = genCommonUrl((int) duration);
        Call<String> stringCall = ApiClient.getInstance().sendRequest(url);
        stringCall.enqueue((new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                logResponse(response, "CRP");
                TimerService.addProcess("Send call report success, number = " + number + ", duration = " + duration + "s, restart process");
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                logFailure(t, "CRP");
                TimerService.addProcess("Send call report failure, number = " + number + ", duration = " + duration + "s, restart process");
            }
        }));
    }

    public void sendNetworkReport(final Boolean isDownload, final int status) {
        String url = genCommonUrl(status);
        Call<String> stringCall = ApiClient.getInstance().sendRequest(url);

        stringCall.enqueue((new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                logResponse(response, "NRP");
                TimerService.addProcess("Send network report success");
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                logFailure(t, "NRP");
                TimerService.addProcess("Send network report failure: " + t.getMessage());
            }
        }));
    }

    private void downloadFile(final String filePath) {
        File f;
        try {
            f = new File(mContext.getExternalFilesDir(null) + File.separator + filePath);
        }catch (Exception e) {
            f = new File(getExternalFilesDir(null) + File.separator + filePath);
            e.printStackTrace();
        }
        if(f == null) {
            TimerService.addProcess("Download file error, cannot resolve file path");
            return;
        }

        fetchingFile = true;

        if (f.exists() && f.length() / 1024 > 10) {
            file = f;
            uploadFile();
            return;
        }
        removeAllFiles();
        Call<ResponseBody> call = ApiClient.getInstanceDownload().downloadFile(ApiClient.DOWNLOAD_URL + filePath);

        call.enqueue(new Callback<ResponseBody>() {
            @SuppressLint("StaticFieldLeak")
            @Override
            public void onResponse(Call<ResponseBody> call, final Response<ResponseBody> response) {
                logResponseBody(response, "DL");
                if (response.isSuccessful() && response.body() != null) {
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... voids) {
                            TimerService.addProcess("Download & Saving the file to disk, this can be time consuming with large file ...");
                            boolean writtenToDisk = writeResponseBodyToDisk(response.body(), filePath);
                            if (writtenToDisk) {
                                TimerService.addProcess("Download file success, save file " + (writtenToDisk ? "success" : "failure"));
                                uploadFile();
                            } else {
                                TimerService.addProcess("Download file failure, " + response.toString());
//                                    startRunnable(null);
                                fetchingFile = false;
                            }
                            return null;
                        }
                    }.execute();
                } else {
                    TimerService.addProcess("Download file failure");
//                        startRunnable(null);
                    fetchingFile = false;
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                logFailure(t, "DL");
                fetchingFile = false;
                TimerService.addProcess("Download file failure: " + t.getMessage());
//                    startRunnable(null);
            }
        });
    }

    private void uploadFile() {
        if (file == null) {
            TimerService.addProcess("Uploading file failure, file does not exist, restart process");
            startRunnable(null);
            fetchingFile = false;
            return;
        }
        TimerService.addProcess("Uploading file, this can be time consuming with large file ...");
        RequestBody requestBody = RequestBody.create(MediaType.parse("*/*"), file);
        MultipartBody.Part fileToUpload = MultipartBody.Part.createFormData("file", file.getName(), requestBody);
        RequestBody filename = RequestBody.create(MediaType.parse("text/plain"), file.getName());

        Call<ResponseBody> call = ApiClient.getInstanceUpload().uploadFile(fileToUpload, filename);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, final Response<ResponseBody> response) {
                logResponseBody(response, "UL");
                fetchingFile = false;
                if (response.isSuccessful()) {
                    TimerService.addProcess("Upload file success");
                    int file_size = Integer.parseInt(String.valueOf(file.length() / 1024));
                    sendNetworkReport(false, file_size);
                } else {
                    TimerService.addProcess("Upload file failure ");
                    sendNetworkReport(false, 0);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                logFailure(t, "UL");
                fetchingFile = false;
                TimerService.addProcess("Upload file failure: " + t.getMessage());
                sendNetworkReport(false, 0);
            }
        });
    }

    private boolean removeFile() {
        File file = new File(pathName);
        boolean isSuccess = false;
        if (file.exists()) {
            if (file.delete()) {
                isSuccess = true;
            }
        }
        pathName = "";
        return isSuccess;
    }

    private boolean removeAllFiles() {
        try {
            File file = getExternalFilesDir(null);
            File[] Files = file.listFiles();
            if (Files != null) {
                for (int j = 0; j < Files.length; j++) {
                    Files[j].delete();
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean writeResponseBodyToDisk(ResponseBody body, String fileName) {
        try {
            pathName = getExternalFilesDir(null) + File.separator + fileName;
            // todo change the file location/name according to your needs
            File futureStudioIconFile = new File(pathName);
            InputStream inputStream = null;
            OutputStream outputStream = null;

            try {
                byte[] fileReader = new byte[4096];
                long fileSize = body.contentLength();
                long fileSizeDownloaded = 0;
                int currentPercent = 0;

                inputStream = body.byteStream();
                outputStream = new FileOutputStream(futureStudioIconFile);

                while (true) {
                    int read = inputStream.read(fileReader);
                    if (read == -1) {
                        break;
                    }
                    outputStream.write(fileReader, 0, read);
                    fileSizeDownloaded += read;

                    int percent = (int) ((100 * fileSizeDownloaded) / fileSize);
                    if (fileSizeDownloaded == 0) {
                    } else if (fileSizeDownloaded == fileSize) {
                        addProcess("Downloaded: 100%");
                    } else if (percent - currentPercent > 2) {
                        currentPercent = percent;
                        addProcess("Downloaded: " + percent + "%");
                    }
                }
                outputStream.flush();
                file = futureStudioIconFile;
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                removeFile();
                return false;
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
