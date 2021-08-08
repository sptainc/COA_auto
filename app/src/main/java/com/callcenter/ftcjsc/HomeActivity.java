package com.callcenter.ftcjsc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.core.content.ContextCompat;

import com.callcenter.ftcjsc.services.MessageEvent;
import com.callcenter.ftcjsc.services.TimerService;
import com.callcenter.ftcjsc.utils.Constants;
import com.callcenter.ftcjsc.utils.RequestCodes;
import com.callcenter.ftcjsc.utils.StorageKeys;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class HomeActivity extends AppCompatActivity {
    private final String TAG = "ActivityHOME";
    private Boolean editable = false;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
//        Log.d(TAG, "MessageEvent: " + event.message);
        ((TextView)findViewById(R.id.tvProcess)).setText(event.message);
        ((ScrollView)findViewById(R.id.scrollView)).fullScroll(View.FOCUS_DOWN);
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
        Intent service = new Intent(this, TimerService.class);
        startService(service);
        addViews();
        addListener();
        Log.d(TAG, "onCreate");
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        EventBus.getDefault().register(this);
        ((TextView)findViewById(R.id.tvProcess)).setText(MessageEvent.globalMessage);
        ((ScrollView)findViewById(R.id.scrollView)).fullScroll(View.FOCUS_DOWN);
        Log.d(TAG, "onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mBatInfoReceiver);
        EventBus.getDefault().unregister(this);
        Log.d(TAG, "onPause");
    }

    private void addViews() {
        ((TextView)findViewById(R.id.lbIdm)).setText(getResources().getText(R.string.idm) + Constants.getIdm());
        ((TextView)findViewById(R.id.lbIds)).setText(getResources().getText(R.string.ids) + Constants.getIds());
        ((TextView)findViewById(R.id.lbDeviceGen)).setText(getResources().getText(R.string.gen) + Constants.getGeneration());
        ((EditText)findViewById(R.id.txtUserInput)).setText(Constants.getUserInput());
    }

    private void addListener() {
        final EditText et = findViewById(R.id.txtUserInput);
        final Button btn = findViewById(R.id.btnEdit);
        final LinearLayout lo = findViewById(R.id.loEdit);
        btn.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void onClick(View view) {
                if(!editable) {
                    et.setEnabled(true);
                    et.requestFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT);
                    et.setSelection(et.getText().length());
                    btn.setBackground(ContextCompat.getDrawable(HomeActivity.this, R.drawable.ic_checked));
                    lo.setBackground(getResources().getDrawable(R.drawable.border_radius));
                }else {
                    String text = et.getText().toString();
                    text = TextUtils.isEmpty(text) ? "" : text;
                    et.setEnabled(false);
                    Constants.setUserInput(text);
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(HomeActivity.this);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString(StorageKeys.user_input.toString(), text);
                    editor.apply();
                    btn.setBackground(ContextCompat.getDrawable(HomeActivity.this, R.drawable.ic_edit));
                    lo.setBackground(getResources().getDrawable(R.drawable.border_radius_disabled));
                    TimerService.getInstance().startRunnable(null);
                }
                editable = !editable;
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult");
        Log.d("onActivityResult", "requestCode: " + requestCode + ", resultCode: " + resultCode);
        if (resultCode == RESULT_OK && requestCode == RequestCodes.onConfigurationsSuccess) {
            TimerService.getInstance().startRunnable(null);
            addViews();
        }
    }
}

