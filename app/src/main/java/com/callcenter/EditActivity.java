package com.callcenter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public class EditActivity extends Activity {
    private RadioGroup rdGroup;
    private RadioButton rdCaller;
    private RadioButton rdReceiver;
    private EditText txtTimer;
    private EditText txtDelay;
    private Button btnConfirm;
    private Button btnCancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        addViews();

        addListener();
    }

    private void addViews() {
        final RadioButton rdCaller = findViewById(R.id.rdCaller);
        final RadioButton rdReceiver = findViewById(R.id.rdReceiver);
        final EditText txtTimer = findViewById(R.id.txtTimerValue);
        final EditText txtDelay = findViewById(R.id.txtDelayAnswer);
        Button btnConfirm = findViewById(R.id.btnConfirm);
        Button btnCancel = findViewById(R.id.btnCancel);
        RadioGroup rdGroup = findViewById(R.id.rdGroup);

        this.rdCaller = rdCaller;
        this.rdReceiver = rdReceiver;
        this.txtTimer = txtTimer;
        this.btnConfirm = btnConfirm;
        this.txtDelay = txtDelay;
        this.rdGroup = rdGroup;
        this.btnCancel = btnCancel;

        
        txtTimer.setText(Utils.COUNTDOWN_TIMER + "");
        txtDelay.setText(Utils.DELAY_TIME_TO_ANSWER + "");
        rdCaller.setChecked(Utils.DEVICE_TYPE == 0);
        rdReceiver.setChecked(Utils.DEVICE_TYPE == 1);

        txtTimer.setSelection(txtTimer.getText().length());
        txtDelay.setSelection(txtDelay.getText().length());
    }

    private void addListener() {
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String timer = txtTimer.getText().toString();
                String delay = txtDelay.getText().toString();

                AlertDialog.Builder builder = new AlertDialog.Builder(EditActivity.this);
                builder.setMessage("Please input a valid number of timer countdown!")
                        .setTitle("Invalid value")
                        .setCancelable(true)
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                            }
                        });
                builder.show();

                if (TextUtils.isEmpty(timer)) {
                    builder.setMessage("Please input a valid number of timer countdown!" + timer)
                            .setTitle("Invalid value");
                    builder.show();
                } else if (TextUtils.isEmpty(delay)) {
                    builder.setMessage("Please input a valid number of timer delay to auto answer!")
                            .setTitle("Invalid value");
                    builder.show();
                } else {
                    Utils.DELAY_TIME_TO_ANSWER = Integer.valueOf(delay);
                    Utils.COUNTDOWN_TIMER = Integer.valueOf(timer);
                    Utils.DEVICE_TYPE = rdCaller.isChecked() ? 0 : 1;

                    setResult(RESULT_OK);
                    finish();
                }
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
    }
}
