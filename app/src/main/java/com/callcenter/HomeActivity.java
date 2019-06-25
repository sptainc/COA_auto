package com.callcenter;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.os.BatteryManager;
import android.content.IntentFilter;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class HomeActivity extends Activity {

    private static final String TAG_HTTP_URL_CONNECTION = "HTTP_URL_CONNECTION";

    // Child thread sent message type value to activity main thread Handler.
    private static final int REQUEST_CODE_SHOW_RESPONSE_TEXT = 1;

    // The key of message stored server returned data.
    private static final String KEY_RESPONSE_TEXT = "KEY_RESPONSE_TEXT";

    // Request method GET. The value must be uppercase.
    private static final String REQUEST_METHOD_GET = "GET";

    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {
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

        fetch();

        this.registerReceiver(this.mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));



    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Utils.RQC_ON_CONFIGURSTIONS_SUCCESS && resultCode == RESULT_OK) {
            setValueFromIntent();

//        Intent intent = new Intent(Intent.ACTION_CALL);
//
//        intent.setData(Uri.parse("tel:" + "+84919549468"));
//        HomeActivity.this.startActivity(intent);
        }
    }

    private void fetch() {


        GPSTracker gps = new GPSTracker(this);
        double latitude = gps.getLatitude();
        double longitude = gps.getLongitude();

        TextView lbLatitude = findViewById(R.id.lbSimLat);
        TextView lbLongitude = findViewById(R.id.lbSimLng);

        lbLatitude.setText("Latitude: " + latitude);
        lbLongitude.setText("Longitude: " + longitude);
    }


    private void addViews() {
        TextView lbPhoneNumber = findViewById(R.id.lbPhoneNumber);
        TextView lbSimImei = findViewById(R.id.lbSimImei);
        TextView lbDeviceGen = findViewById(R.id.lbDeviceGen);

        Button btnEdit = findViewById(R.id.btnEdit);

        btnEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, EditActivity.class);
                startActivityForResult(intent, Utils.RQC_ON_CONFIGURSTIONS_SUCCESS);
            }
        });


        lbPhoneNumber.setText("Phone number: " + Utils.getSimPhoneNumber(HomeActivity.this));
        lbSimImei.setText("Sim IMEI number: " + Utils.getSimSerialNumber(HomeActivity.this));
        lbDeviceGen.setText("Device Generation: " + Utils.getDeviceGeneration(HomeActivity.this));

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
