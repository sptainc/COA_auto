package com.callcenter.ftcjsc.services;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.callcenter.ftcjsc.utils.Constants;
import com.callcenter.ftcjsc.utils.Utils;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TimerService extends Service {
    private Context mContext = this;
    private static List<String> process = new ArrayList<>();
    private static File file;

    private static TimerService instance = null;
    private static final Handler requestHandler = new Handler();
    private static final Handler checkerHandler = new Handler();
    private static Runnable requestRunnable;
    private static Runnable checkerRunnable;
    private double lastRequestTime = 0;
    private String pathName = "";

    public static TimerService getInstance() {
        if (instance == null) {
            instance = new TimerService();
        }
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        lastRequestTime = new Date().getTime() + Constants.getDelayTime();
        startRunnable(null);
        addChecker();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // Check if there are any agents blocked this runnable restart process, this process will be ensure that it always running
    private void addChecker() {
        // check after 1 day
        final int duration = 86400000;
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
                if (CallManager.IS_IDLE) {
                    sendRequest();
//                    downloadFile("20MB.zip");
                    lastRequestTime = new Date().getTime();
                } else {
                    requestHandler.postDelayed(requestRunnable, Constants.getDelayTime());
                }
            }
        };
        requestHandler.postDelayed(requestRunnable, Constants.getDelayTime());
    }

    public static void clearProcess() {
        Log.d("MessageEvent", "process cleared");
        process.clear();
        EventBus.getDefault().post(new MessageEvent(""));
    }

    public static void addProcess(String proc) {
        process.add("->  " + proc + " (" + Utils.getTime() + ")\n\n");
        String str = "";
        for (String s : process) {
            str += s;
        }
        Log.d("MessageEvent", "send from service: " + str);
        EventBus.getDefault().post(new MessageEvent(str));
    }

    private void endCall(int duration) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (CallManager.IS_IDLE) {
                    return;
                }
                Log.d("CallingOutGoing", "the call has not ended for any other reason, force end call");
                try {
                    TelephonyManager tm = (TelephonyManager) mContext
                            .getSystemService(Context.TELEPHONY_SERVICE);
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
        }, duration);
    }

    private String genCommonUrl() {
        String idm = Constants.getIdm();
        String ids = Constants.getIds();
        String generation = Utils.getDeviceGeneration(mContext);
        String name = Constants.getDeviceName();
        int version = android.os.Build.VERSION.SDK_INT;

        if (idm == null || ids == null) {
            Log.d("ParametersInvalid", "idm = " + idm + ", ids = " + ids);
        }
        return "?IDS=" + ids + "&IDM=" + idm + "&G=" + generation + "&D=" + version + "&M=" + name;
    }

    public void sendRequest() {
        clearProcess();
        TimerService.addProcess("Sending request");

        String commonUrl = genCommonUrl();
        String url = commonUrl + "&P=" + Constants.getUserInput();
        Call<String> stringCall = ApiClient.getInstance().sendRequest(url);
        Log.d("SendRequestStart", url);
        stringCall.enqueue((new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                TimerService.addProcess("Request responded " + response.toString());
                if (response.isSuccessful() && response.body() != null) {
                    String result = response.body();

                    if (!TextUtils.isEmpty(result)) {
                        Log.d("SendRequestResult", result);
                        TimerService.addProcess("Request => " + result);
                        if (result.contains("$ok;")) {
//                            Log.d("SendRequestSuccess", "start waiting for incoming call");
//                            TimerService.addProcess("Waiting for incoming call ...");
//                            startRunnable(null);
                                downloadFile("20MB.zip");
                        } else if (result.contains("$c:") && result.contains(";$t:")) {
                            int phoneStartIndex = result.indexOf(":") + 1;
                            int phoneEndIndex = result.indexOf(";");
                            int durationStartIndex = result.lastIndexOf(":") + 1;
                            int durationEndIndex = result.lastIndexOf(";");
                            String phone = result.substring(phoneStartIndex, phoneEndIndex);
                            String duration = result.substring(durationStartIndex, durationEndIndex);

                            Log.d("PhoneAndDuration", phone + ", " + duration);
                            try {
                                int iDuration = Integer.parseInt(duration);
                                Log.d("ValidityDuration", "duration = " + iDuration + ", duration > 0 ? " + (iDuration > 0));
                                Log.d("ValidityPhone", "phone = " + phone + ", valid ? " + PhoneNumberUtils.isGlobalPhoneNumber(phone));
                                Log.d("ValidityDevice", "device is idle ? " + CallManager.IS_IDLE);

                                if (iDuration > 0 && CallManager.IS_IDLE) {
                                    TimerService.addProcess("Start outgoing call to " + phone + ", duration = duration");

                                    Intent intent = new Intent(Intent.ACTION_CALL);
                                    intent.setData(Uri.parse("tel:" + phone));
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    mContext.startActivity(intent);
                                    endCall(iDuration * 1000);
                                    Log.d("SendRequestSuccess", "start outgoing call to " + phone + ", duration = " + duration);
                                } else {
                                    TimerService.addProcess("Cannot start outgoing call to phone because parameters invalid, restart process");
                                    Log.d("SendRequestSuccess", "cannot start outgoing call to " + phone + " because parameters invalid");
                                    startRunnable(null);
                                }
                            } catch (Exception e) {
                                TimerService.addProcess("There are errors occurred: " + e.getMessage() + ", restart process");
                                Log.d("SendRequestSuccess", "throw to catch block" + e.getMessage());
                                startRunnable(null);
                            }
                        } else if (result.contains("$f:") && result.contains(".zip;")) {
                            TimerService.addProcess("Start download file");
                            Log.d("SendRequestSuccess", "download & upload file");
                            int fileStartIndex = result.indexOf(":") + 1;
                            int fileEndIndex = result.lastIndexOf(";");
                            String filePath = result.substring(fileStartIndex, fileEndIndex);
                            downloadFile(filePath);
                        } else {
                            TimerService.addProcess("Unhandled, restart process");
                            startRunnable(null);
                        }
                    } else {
                        TimerService.addProcess("Request empty, restart process");
                        startRunnable(null);
                    }
                } else {
                    TimerService.addProcess("Request error, restart process");
                    startRunnable(null);
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                TimerService.addProcess("Send request failure: " + t.getMessage() + ", restart process");
                Log.d("SendRequestFailure", "throwable: " + t.getMessage());
                startRunnable(null);
            }
        }));
    }

    public void sendCallReport(final Context context, final String number, final double duration) {
        mContext = context;
        TimerService.addProcess("Send call report start, number = " + number + ", duration = " + duration + "s");
        int iDuration = (int) (duration / 1000);
        String commonUrl = genCommonUrl();
        String url = commonUrl + "&t=" + iDuration;
        Call<String> stringCall = ApiClient.getInstance().sendRequest(url);
        Log.d("SendCallReportStart", url);
        stringCall.enqueue((new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                TimerService.addProcess("Send call report success, number = " + number + ", duration = " + duration + "s, restart process");
                Log.d("SendCallReportSuccess", "restart process");
                startRunnable(null);
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                TimerService.addProcess("Send call report failure, number = " + number + ", duration = " + duration + "s, restart process");
                Log.d("SendCallReportFailure", "throwable: " + t.getMessage());
                startRunnable(null);
            }
        }));
    }

    public void sendNetworkReport(final Boolean isDownload, final int status) {
        final String TAG = isDownload ? "Download" : "Upload";
        TimerService.addProcess("Send network report at process " + TAG);

        String commonUrl = genCommonUrl();
        String url = commonUrl + (isDownload ? "&DL=" : "&UL=") + status;
        Call<String> stringCall = ApiClient.getInstance().sendRequest(url);
        Log.d(TAG + "Start", url);

        stringCall.enqueue((new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                boolean shouldResend = status == 0 || !isDownload;
                TimerService.addProcess("Send network report at process " + TAG + " success, " + (shouldResend ? "restart process" : "continue to save file and upload process"));

                Log.d(TAG + "Success", "restart process? " + shouldResend);
                if (shouldResend) {
                    startRunnable(null);
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                TimerService.addProcess("Send network report at process " + TAG + " failure: " + t.getMessage() + ", restart process");
                Log.d(TAG + "Failure", "throwable: " + t.getMessage());
                startRunnable(null);
            }
        }));
    }

    private void downloadFile(final String filePath) {
        Call<ResponseBody> call = ApiClient.getInstanceDownload().downloadFile(filePath);
        Log.d("DownloadStart", "contacting server");

        call.enqueue(new Callback<ResponseBody>() {
            @SuppressLint("StaticFieldLeak")
            @Override
            public void onResponse(Call<ResponseBody> call, final Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... voids) {
                            TimerService.addProcess("Download & Saving the file to disk, this can be time consuming with large file ...");
                            boolean writtenToDisk = writeResponseBodyToDisk(response.body(), filePath);
                            TimerService.addProcess("Download file success, save file " + (writtenToDisk ? "success" : "failure"));
                            Log.d("DownloadSuccess", "save file " + (writtenToDisk ? "success" : "failure, start process"));
                            sendNetworkReport(true, 1);

                            if (writtenToDisk) {
                                uploadFile();
                            } else {
                                startRunnable(null);
                            }
                            return null;
                        }
                    }.execute();
                } else {
                    TimerService.addProcess("Download file failure");
                    Log.d("DownloadFailure", "server contact failed " + response.toString());
                    sendNetworkReport(true, 0);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                TimerService.addProcess("Download file failure: " + t.getMessage());
                Log.e("DownloadFailure", "throwable: " + t.getMessage());
                sendNetworkReport(true, 0);
            }
        });
    }

    private void uploadFile() {
        if(file == null) {
            TimerService.addProcess("Uploading file failure, file does not exist, restart process");
            startRunnable(null);
            return;
        }
        TimerService.addProcess("Uploading file, this can be time consuming with large file ...");
        RequestBody requestBody = RequestBody.create(MediaType.parse("*/*"), file);
        MultipartBody.Part fileToUpload = MultipartBody.Part.createFormData("file", file.getName(), requestBody);
        RequestBody filename = RequestBody.create(MediaType.parse("text/plain"), file.getName());

        Call<ResponseBody> call = ApiClient.getInstanceUpload().uploadFile(fileToUpload, filename);
        Log.d("Uploading", "contacting server");

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, final Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    TimerService.addProcess("Upload file success");
                    Log.d("UploadFile Success", "server contact successfully");
                    sendNetworkReport(false, 1);
                } else {
                    TimerService.addProcess("Upload file failure");
                    Log.d("UploadFile Failure", "server contact failed " + response.toString());
                    sendNetworkReport(false, 0);
                }
                boolean status = removeFile();
                TimerService.addProcess("Delete downloaded file " + (status ? "success" : "failure"));
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                boolean status = removeFile();
                TimerService.addProcess("Upload file failure: " + t.getMessage() + ", delete downloaded file " + (status ? "success" : "failure"));
                Log.e("UploadFile Failure", "throwable: " + t.getMessage());
                sendNetworkReport(false, 0);
            }
        });
    }

    private boolean removeFile() {
        File file = new File(pathName);
        boolean isSuccess = false;
        if (file.exists()) {
            if (file.delete()) {
                Log.d("RemoveFileSuccess", "path = " + pathName);
                isSuccess = true;
            } else {
                Log.d("RemoveFileFailure", "path = " + pathName);
            }
        } else {
            Log.d("RemoveFileFailure", "path = " + pathName + " does not exist");
        }
        pathName = "";
        return isSuccess;
    }

    private boolean writeResponseBodyToDisk(ResponseBody body, String url) {
        try {
            pathName = getExternalFilesDir(null) + File.separator + new Date().getTime() + "_" + url;
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

                    int percent = (int)((100 * fileSizeDownloaded) / fileSize);
                    if(fileSizeDownloaded == 0) {
//                        addProcess("Downloaded: 0%");
                    }else if(fileSizeDownloaded == fileSize){
                        addProcess("Downloaded: 100%");
                    }else if(percent - currentPercent > 2){
                        currentPercent = percent;
                        addProcess("Downloaded: " + percent + "%");
                    }
                }
                outputStream.flush();
                file = futureStudioIconFile;
                return true;
            } catch (Exception e) {
                Log.d("WroteFileFailure", e.getMessage());
                e.printStackTrace();
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
            Log.d("WroteFileFailure", e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
