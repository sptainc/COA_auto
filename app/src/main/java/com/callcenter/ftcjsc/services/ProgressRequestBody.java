package com.callcenter.ftcjsc.services;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;

public class ProgressRequestBody extends RequestBody {
    private File mFile;
    private ImageUploadCallback mListener;
    private String content_type;
    private static final int DEFAULT_BUFFER_SIZE = 2048;
    private int per = 0;

    public ProgressRequestBody(final File file, String content_type, final  ImageUploadCallback listener) {
        this.content_type = content_type;
        mFile = file;
        mListener = listener;
    }
    @Nullable
    @Override
    public MediaType contentType() {
        return MediaType.parse(content_type+"/*");
    }
    @Override
    public long contentLength() throws IOException {
        return mFile.length();
    }
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void writeTo(@NonNull BufferedSink sink) throws IOException {
        long fileLength = mFile.length();
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        long uploaded = 0;
        try (FileInputStream in = new FileInputStream(mFile)) {
            int read;
            Handler handler = new Handler(Looper.getMainLooper());
            while ((read = in.read(buffer)) != -1) {
                uploaded += read;
                sink.write(buffer, 0, read);

                int newPer = (int)(100 * uploaded / fileLength);
                if(newPer - per > 2) {
                    this.per = newPer;
                    handler.post(new ProgressUpdater(uploaded, fileLength));
                }
            }
        }
    }
    private class ProgressUpdater implements Runnable {
        private long mUploaded;
        private long mTotal;
        ProgressUpdater(long uploaded, long total) {
            mUploaded = uploaded;
            mTotal = total;
        }
        @Override
        public void run() {
            if(mListener!=null)
                mListener.onUploadProgressUpdate((int)(100 * mUploaded / mTotal));
        }
    }
}