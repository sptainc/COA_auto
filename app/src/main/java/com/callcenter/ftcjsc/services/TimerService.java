package com.callcenter.ftcjsc.services;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import com.callcenter.ftcjsc.utils.Constants;
import com.callcenter.ftcjsc.utils.Utils;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
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
        Log.d("VERSION", Build.VERSION.SDK_INT + "");
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
                if (CallManager.IS_IDLE && !fetchingFile) {
                    sendRequest();
                    lastRequestTime = new Date().getTime();
                } else {
                    requestHandler.postDelayed(requestRunnable, Constants.getDelayTime());
                }
            }
        };
        requestHandler.postDelayed(requestRunnable, Constants.getDelayTime());
    }

    public static void clearProcess() {
        process.clear();
        EventBus.getDefault().post(new MessageEvent(""));
    }

    public static void addProcess(String proc) {
        process.add("->  " + proc + " (" + Utils.getTime() + ")\n\n");
        String str = "";
        for (String s : process) {
            str += s;
        }
        EventBus.getDefault().post(new MessageEvent(str));
    }

    private void logResponse(Response<String> response) {
        String body = "";
        String res = response.toString();
        try {
            body = response.body();
        } catch (Exception e) {
        }
        Log.d("RESPONSE", TextUtils.isEmpty(res) ? "[empty response]" : res);
        Log.d("RESPONSE body", TextUtils.isEmpty(body) ? "[empty body]" : res);
    }

    private void logResponseBody(Response<ResponseBody> response) {
        String body = "";
        String res = response.toString();
        try {
            body = response.body().toString();
        } catch (Exception e) {
        }
        Log.d("RESPONSE", TextUtils.isEmpty(res) ? "[empty response]" : res);
        Log.d("RESPONSE body", TextUtils.isEmpty(body) ? "[empty body]" : res);
    }

    private void logFailure(Throwable t) {
        String mess = t.toString();
        try {
            String s = t.getMessage();
            if (TextUtils.isEmpty(s)) {
                mess = s;
            }
        } catch (Exception e) {
        }
        Log.d("THROWABLE", TextUtils.isEmpty(mess) ? "[empty message]" : mess);
    }

    private String genCommonUrl(int t) {
        String idm = Constants.getIdm();
        String ids = Constants.getIds();
        String generation = Constants.getGeneration();
        String name = Constants.getDeviceName();

        return "?IDS=" + ids + "&IDM=" + idm + "&G=" + generation + "&D=" + name + "&P=" + Constants.getUserInput() + "&t=" + t;
    }

    public void sendRequest() {
        clearProcess();
        TimerService.addProcess("Sending request");
        String url = genCommonUrl(0);
        Call<String> stringCall = ApiClient.getInstance().sendRequest(url);
        stringCall.enqueue((new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                logResponse(response);
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

                                    Intent intent = new Intent(Intent.ACTION_CALL);
                                    intent.setData(Uri.parse("tel:" + phone));
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    Constants.OUTGOING_CALL_TIME = iDuration;
                                    mContext.startActivity(intent);
                                } else {
                                    TimerService.addProcess("Cannot start outgoing call to phone because parameters invalid, restart process");
                                    startRunnable(null);
                                }
                            } catch (Exception e) {
                                TimerService.addProcess("There are errors occurred: " + e.getMessage() + ", restart process");
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
                logFailure(t);
                TimerService.addProcess("Send request failure: " + t.getMessage() + ", restart process");
                startRunnable(null);
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
                logResponse(response);
                TimerService.addProcess("Send call report success, number = " + number + ", duration = " + duration + "s, restart process");
                startRunnable(null);
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                logFailure(t);
                TimerService.addProcess("Send call report failure, number = " + number + ", duration = " + duration + "s, restart process");
                startRunnable(null);
            }
        }));
    }

    public void sendNetworkReport(final Boolean isDownload, final int status) {
        String url = genCommonUrl(status);
        Call<String> stringCall = ApiClient.getInstance().sendRequest(url);

        stringCall.enqueue((new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                logResponse(response);
                boolean shouldResend = status == 0 || !isDownload;
                TimerService.addProcess("Send network report success");
                if (shouldResend) {
                    startRunnable(null);
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                logFailure(t);
                TimerService.addProcess("Send network report failure: " + t.getMessage());
                startRunnable(null);
            }
        }));
    }

    private void downloadFile(final String filePath) {
        try {
            File f = new File(getExternalFilesDir(null) + File.separator + filePath);
            if (f.exists() && f.length() / 1024 > 10) {
                file = f;
                uploadFile();
                return;
            }
            removeAllFiles();
            Call<ResponseBody> call = ApiClient.getInstanceDownload().downloadFile(ApiClient.DOWNLOAD_URL + filePath);
            fetchingFile = true;

            call.enqueue(new Callback<ResponseBody>() {
                @SuppressLint("StaticFieldLeak")
                @Override
                public void onResponse(Call<ResponseBody> call, final Response<ResponseBody> response) {
                    logResponseBody(response);
                    if (response.isSuccessful() && response.body() != null) {
                        new AsyncTask<Void, Void, Void>() {
                            @Override
                            protected Void doInBackground(Void... voids) {
                                TimerService.addProcess("Download & Saving the file to disk, this can be time consuming with large file ...");
                                boolean writtenToDisk = writeResponseBodyToDisk(response.body(), filePath);
                                fetchingFile = false;
                                if (writtenToDisk) {
                                    TimerService.addProcess("Download file success, save file " + (writtenToDisk ? "success" : "failure"));
                                    uploadFile();
                                } else {
                                    TimerService.addProcess("Download file failure, " + response.toString());
                                    startRunnable(null);
                                }
                                return null;
                            }
                        }.execute();
                    } else {
                        TimerService.addProcess("Download file failure");
                        startRunnable(null);
                        fetchingFile = false;
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    logFailure(t);
                    fetchingFile = false;
                    TimerService.addProcess("Download file failure: " + t.getMessage());
                    startRunnable(null);
                }
            });
        }catch (Exception e) {
            TimerService.addProcess("Download file failure: " + e.getMessage());
            startRunnable(null);
        }

    }

    private void uploadFile() {
        if (file == null) {
            TimerService.addProcess("Uploading file failure, file does not exist, restart process");
            startRunnable(null);
            return;
        }
        TimerService.addProcess("Uploading file, this can be time consuming with large file ...");
        RequestBody requestBody = RequestBody.create(MediaType.parse("*/*"), file);
        MultipartBody.Part fileToUpload = MultipartBody.Part.createFormData("file", file.getName(), requestBody);
        RequestBody filename = RequestBody.create(MediaType.parse("text/plain"), file.getName());

        Call<ResponseBody> call = ApiClient.getInstanceUpload().uploadFile(fileToUpload, filename);
        fetchingFile = true;

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, final Response<ResponseBody> response) {
                logResponseBody(response);
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
                logFailure(t);
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
