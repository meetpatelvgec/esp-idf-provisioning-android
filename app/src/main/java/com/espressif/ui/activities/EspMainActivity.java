// Copyright 2020 Espressif Systems (Shanghai) PTE LTD
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.espressif.ui.activities;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.espressif.AppConstants;
import com.espressif.provisioning.ESPConstants;
import com.espressif.provisioning.ESPProvisionManager;
import com.espressif.wifi_provisioning.BuildConfig;
import com.espressif.wifi_provisioning.R;

public class EspMainActivity extends AppCompatActivity {

    private static final String TAG = EspMainActivity.class.getSimpleName();

    // Request codes
    private static final int REQUEST_LOCATION = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    private ESPProvisionManager provisionManager;
    private CardView btnAddDevice;
    private ImageView ivEsp;
    private SharedPreferences sharedPreferences;
    private String deviceType;



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_esp_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        initViews();

        sharedPreferences = getSharedPreferences(AppConstants.ESP_PREFERENCES, Context.MODE_PRIVATE);
        provisionManager = ESPProvisionManager.getInstance(getApplicationContext());
    }

    @Override
    protected void onResume() {
        super.onResume();

        deviceType = sharedPreferences.getString(AppConstants.KEY_DEVICE_TYPES, AppConstants.DEVICE_TYPE_DEFAULT);
        if (deviceType.equals("wifi")) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(AppConstants.KEY_DEVICE_TYPES, AppConstants.DEVICE_TYPE_DEFAULT);
            editor.apply();
        }

        deviceType = sharedPreferences.getString(AppConstants.KEY_DEVICE_TYPES, AppConstants.DEVICE_TYPE_DEFAULT);
        if (deviceType.equals(AppConstants.DEVICE_TYPE_BLE)) {
            ivEsp.setImageResource(R.drawable.ic_esp_ble);
        } else if (deviceType.equals(AppConstants.DEVICE_TYPE_SOFTAP)) {
            ivEsp.setImageResource(R.drawable.ic_esp_softap);
        } else {
            ivEsp.setImageResource(R.drawable.ic_esp);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        if (BuildConfig.isSettingsAllowed) {
            // Inflate the menu; this adds items to the action bar if it is present.
            getMenuInflater().inflate(R.menu.menu_settings, menu);
            return true;
        } else {
            menu.clear();
            return true;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_LOCATION) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {

                if (isLocationEnabled()) {
                    addDeviceClick();
                }
            }
        }

        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            Toast.makeText(this, "Bluetooth is turned ON, you can provision device now.", Toast.LENGTH_LONG).show();
        }
    }

    private void initViews() {

        ivEsp = findViewById(R.id.iv_esp);
        btnAddDevice = findViewById(R.id.btn_provision_device);
        btnAddDevice.findViewById(R.id.iv_arrow).setVisibility(View.GONE);
        btnAddDevice.setOnClickListener(addDeviceBtnClickListener);

        TextView tvAppVersion = findViewById(R.id.tv_app_version);

        String version = "";
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        String appVersion = getString(R.string.app_version) + " - v" + version;
        tvAppVersion.setText(appVersion);
    }

    View.OnClickListener addDeviceBtnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {

                if (!isLocationEnabled()) {
                    askForLocation();
                    return;
                }
            }
            addDeviceClick();
        }
    };

    private void addDeviceClick() {
        String customData, readData;
        EditText et_usr_email = (EditText) findViewById(R.id.et_usr_email);
        EditText etDevice1Name = (EditText) findViewById(R.id.et_usr_device1_name);
        EditText etDevice1Description = (EditText) findViewById(R.id.et_usr_device1_description);
        EditText etDevice2Name = (EditText) findViewById(R.id.et_usr_device2_name);
        EditText etDevice2Description = (EditText) findViewById(R.id.et_usr_device2_description);
        EditText etDevice3Name = (EditText) findViewById(R.id.et_usr_device3_name);
        EditText etDevice3Description = (EditText) findViewById(R.id.et_usr_device3_description);
        EditText etDevice4Name = (EditText) findViewById(R.id.et_usr_device4_name);
        EditText etDevice4Description = (EditText) findViewById(R.id.et_usr_device4_description);
        Spinner spnDevice1 = (Spinner) findViewById(R.id.spn_usr_device1_type);
        Spinner spnDevice2 = (Spinner) findViewById(R.id.spn_usr_device2_type);
        Spinner spnDevice3 = (Spinner) findViewById(R.id.spn_usr_device3_type);
        Spinner spnDevice4 = (Spinner) findViewById(R.id.spn_usr_device4_type);
        SwitchCompat swcDevice1 = (SwitchCompat) findViewById(R.id.swc_usr_device1_dimmable);
        SwitchCompat swcDevice2 = (SwitchCompat) findViewById(R.id.swc_usr_device2_dimmable);
        SwitchCompat swcDevice3 = (SwitchCompat) findViewById(R.id.swc_usr_device3_dimmable);
        SwitchCompat swcDevice4 = (SwitchCompat) findViewById(R.id.swc_usr_device4_dimmable);

        /* Validating the Data */
        if (TextUtils.isEmpty(et_usr_email.getText())) {
            Toast.makeText(this, "Please provide Email ID", Toast.LENGTH_SHORT).show();
            return;
        } else if (
                TextUtils.isEmpty(etDevice1Name.getText()) &&
                TextUtils.isEmpty(etDevice2Name.getText()) &&
                TextUtils.isEmpty(etDevice3Name.getText()) &&
                TextUtils.isEmpty(etDevice4Name.getText())
        ) {
            Toast.makeText(this, "At least one device name is needed", Toast.LENGTH_SHORT).show();
            return;
        }

        int d1max = spnDevice1.getCount(), d1Index = spnDevice1.getSelectedItemPosition();
        int d2max = spnDevice2.getCount(), d2Index = spnDevice2.getSelectedItemPosition();
        int d3max = spnDevice3.getCount(), d3Index = spnDevice3.getSelectedItemPosition();
        int d4max = spnDevice4.getCount(), d4Index = spnDevice4.getSelectedItemPosition();
        String d1CategoryBinary = Integer.toBinaryString(((d1Index < (d1max - 1)) ? d1Index : 15));
        String d2CategoryBinary = Integer.toBinaryString(((d2Index < (d2max - 1)) ? d2Index : 15));
        String d3CategoryBinary = Integer.toBinaryString(((d3Index < (d3max - 1)) ? d3Index : 15));
        String d4CategoryBinary = Integer.toBinaryString(((d4Index < (d4max - 1)) ? d4Index : 15));

        String d1Parameters = "", d2Parameters = "", d3Parameters = "", d4Parameters = "";
        if (!TextUtils.isEmpty(etDevice1Name.getText())) {
            d1Parameters =  "d1name=" + etDevice1Name.getText().toString() + "&" +
                            "d1category=" + d1CategoryBinary + "&" +
                            "d1dimmable=" + (swcDevice1.isChecked() ? "1" : "0") + "&" +
                            "d1description=" + etDevice1Description.getText().toString() + "&";
        }
        if (!TextUtils.isEmpty(etDevice2Name.getText())) {
            d2Parameters =  "d2name=" + etDevice2Name.getText().toString() + "&" +
                            "d2category=" + d2CategoryBinary + "&" +
                            "d2dimmable=" + (swcDevice2.isChecked() ? "1" : "0") + "&" +
                            "d2description=" + etDevice2Description.getText().toString() + "&";
        }
        if (!TextUtils.isEmpty(etDevice3Name.getText())) {
            d3Parameters =  "d3name=" + etDevice3Name.getText().toString() + "&" +
                            "d3category=" + d3CategoryBinary + "&" +
                            "d3dimmable=" + (swcDevice3.isChecked() ? "1" : "0") + "&" +
                            "d3description=" + etDevice3Description.getText().toString() + "&";
        }
        if (!TextUtils.isEmpty(etDevice4Name.getText())) {
            d4Parameters =  "d4name=" + etDevice4Name.getText().toString() + "&" +
                            "d4category=" + d4CategoryBinary + "&" +
                            "d4dimmable=" + (swcDevice4.isChecked() ? "1" : "0") + "&" +
                            "d4description=" + etDevice4Description.getText().toString() + "&";
        }

        String emailParameter = "email=" + et_usr_email.getText().toString() + "&";

        customData = emailParameter + d1Parameters + d2Parameters + d3Parameters + d4Parameters;

        customData = customData.replaceAll(" ", "%20");
        customData = customData.replaceAll("@", "%40");

        int startIndex = 0, pendingToSend;
        pendingToSend = customData.length();

        while (pendingToSend > 0) {
            String partial_custom_data = customData.substring(startIndex, (pendingToSend > 128 ? startIndex + 128: startIndex + pendingToSend));
            Toast.makeText(this, partial_custom_data, Toast.LENGTH_LONG).show();
            startIndex += 128;
            pendingToSend -= 128;
        }



        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(AppConstants.KEY_CUSTOM_DATA, customData);
        editor.apply();

        readData = sharedPreferences.getString(AppConstants.KEY_CUSTOM_DATA, "");

        if (BuildConfig.isQrCodeSupported) {

            gotoQrCodeActivity();

        } else {

            if (deviceType.equals(AppConstants.DEVICE_TYPE_BLE) || deviceType.equals(AppConstants.DEVICE_TYPE_BOTH)) {

                final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                BluetoothAdapter bleAdapter = bluetoothManager.getAdapter();

                if (!bleAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                } else {
                    startProvisioningFlow();
                }
            } else {
                startProvisioningFlow();
            }
        }
    }

    private void startProvisioningFlow() {

        deviceType = sharedPreferences.getString(AppConstants.KEY_DEVICE_TYPES, AppConstants.DEVICE_TYPE_DEFAULT);
        final boolean isSec1 = sharedPreferences.getBoolean(AppConstants.KEY_SECURITY_TYPE, true);
        Log.d(TAG, "Device Types : " + deviceType);
        Log.d(TAG, "isSec1 : " + isSec1);
        int securityType = 0;
        if (isSec1) {
            securityType = 1;
        }

        if (deviceType.equals(AppConstants.DEVICE_TYPE_BLE)) {

            if (isSec1) {
                provisionManager.createESPDevice(ESPConstants.TransportType.TRANSPORT_BLE, ESPConstants.SecurityType.SECURITY_1);
            } else {
                provisionManager.createESPDevice(ESPConstants.TransportType.TRANSPORT_BLE, ESPConstants.SecurityType.SECURITY_0);
            }
            goToBLEProvisionLandingActivity(securityType);

        } else if (deviceType.equals(AppConstants.DEVICE_TYPE_SOFTAP)) {

            if (isSec1) {
                provisionManager.createESPDevice(ESPConstants.TransportType.TRANSPORT_SOFTAP, ESPConstants.SecurityType.SECURITY_1);
            } else {
                provisionManager.createESPDevice(ESPConstants.TransportType.TRANSPORT_SOFTAP, ESPConstants.SecurityType.SECURITY_0);
            }
            goToWiFiProvisionLandingActivity(securityType);

        } else {

            final String[] deviceTypes = {"BLE", "SoftAP"};
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(true);
            builder.setTitle(R.string.dialog_msg_device_selection);
            final int finalSecurityType = securityType;
            builder.setItems(deviceTypes, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int position) {

                    switch (position) {
                        case 0:

                            if (isSec1) {
                                provisionManager.createESPDevice(ESPConstants.TransportType.TRANSPORT_BLE, ESPConstants.SecurityType.SECURITY_1);
                            } else {
                                provisionManager.createESPDevice(ESPConstants.TransportType.TRANSPORT_BLE, ESPConstants.SecurityType.SECURITY_0);
                            }
                            goToBLEProvisionLandingActivity(finalSecurityType);
                            break;

                        case 1:

                            if (isSec1) {
                                provisionManager.createESPDevice(ESPConstants.TransportType.TRANSPORT_SOFTAP, ESPConstants.SecurityType.SECURITY_1);
                            } else {
                                provisionManager.createESPDevice(ESPConstants.TransportType.TRANSPORT_SOFTAP, ESPConstants.SecurityType.SECURITY_0);
                            }
                            goToWiFiProvisionLandingActivity(finalSecurityType);
                            break;
                    }
                    dialog.dismiss();
                }
            });
            builder.show();
        }
    }

    private void askForLocation() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setMessage(R.string.dialog_msg_gps);

        // Set up the buttons
        builder.setPositiveButton(R.string.btn_ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), REQUEST_LOCATION);
            }
        });

        builder.setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private boolean isLocationEnabled() {

        boolean gps_enabled = false;
        boolean network_enabled = false;
        LocationManager lm = (LocationManager) getApplicationContext().getSystemService(Activity.LOCATION_SERVICE);

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
        }

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
        }

        Log.d(TAG, "GPS Enabled : " + gps_enabled + " , Network Enabled : " + network_enabled);

        boolean result = gps_enabled || network_enabled;
        return result;
    }

    private void gotoQrCodeActivity() {
        Intent intent = new Intent(EspMainActivity.this, AddDeviceActivity.class);
        startActivity(intent);
    }

    private void goToBLEProvisionLandingActivity(int securityType) {

        Intent intent = new Intent(EspMainActivity.this, BLEProvisionLanding.class);
        intent.putExtra(AppConstants.KEY_SECURITY_TYPE, securityType);
        startActivity(intent);
    }

    private void goToWiFiProvisionLandingActivity(int securityType) {

        Intent intent = new Intent(EspMainActivity.this, ProvisionLanding.class);
        intent.putExtra(AppConstants.KEY_SECURITY_TYPE, securityType);
        startActivity(intent);
    }
}
