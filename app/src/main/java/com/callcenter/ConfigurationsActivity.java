package com.callcenter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

public class ConfigurationsActivity extends Activity {
    private RadioButton rdCaller;
    private EditText txtTimer;
    private Button btnConfirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent service = new Intent(ConfigurationsActivity.this, TimerService.class);
        startService(service);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ConfigurationsActivity.this);

        SharedPreferences.Editor editor = preferences.edit();

        getStorage(preferences);

        String firstTime = preferences.getString("firstTime", "Y");
        if (firstTime.equals("Y")) {
            setContentView(R.layout.activity_edit);
            addViews();
            addListener();

            editor.putString("firstTime", "N");
            editor.apply();
        } else {
            Intent i = new Intent(ConfigurationsActivity.this, HomeActivity.class);
            startActivity(i);
            finish();
        }
    }

    private void getStorage(SharedPreferences preferences) {
        String _type = preferences.getString("type", "0");
        String _delay = preferences.getString("delay", "15");

        int type = Integer.valueOf(_type);
        int delay = Integer.valueOf(_delay);

        Constants.DEVICE_TYPE = type;
        Constants.DELAY_TIME = delay * 1000;
    }

    private void addViews() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ConfigurationsActivity.this);

        String _type = preferences.getString("type", "0");
        String _delay = preferences.getString("delay", "15");

        int type = Integer.valueOf(_type);
        int delay = Integer.valueOf(_delay);

        Constants.DEVICE_TYPE = type;
        Constants.DELAY_TIME = delay * 1000;

        final RadioButton rdCaller = findViewById(R.id.rdCaller);
        final RadioButton rdReceiver = findViewById(R.id.rdReceiver);
        final EditText txtTimer = findViewById(R.id.txtTimerValue);
        Button btnConfirm = findViewById(R.id.btnConfirm);
        TextView lbCurrentType = findViewById(R.id.lbCurrentType);
        TextView lbCurrentDelay = findViewById(R.id.lbCurrentDelay);

        this.rdCaller = rdCaller;
        this.txtTimer = txtTimer;
        this.btnConfirm = btnConfirm;
        lbCurrentDelay.setText(lbCurrentDelay.getText().toString() + delay);
        lbCurrentType.setText(lbCurrentType.getText().toString() + type);


        rdCaller.setChecked(type == 0);
        rdReceiver.setChecked(type == 1);

        txtTimer.setText(_delay);
        txtTimer.setSelection(txtTimer.getText().length());
    }

    private void addListener() {
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String delay = txtTimer.getText().toString();

                AlertDialog.Builder builder = new AlertDialog.Builder(ConfigurationsActivity.this);
                builder.setMessage("Please input a valid number of timer countdown!")
                        .setTitle("Invalid value")
                        .setCancelable(true)
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                            }
                        });

                if (TextUtils.isEmpty(delay)) {
                    builder.setMessage("Please input a valid number of timer countdown!" + delay)
                            .setTitle("Invalid value");
                    builder.show();
                } else {
                    Intent i = new Intent(ConfigurationsActivity.this, HomeActivity.class);
                    String type = rdCaller.isChecked() ? "0" : "1";
                    i.putExtra("delay", delay);
                    i.putExtra("type", type);

                    // storage for current session
                    Constants.DEVICE_TYPE = Integer.valueOf(type);
                    Constants.DELAY_TIME = Integer.valueOf(delay) * 1000;

                    // storage for next session
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ConfigurationsActivity.this);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString("type", type);
                    editor.putString("delay", delay);
                    editor.apply();

                    startActivity(i);
                    finish();
                }
            }
        });

    }
}
