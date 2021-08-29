package com.callcenter.ftcjsc;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;

import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.callcenter.ftcjsc.services.MessageEvent;
import com.callcenter.ftcjsc.services.TimerService;
import com.callcenter.ftcjsc.services.StorageKeys;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class HomeActivity extends Activity {
    private final String TAG = "ActivityHOME";

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
//        Log.d(TAG, "MessageEvent: " + event.message);
        ((TextView) findViewById(R.id.tvProcess)).setText(event.message);
        ((ScrollView) findViewById(R.id.scrollView)).fullScroll(View.FOCUS_DOWN);
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

        boolean isUpdate = TimerService.updateConstants(this);
        if(isUpdate) {
            Intent service = new Intent(this, TimerService.class);
            startService(service);
        }
        addViews();
        Log.d(TAG, "onCreate");
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        EventBus.getDefault().register(this);
        ((TextView) findViewById(R.id.tvProcess)).setText(MessageEvent.globalMessage);
        ((ScrollView) findViewById(R.id.scrollView)).fullScroll(View.FOCUS_DOWN);
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
        ((TextView) findViewById(R.id.lbIds)).setText(getResources().getText(R.string.ids) + TimerService.ids);
        ((TextView) findViewById(R.id.lbIdm)).setText(getResources().getText(R.string.idm) + TimerService.idm);
        ((TextView) findViewById(R.id.lbDeviceGen)).setText(getResources().getText(R.string.gen) + TimerService.generation);
        ((TextView) findViewById(R.id.txtUserInput)).setText(getResources().getText(R.string.user_input) + TimerService.userInput);
        findViewById(R.id.btnEditUserInput).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onItemSelected(view.getId());
            }
        });
        String s = TimerService.ids;
        Button btn = findViewById(R.id.btnEditUserInput);
        if(s == null || TextUtils.isEmpty(s))  {
            btn.setVisibility(View.VISIBLE);
        }else {
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onItemSelected(view.getId());
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(TimerService.ids == null ? R.menu.menu_double : R.menu.menu_single, menu);
        return true;
    }

    private void onItemSelected(final int id) {
        try {
            final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(HomeActivity.this);
            final SharedPreferences.Editor editor = preferences.edit();

            String title = id == R.id.btnEditUserInput ? "Edit user input" : "Edit ids prefix";
            String key = id == R.id.btnEditUserInput ? StorageKeys.user_input.toString() : StorageKeys.ids_input.toString();
            String defaultValue = preferences.getString(key, "");
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
            alertDialog.setTitle(title);
            alertDialog.setMessage("Input value less than 10 characters");
            final EditText input = new EditText(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT);
            input.setPadding(15, 15, 15, 15);
            input.setSingleLine();
            lp.setMargins(5, 5, 5, 5);
            input.setHint("Input value here");
            input.setText(defaultValue);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                input.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            }
            input.setLayoutParams(lp);
            input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    input.post(new Runnable() {
                        @Override
                        public void run() {
                            InputMethodManager inputMethodManager = (InputMethodManager) HomeActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
                            inputMethodManager.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
                        }
                    });
                }
            });
            input.requestFocus();
            alertDialog.setView(input);
            alertDialog.setIcon(android.R.drawable.ic_menu_edit);

            alertDialog.setPositiveButton("Update",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            String text = input.getText().toString();
                            if (id == R.id.btnEditUserInput) {
                                TimerService.userInput = text;
                                editor.putString(StorageKeys.user_input.toString(), text);
                                editor.apply();
                            } else if (id == R.id.btnEditIds) {
                                TimerService.ids = text + TimerService.idm;
                                editor.putString(StorageKeys.ids_input.toString(), text);
                                editor.apply();
                            }
                            addViews();
                            TimerService.getInstance().startRunnable(null);
                        }
                    });
            alertDialog.setNegativeButton("Cancel",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                            TimerService.getInstance().startRunnable(null);
                        }
                    });
            alertDialog.show();
        }catch (Exception e) {
            Log.d("Crash", e.getMessage());
            e.printStackTrace();
        }
    }
}

