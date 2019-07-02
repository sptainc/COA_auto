package com.callcenter;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.util.Log;
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

        this.registerReceiver(this.mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        addViews();
    }

    @Override
    public void onResume() {
        super.onResume();
        this.registerReceiver(this.mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    @Override
    public void onPause() {
        super.onPause();
        this.unregisterReceiver(this.mBatInfoReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(this.mBatInfoReceiver);
    }


    private void addViews() {
        double latitude = Constants.LAT;
        double longitude = Constants.LONG;
        String imei = Constants.IMEI;
        String phone = Constants.PHONE_NUMBER;
        String gen = Constants.GENERATION;


        Log.v("AAAAA NUMBER", Constants.PHONE_NUMBER);

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
