package com.callcenter.ftcjsc;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

public class ConfigurationsActivity extends AppCompatActivity {
    private RadioButton rdCaller;
    private EditText txtTimer;
    private Button btnConfirm;

    public static final int MY_PERMISSIONS_REQUEST_CODE = 99;

    private  void goMainView () {
        Intent service = new Intent(ConfigurationsActivity.this, TimerService.class);

        startService(service);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ConfigurationsActivity.this);

        SharedPreferences.Editor editor = preferences.edit();

        getStorage(preferences);

        String firstTime = preferences.getString("firstTime", "Y");

        Intent in = getIntent();

        String isEdit = in.getStringExtra("edit");

        if (firstTime.equals("Y") || !TextUtils.isEmpty(isEdit)) {
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

    public boolean checkLocationPermission() {
        if (
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(ConfigurationsActivity.this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.CALL_PHONE
                    },
                    MY_PERMISSIONS_REQUEST_CODE);
            return false;
        } else {
            goMainView();
            return true;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkLocationPermission();
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

                    Intent in = getIntent();

                    String isEdit = in.getStringExtra("edit");



                    if (TextUtils.isEmpty(isEdit)) {
                        startActivity(i);
                        finish();
                    }else {
                        setResult(AppCompatActivity.RESULT_OK);
                        finish();
                    }
                }
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String permissions[],
            int[] grantResults) {
        if(requestCode == MY_PERMISSIONS_REQUEST_CODE) {
            for(int i = 0; i< grantResults.length; i++) {
                if(grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this)
                            .setTitle("Authorize permission before use")
                            .setMessage("Please go to Settings => Applications => CallCenter and authorize the permissions before use")
                            .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                    Intent intent = new Intent();
                                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                                    intent.setData(uri);
                                    startActivity(intent);
                                }
                            });
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                    return;
                }
            }
            goMainView();

        }

    }
}
