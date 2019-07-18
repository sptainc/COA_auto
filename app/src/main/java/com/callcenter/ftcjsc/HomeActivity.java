package com.callcenter.ftcjsc;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.callcenter.ftcjsc.R;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;


public class HomeActivity extends Activity {
    private Context mContext;

    // This method will be called when a MessageEvent is posted (in the UI thread for Toast)
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        TextView tv = findViewById(R.id.lbOutgoingDetail);
        tv.setText("Response data: " + event.message);
    }


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
        mContext = this;
        setContentView(R.layout.activity_home);

        this.registerReceiver(this.mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        addViews();

        addListener();
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
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }



    private void addViews() {
        double latitude = Constants.LAT;
        double longitude = Constants.LONG;
        String imei = Constants.IMEI;
        String phone = Constants.PHONE_NUMBER;
        String gen = Constants.GENERATION;


        TextView lbSimImei = findViewById(R.id.lbSimImei);
        TextView lbDeviceGen = findViewById(R.id.lbDeviceGen);
        TextView lbLatitude = findViewById(R.id.lbSimLat);
        TextView lbLongitude = findViewById(R.id.lbSimLng);
        TextView lbDeviceType = findViewById(R.id.lbDeviceType);
        TextView lbCountdownTimer = findViewById(R.id.lbCountdownTimer);
        TextView lblMCCMNC = findViewById(R.id.lblMCCMNC);
        TextView lblLACCIDPSC = findViewById(R.id.lblLACCIDPSC);

        lbDeviceType.setText("Device type: " + Constants.DEVICE_TYPE);
        lbCountdownTimer.setText("Delay timer (second): " + Constants.DELAY_TIME / 1000);

        lbSimImei.setText("Phone IMEI: " + imei);
        lbDeviceGen.setText("Device Generation: " + gen);
        lbLatitude.setText("Latitude: " + latitude);
        lbLongitude.setText("Longitude: " + longitude);

        TelephonyManager mTelephonyManager = (TelephonyManager)
                mContext.getSystemService(Context.TELEPHONY_SERVICE);
        GsmCellLocation gsmCellLocation = (GsmCellLocation)mTelephonyManager.getCellLocation();

        lblMCCMNC.setText("MCC: " + mTelephonyManager.getNetworkOperator().substring(0,3) + " - MNC: " + mTelephonyManager.getNetworkOperator().substring(3));
        String dataExten = "LAC (GSM Location Area Code): " + gsmCellLocation.getLac();
        dataExten += "\n CID (GSM Cell ID): " + gsmCellLocation.getCid();
        dataExten += "\n PSC: " + gsmCellLocation.getPsc();

        lblLACCIDPSC.setText( dataExten );



    }

    private void addListener() {
        findViewById(R.id.btnSettings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(HomeActivity.this, ConfigurationsActivity.class);
                i.putExtra("edit", "true");
                startActivityForResult(i, 1);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1 && resultCode == RESULT_OK) {
            addViews();
            TimerService instance = TimerService.getInstance();
            instance.stopInterval();
            instance.startInterval(null);
        }
    }
}

