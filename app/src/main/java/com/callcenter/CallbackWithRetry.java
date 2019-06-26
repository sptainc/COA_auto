package com.callcenter;

import android.os.Handler;

import retrofit2.Call;
import retrofit2.Callback;

public abstract class CallbackWithRetry<T> implements Callback<T> {
    private static boolean WILL_RETRY = true;
    private final Call<T> call;

    public CallbackWithRetry(Call<T> call) {
        this.call = call;
    }

    public static void cancelRetry() {
        WILL_RETRY = false;
    }

    public static void startRetry() {
        WILL_RETRY = true;
    }

    public static boolean getRetryStatus() {
        return WILL_RETRY;
    }

    @Override
    public void onResponse(Call<T> call, retrofit2.Response<T> response) {
        WILL_RETRY = false;
    }

    @Override
    public void onFailure(Call<T> call, Throwable t) {
        if (WILL_RETRY) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    retry();
                }
            }, 3000);
        }
    }

    private void retry() {
        call.clone().enqueue(this);
    }
}