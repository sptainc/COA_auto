package com.callcenter;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.widget.TextView;

public class HomeActivity extends Activity {
    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context ctxt, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            TextView lbPinLevel = findViewById(R.id.lbPinLevel);
            lbPinLevel.setText("Pin level: " + String.valueOf(level) + "%");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        addViews();

         this.registerReceiver(this.mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        Intent service = new Intent(HomeActivity.this, TimerService.class);

        startService(service);
    }

//    public void onPause() {
//        super.onPause();
//        this.unregisterReceiver(mBatInfoReceiver);
//    }
//
//    public void onResume() {
//        super.onResume();
//        registerReceiver(mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
//    }


    private void addViews() {
        HelperService gps = new HelperService(HomeActivity.this);
        double latitude = gps.getLatitude();
        double longitude = gps.getLongitude();
        String imei = gps.getSimSerialNumber();
        String phone = gps.getSimPhoneNumber();
        String gen = gps.getDeviceGeneration();

        TextView lbPhoneNumber = findViewById(R.id.lbPhoneNumber);
        TextView lbSimImei = findViewById(R.id.lbSimImei);
        TextView lbDeviceGen = findViewById(R.id.lbDeviceGen);
        TextView lbLatitude = findViewById(R.id.lbSimLat);
        TextView lbLongitude = findViewById(R.id.lbSimLng);


        lbPhoneNumber.setText("Phone number: " + phone);
        lbSimImei.setText("Sim IMEI number: " + imei);
        lbDeviceGen.setText("Device Generation: " + gen);
        lbLatitude.setText("Latitude: " + latitude);
        lbLongitude.setText("Longitude: " + longitude);

        setValueFromIntent();
    }

    private void setValueFromIntent() {
        TextView lbDeviceType = findViewById(R.id.lbDeviceType);
        TextView lbCountdownTimer = findViewById(R.id.lbCountdownTimer);

        Intent i = getIntent();
        String type = i.getStringExtra("type");
        String delay = i.getStringExtra("delay");


        lbDeviceType.setText("Device type: " + type);
        lbCountdownTimer.setText("Delay timer (second): " + delay);
    }
}
