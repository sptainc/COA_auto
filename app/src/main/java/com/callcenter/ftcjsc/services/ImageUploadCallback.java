package com.callcenter.ftcjsc.services;

public interface ImageUploadCallback {
    void onUploadProgressUpdate(int percentage);
    void onUploadError(String message);
    void onUploadSuccess(String message);
}
