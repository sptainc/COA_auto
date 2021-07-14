package com.callcenter.ftcjsc;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import android.util.Log;
import android.view.View;

import com.callcenter.ftcjsc.utils.Constants;
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
        final int granted = PackageManager.PERMISSION_GRANTED;
        final String readPhoneState = Manifest.permission.READ_PHONE_STATE;
//        final String fineLocation = Manifest.permission.ACCESS_FINE_LOCATION;
        boolean phoneGranted = false;
//        boolean locationGranted = false;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
             phoneGranted = checkSelfPermission(readPhoneState) == granted;
//             locationGranted = checkSelfPermission(fineLocation) == granted;
        }else {
            phoneGranted = true;
//            locationGranted = true;
        }

        (findViewById(R.id.btnPhonePermission)).setVisibility(phoneGranted ? View.GONE : View.VISIBLE);
        (findViewById(R.id.imgPhonePermission)).setVisibility(phoneGranted ? View.VISIBLE : View.GONE);

//        (findViewById(R.id.btnLocationPermission)).setVisibility(locationGranted ? View.GONE : View.VISIBLE);
//        (findViewById(R.id.imgLocationPermission)).setVisibility(locationGranted ? View.VISIBLE : View.GONE);

        if(phoneGranted) {
            Utils.updateConstants(this);
//            Location location = ((LocationManager) getSystemService(LOCATION_SERVICE)).getLastKnownLocation(LocationManager.GPS_PROVIDER);
//            Constants.setLocation(((LocationManager) getSystemService(LOCATION_SERVICE)).getLastKnownLocation(LocationManager.GPS_PROVIDER));
            Intent i = new Intent(this, HomeActivity.class);
            startActivity(i);
            finish();
        }
    }

    private void addListeners() {
        final int denied = PackageManager.PERMISSION_DENIED;
        final String readPhoneState = Manifest.permission.READ_PHONE_STATE;
//        final String fineLocation = Manifest.permission.ACCESS_FINE_LOCATION;

        findViewById(R.id.btnPhonePermission).setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {
                if(checkSelfPermission(readPhoneState) == denied && !shouldShowRequestPermissionRationale(readPhoneState)) {
                    openSettingsToEnablePermissions("PhoneState");
                }else {
                    requestPermissions(new String[]{
                            readPhoneState
                    }, RequestCodes.requestPermissions);
                }
            }
        });

//        findViewById(R.id.btnLocationPermission).setOnClickListener(new View.OnClickListener() {
//            @RequiresApi(api = Build.VERSION_CODES.M)
//            @Override
//            public void onClick(View v) {
//                if(checkSelfPermission(fineLocation) == denied && !shouldShowRequestPermissionRationale(fineLocation)) {
//                    openSettingsToEnablePermissions("Location");
//                }else {
//                    requestPermissions(new String[]{
//                            fineLocation
//                    }, RequestCodes.requestPermissions);
//                }
//            }
//        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
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
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RequestCodes.openSettingsForPermissions:
            case RequestCodes.requestPermissions: {
                addViewsAndPreload();
            }
        }
    }
}