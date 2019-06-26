package com.callcenter;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import java.util.Timer;
import java.util.TimerTask;

public class TimerService extends Service {
    private static TimerService instance = null;

    private static Timer timer;
    private static TimerTask timerTask;

    private TimerService() {};

    public static TimerService getInstance() {
        if(instance == null) {
            instance = new TimerService();
        }
        return instance;
    }


    @Override
    public void onCreate() {
        super.onCreate();

        startTimer();

    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }




    public void startTimer() {
        timer = new Timer();

        initializeTimerTask();

        timer.schedule(timerTask, 0, Utils.COUNTDOWN_TIMER);
    }


    /**
     * it sets the timer to print the counter every x seconds
     */
    private void initializeTimerTask() {
        timerTask = new TimerTask() {
            public void run() {
                int type = Utils.DEVICE_TYPE;
                if(type == 0) {
                    ApiUtils.sendRequestNumber(getApplicationContext());
                }else {
                    ApiUtils.sendReceiverReport(getApplicationContext());
                }
            }
        };
    }

    /**
     * not needed
     */
    public void stopTimerTask() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}
