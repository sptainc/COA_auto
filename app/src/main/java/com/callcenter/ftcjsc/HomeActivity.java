package com.callcenter.ftcjsc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.appcompat.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import com.callcenter.ftcjsc.services.MessageEvent;
import com.callcenter.ftcjsc.services.TimerService;
import com.callcenter.ftcjsc.utils.Constants;
import com.callcenter.ftcjsc.utils.RequestCodes;
import com.callcenter.ftcjsc.utils.StorageKeys;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;


public class HomeActivity extends AppCompatActivity {
//    private LocationManager mLocationManager;

    // This method will be called when a MessageEvent is posted (in the UI thread for Toast)
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        TextView tv = findViewById(R.id.lbOutgoingDetail);
        tv.setText(getResources().getString(R.string.outgoing_detail) + " " + event.message);
    }

    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context _, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            TextView lbPinLevel = findViewById(R.id.lbPinLevel);
            lbPinLevel.setText(getResources().getString(R.string.pin_percent) + level + "%");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

//        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Intent service = new Intent(this, TimerService.class);
        startService(service);

//        checkGpsEnabled();
//        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, Constants.MIN_TIME_BW_UPDATES, Constants.MIN_DISTANCE_CHANGE_FOR_UPDATES, locationListenerGPS);
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
//        ((TextView)findViewById(R.id.lbSimLat)).setText("" + getResources().getText(R.string.lat) + Constants.getLatitude());
//        ((TextView)findViewById(R.id.lbSimLng)).setText("" + getResources().getText(R.string.lng) + Constants.getLongitude());

        ((TextView)findViewById(R.id.lbDeviceType)).setText(getResources().getText(R.string.device_type) + Constants.getLabel(Constants.getDeviceType()));
        ((TextView)findViewById(R.id.lbCountdownTimer)).setText("" + getResources().getText(R.string.delay) + Constants.getDelayTime() / 1000);
//        ((TextView)findViewById(R.id.lbPhone)).setText(getResources().getText(R.string.phone) + Constants.getPhoneNumber());
        ((TextView)findViewById(R.id.lbIdm)).setText(getResources().getText(R.string.idm) + Constants.getIdm());
        ((TextView)findViewById(R.id.lbIds)).setText(getResources().getText(R.string.ids) + Constants.getIds());
        ((TextView)findViewById(R.id.lbDeviceGen)).setText(getResources().getText(R.string.gen) + Constants.getGeneration());
        ((TextView)findViewById(R.id.lblMCC)).setText(getResources().getText(R.string.mcc) + Constants.getMcc());
        ((TextView)findViewById(R.id.lblMNC)).setText(getResources().getText(R.string.mnc) + Constants.getMnc());
        ((TextView)findViewById(R.id.lblLAC)).setText("" + getResources().getText(R.string.lac) + Constants.getLac());
        ((TextView)findViewById(R.id.lblCID)).setText("" + getResources().getText(R.string.cid) + Constants.getCid());
        ((TextView)findViewById(R.id.lblPSC)).setText("" + getResources().getText(R.string.psc) + Constants.getPsc());
        ((EditText)findViewById(R.id.txtUserInput)).setText(Constants.getUserInput());
    }

    private void addListener() {
        findViewById(R.id.btnConfig).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent i = new Intent(HomeActivity.this, ConfigActivity.class);
//                startActivityForResult(i, RequestCodes.onConfigurationsSuccess);
            }
        });

        final EditText et = findViewById(R.id.txtUserInput);
        findViewById(R.id.checkbox).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final CheckBox mapView = (CheckBox) view;
                boolean checked = mapView.isChecked();
                if(checked) {
                    et.setEnabled(true);
                    et.requestFocus();
                }else {
                    String text = et.getText().toString();
                    text = TextUtils.isEmpty(text) ? "" : text;
                    et.setEnabled(false);
                    Constants.setUserInput(text);
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(HomeActivity.this);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString(StorageKeys.user_input.toString(), text);
                    editor.apply();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("onActivityResult", "requestCode: " + requestCode + ", resultCode: " + resultCode);
        if (resultCode == RESULT_OK && requestCode == RequestCodes.onConfigurationsSuccess) {
            TimerService.getInstance().startRunnable(null);
            addViews();
        }
    }
}

