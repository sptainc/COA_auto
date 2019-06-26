package com.callcenter;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class HomeActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        addViews();

        addListener();


        Intent service = new Intent(this, TimerService.class);

        startService(service);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Utils.RQC_ON_CONFIGURSTIONS_SUCCESS && resultCode == RESULT_OK) {
            setValueFromIntent();
            TimerService.getInstance().stopTimerTask();
            TimerService.getInstance().startTimer();
        }
    }



    private void addListener() {

        Button btnEdit = findViewById(R.id.btnEdit);

        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, EditActivity.class);
                startActivityForResult(intent, Utils.RQC_ON_CONFIGURSTIONS_SUCCESS);
            }
        });
    }


    private void addViews() {
        GPSTracker gps = new GPSTracker(this);
        double latitude = gps.getLatitude();
        double longitude = gps.getLongitude();

        TextView lbPhoneNumber = findViewById(R.id.lbPhoneNumber);
        TextView lbSimImei = findViewById(R.id.lbSimImei);
        TextView lbDeviceGen = findViewById(R.id.lbDeviceGen);
        TextView lbLatitude = findViewById(R.id.lbSimLat);
        TextView lbLongitude = findViewById(R.id.lbSimLng);


        lbPhoneNumber.setText("Phone number: " + Utils.getSimPhoneNumber(HomeActivity.this));
        lbSimImei.setText("Sim IMEI number: " + Utils.getSimSerialNumber(HomeActivity.this));
        lbDeviceGen.setText("Device Generation: " + Utils.getDeviceGeneration(HomeActivity.this));
        lbLatitude.setText("Latitude: " + latitude);
        lbLongitude.setText("Longitude: " + longitude);

        setValueFromIntent();
    }

    private void setValueFromIntent() {
        TextView lbDeviceType = findViewById(R.id.lbDeviceType);
        TextView lbCountdownTimer = findViewById(R.id.lbCountdownTimer);
        TextView lbDelayAnswer = findViewById(R.id.lbDelayAnswer);

        lbDeviceType.setText("Device type: " + Utils.DEVICE_TYPE);
        lbCountdownTimer.setText("Countdown timer (second): " + Utils.COUNTDOWN_TIMER);
        lbDelayAnswer.setText("Delay timer (second): " + Utils.DELAY_TIME_TO_ANSWER);
    }
}
