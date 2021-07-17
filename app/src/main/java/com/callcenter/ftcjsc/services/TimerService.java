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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Date;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TimerService extends Service {
    private Context mContext;

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
//                    downloadFile("");
                    lastRequestTime = new Date().getTime();
                } else {
                    requestHandler.postDelayed(requestRunnable, Constants.getDelayTime());
                }
            }
        };
        requestHandler.postDelayed(requestRunnable, Constants.getDelayTime());
    }

    private void endCall(int duration) {
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
        }, duration);
    }

    private String genCommonUrl() {
        String idm = Constants.getIdm();
        String ids = Constants.getIds();
        String generation = Utils.getDeviceGeneration(mContext);
        String name = Constants.getDeviceName();
        int version = android.os.Build.VERSION.SDK_INT;

        if(idm == null || ids == null) {
            Log.d("ParametersInvalid", "idm = " + idm + ", ids = " + ids);
        }
        return "?IDS=" + ids + "&IDM=" + idm + "&G=" + generation + "&D=" + version + "&M=" + name;
    }

    public void sendRequest() {
        String commonUrl = genCommonUrl();
        String url = commonUrl + "&P=" + Constants.getUserInput();
        Call<String> stringCall = ApiClient.getAPIService(false).sendRequest(url);
        Log.d("SendRequestStart", url);

        stringCall.enqueue((new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if(response.isSuccessful() && response.body() != null) {
                    String result = response.body();

                    if (!TextUtils.isEmpty(result)) {
                        Log.d("SendRequestResult", result);
                        if (result.contains("$ok;")) {
                            Log.d("SendRequestSuccess", "start waiting incoming call");
                            startRunnable(null);
                        } else if (result.contains("$c:") && result.contains(";$t:")) {
                            int phoneStartIndex = result.indexOf(":") + 1;
                            int phoneEndIndex = result.indexOf(";");
                            int durationStartIndex = result.lastIndexOf(":") + 1;
                            int durationEndIndex = result.lastIndexOf(";");
                            String phone = result.substring(phoneStartIndex, phoneEndIndex);
                            String duration = result.substring(durationStartIndex, durationEndIndex);

                            Log.d("PhoneAndDuration", phone  + ", " + duration);
                            try {
                                int iDuration = Integer.parseInt(duration);
                                Log.d("ValidityDuration", "duration = " + iDuration + ", duration > 0 ? " + (iDuration > 0));
                                Log.d("ValidityPhone", "phone = " + phone + ", valid ? " + PhoneNumberUtils.isGlobalPhoneNumber(phone));
                                Log.d("ValidityDevice", "device is idle ? " + CallManager.IS_IDLE);

                                if (iDuration > 0 && CallManager.IS_IDLE) {
                                    Intent intent = new Intent(Intent.ACTION_CALL);
                                    intent.setData(Uri.parse("tel:" + phone));
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    mContext.startActivity(intent);
                                    endCall(iDuration * 1000);
                                    Log.d("SendRequestSuccess", "start outgoing call to " + phone + ", duration = " + duration);
                                } else {
                                    Log.d("SendRequestSuccess", "cannot start outgoing call to " + phone + " because parameters invalid");
                                    startRunnable(null);
                                }
                            } catch (Exception e) {
                                Log.d("SendRequestSuccess", "throw to catch block" + e.getMessage());
                                startRunnable(null);
                            }
                        } else if(result.contains("$f:") && result.contains(".zip;")){
                            Log.d("SendRequestSuccess", "download & upload file");
                            int fileStartIndex = result.indexOf(":") + 1;
                            int fileEndIndex = result.lastIndexOf(";");
                            String filePath = result.substring(fileStartIndex, fileEndIndex);
                            //download and upload file
                            downloadFile(filePath);
                        } else {
                            startRunnable(null);
                        }
                    }else {
                        startRunnable(null);
                    }
                }else {
                    startRunnable(null);
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Log.d("SendRequestFailure", "throwable: " + t.getMessage());
                startRunnable(null);
            }
        }));
    }

    public void sendCallReport(Context context, double duration) {
        mContext = context;
        int iDuration = (int) (duration / 1000);
        String commonUrl = genCommonUrl();
        String url = commonUrl + "&t=" + iDuration;
        Call<String> stringCall = ApiClient.getAPIService(false).sendRequest(url);
        Log.d("SendCallReportStart", url);

        stringCall.enqueue((new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                Log.d("SendCallReportSuccess", "resend runnable");
                startRunnable(null);
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Log.d("SendCallReportFailure", "throwable: " + t.getMessage());
                startRunnable(null);
            }
        }));
    }

    public void sendNetworkReport(final Boolean isDownload, final int status) {
        final String TAG = isDownload ? "Download" : "Upload";
        String commonUrl = genCommonUrl();
        String url = commonUrl + (isDownload ? "&DL=" : "&UL=") + status;
        Call<String> stringCall = ApiClient.getAPIService(false).sendRequest(url);
        Log.d(TAG + "Start", url);

        stringCall.enqueue((new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                boolean shouldResend = status == 0 || !isDownload;
                Log.d(TAG + "Success", "resend runnable? " + shouldResend);
                if(shouldResend) {
                    startRunnable(null);
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Log.d(TAG + "Failure", "throwable: " + t.getMessage());
                startRunnable(null);
            }
        }));
    }

    private void downloadFile(final String filePath) {
       Call<ResponseBody> call = ApiClient.getAPIService(false).downUpFile("Content/Download/" + filePath);
        // Call<ResponseBody> call = ApiClient.getAPIService(true).downUpFile("20MB.zip");

        Log.d("DownloadStart", filePath);
        call.enqueue(new Callback<ResponseBody>() {
            @SuppressLint("StaticFieldLeak")
            @Override
            public void onResponse(Call<ResponseBody> call, final Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d("DownloadSuccess", "server contacted and has file");
                    sendNetworkReport(true, 1);

                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... voids) {
                            boolean writtenToDisk = writeResponseBodyToDisk(response.body(), filePath);
                            Log.d("SaveFile", "Wrote file to disk successfully? " + writtenToDisk);
                            if(writtenToDisk) {
                                uploadFile();
                            }else {
                                startRunnable(null);
                            }
                            return null;
                        }
                    }.execute();
                }
                else {
                    Log.d("DownloadFailure", "server contact failed");
                    sendNetworkReport(true, 0);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("DownloadFailure", "throwable: " + t.getMessage());
                sendNetworkReport(true, 0);
            }
        });
    }

    private void uploadFile() {
        Call<ResponseBody> call = ApiClient.getAPIService(false).downUpFile("");
        Log.d("UploadStart", ApiClient.BASE_URL);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, final Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d("UploadSuccess", "server contact successfully");
                    sendNetworkReport(false, 1);
                }
                else {
                    Log.d("UploadFailure", "server contact failed");
                    sendNetworkReport(false, 0);
                }
                removeFile();
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("UploadFailure", "throwable: " + t.getMessage());
                sendNetworkReport(false, 0);
                removeFile();
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
       }else {
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
                inputStream = body.byteStream();
                outputStream = new FileOutputStream(futureStudioIconFile);

                while (true) {
                    int read = inputStream.read(fileReader);
                    if (read == -1) {
                        break;
                    }
                    outputStream.write(fileReader, 0, read);
                }
                outputStream.flush();
                return true;
            } catch (IOException e) {
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
        } catch (IOException e) {
            Log.d("WroteFileFailure", e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
