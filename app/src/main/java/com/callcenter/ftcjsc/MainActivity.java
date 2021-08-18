package com.callcenter.ftcjsc;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.PermissionChecker;

import android.util.Log;
import android.view.View;
import com.callcenter.ftcjsc.utils.RequestCodes;
import com.callcenter.ftcjsc.utils.Utils;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        addListeners();
        checkAndRequestPermission();
    }

    private void checkAndRequestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, RequestCodes.requestPermissions);
        }else {
            addViewsAndPreload();
        }
    }

    private void openSettingsToEnablePermissions(String permission) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Allow COA accesses "  + permission + " before continue")
                .setTitle("Grant permission")
                .setCancelable(false)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
                        startActivityForResult(intent, RequestCodes.openSettingsForPermissions);
                    }
                });
        builder.show();
    }

    private void addViewsAndPreload() {
        boolean phoneGranted = false;
        boolean locationGranted = false;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
             phoneGranted = PermissionChecker.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_PHONE_STATE) == PermissionChecker.PERMISSION_GRANTED;
             locationGranted = PermissionChecker.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PermissionChecker.PERMISSION_GRANTED;

        }else {
            phoneGranted = true;
            locationGranted = true;
        }

        (findViewById(R.id.btnPhonePermission)).setVisibility(phoneGranted ? View.GONE : View.VISIBLE);
        (findViewById(R.id.imgPhonePermission)).setVisibility(phoneGranted ? View.VISIBLE : View.GONE);

        (findViewById(R.id.btnLocationPermission)).setVisibility(locationGranted ? View.GONE : View.VISIBLE);
        (findViewById(R.id.imgLocationPermission)).setVisibility(locationGranted ? View.VISIBLE : View.GONE);

        if(phoneGranted && locationGranted) {
//            Utils.updateConstants(this);
//            Location location = ((LocationManager) getSystemService(LOCATION_SERVICE)).getLastKnownLocation(LocationManager.GPS_PROVIDER);
//            Constants.setLocation(((LocationManager) getSystemService(LOCATION_SERVICE)).getLastKnownLocation(LocationManager.GPS_PROVIDER));
            Intent i = new Intent(this, HomeActivity.class);
            startActivity(i);
            finish();
        }
    }

    private void addListeners() {
        findViewById(R.id.btnPhonePermission).setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {
                openSettingsToEnablePermissions("PhoneState");
            }
        });

        findViewById(R.id.btnLocationPermission).setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {
                openSettingsToEnablePermissions("Location");
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d("onRequestPerResult", "onActivityResult: " + requestCode);

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case RequestCodes.openSettingsForPermissions:
            case RequestCodes.requestPermissions: {
                addViewsAndPreload();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("onActivityResult", "onActivityResult: " + requestCode + ", " + resultCode);
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RequestCodes.openSettingsForPermissions:
            case RequestCodes.requestPermissions: {
                addViewsAndPreload();
            }
        }
    }
}