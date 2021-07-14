package com.callcenter.ftcjsc;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import com.callcenter.ftcjsc.utils.Constants;
import com.callcenter.ftcjsc.utils.StorageKeys;

public class ConfigActivity extends AppCompatActivity {
    private RadioButton rdCaller;
    private EditText txtTimer;
    private Button btnConfirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);
        addViews();
        addListener();
    }

    private void addViews() {
        int iType = Constants.getDeviceType();
        int iDelay = Constants.getDelayTime();

        final RadioButton rdCaller = findViewById(R.id.rdCaller);
        final RadioButton rdReceiver = findViewById(R.id.rdReceiver);
        final EditText txtTimer = findViewById(R.id.txtTimerValue);
        Button btnConfirm = findViewById(R.id.btnConfirm);
        TextView lbCurrentType = findViewById(R.id.lbCurrentType);
        TextView lbCurrentDelay = findViewById(R.id.lbCurrentDelay);

        this.rdCaller = rdCaller;
        this.txtTimer = txtTimer;
        this.btnConfirm = btnConfirm;

        lbCurrentDelay.setText(lbCurrentDelay.getText().toString() + iDelay);
        lbCurrentType.setText(lbCurrentType.getText().toString() + Constants.getLabel(iType));

        rdCaller.setText(Constants.getLabel(0));
        rdCaller.setChecked(iType == 0);

        rdReceiver.setText(Constants.getLabel(1));
        rdReceiver.setChecked(iType == 1);

        txtTimer.setText(iDelay + "");
        txtTimer.setSelection(txtTimer.getText().length());
    }

    private void addListener() {
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String sDelay = txtTimer.getText().toString();

                AlertDialog.Builder builder = new AlertDialog.Builder(ConfigActivity.this);
                builder.setCancelable(true)
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                            }
                        });

                if (TextUtils.isEmpty(sDelay)) {
                    builder.setMessage("Please input a valid number of timer countdown!" + sDelay)
                            .setTitle("Invalid value");
                    builder.show();
                } else {
                    int iDelay = Integer.valueOf(sDelay);
                    int type = rdCaller.isChecked() ? 0 : 1;

                    // storage for next session
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ConfigActivity.this);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putInt(StorageKeys.device_type.toString(), type);
                    editor.putInt(StorageKeys.delay_time.toString(), iDelay);
                    editor.apply();

                    Constants.setDelayTime(iDelay);
                    Constants.setDeviceType(type);

                    setResult(Activity.RESULT_OK);
                    finish();
                }
            }
        });

    }
}
