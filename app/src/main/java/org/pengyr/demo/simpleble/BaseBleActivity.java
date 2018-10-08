package org.pengyr.demo.simpleble;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import org.pengyr.demo.simpleble.ble.BlePermissionUtil;
import org.pengyr.demo.simpleble.ble.BleService;
import org.pengyr.demo.simpleble.ble.LocationPermissionUtil;

import static android.os.Build.VERSION_CODES.M;
import static org.pengyr.demo.simpleble.ble.BleConstants.ACTION_DEVICE_CONNECT_SUCCEEDED;
import static org.pengyr.demo.simpleble.ble.BleConstants.ACTION_DEVICE_NOT_CONNECTED;
import static org.pengyr.demo.simpleble.ble.BleConstants.BLUETOOTH_ACTION;
import static org.pengyr.demo.simpleble.ble.BleConstants.BROADCAST_BLUETOOTH;
import static org.pengyr.demo.simpleble.ble.BlePermissionUtil.REQUEST_ENABLE_BT;
import static org.pengyr.demo.simpleble.ble.LocationPermissionUtil.ENABLE_REQUEST_COARSE_LOCATION;

/**
 * Base Activity with BLE service,
 * to check location & bluetooth permission,
 * will start connecting when all permission is ready.
 */
public abstract class BaseBleActivity extends AppCompatActivity {

    private final static String TAG = "BaseBleActivity";

    protected BleService bluetoothService;
    private boolean serviceBound = false;

    protected boolean locationPermissionAllow = false;
    protected boolean locationAllow = false;
    protected boolean bluetoothAllow = false;

    protected boolean isAskingPromission = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Toast.makeText(this, R.string.ble_not_support, Toast.LENGTH_LONG).show();
            return;
        }

        // Bind to LocalService
        Intent intent = new Intent(this, BleService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
        registerReceiver(gattUpdateReceiver, new IntentFilter(BROADCAST_BLUETOOTH));

        // android skd > 6.0 must check location when using bluetooth
        if (Build.VERSION.SDK_INT >= M) {
            checkLocationEnable();
        }
        checkBleEnable();
    }


    @Override
    protected void onResume() {
        super.onResume();

        locationAllow = isLocationEnable();
        bluetoothAllow = isBleEnable();
    }


    @Override
    protected void onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            unregisterReceiver(gattUpdateReceiver);
            // disconnect device
            if (bluetoothService != null) {
                bluetoothService.disconnectDevice();
                bluetoothService = null;
            }

            // Unbind service
            if (serviceBound) {
                unbindService(connection);
                serviceBound = false;
            }
        }
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        onBleEnableResult(requestCode, resultCode);
        onLocationEnableResult(requestCode, resultCode);
        super.onActivityResult(requestCode, resultCode, data);
    }


    /**
     * Permission methods
     */
    protected synchronized boolean isBleEnable() {
        boolean enable = BlePermissionUtil.isBluetoothEnable();
        if (enable) {
            onBleEnableResult(REQUEST_ENABLE_BT, RESULT_OK);
        }
        return enable;
    }

    protected synchronized void checkBleEnable() {
        if (isAskingPromission) return;
        isAskingPromission = true;

        if (!isBleEnable()) {
            BlePermissionUtil.enableBluetooth(this);
        }
    }

    protected synchronized boolean isLocationEnable() {
        if (Build.VERSION.SDK_INT >= M) {
            locationPermissionAllow = LocationPermissionUtil.isLocationPermissionEnable(this);
            locationAllow = LocationPermissionUtil.isLocationEnable(this);
            if (locationPermissionAllow && locationAllow) {
                onLocationEnableResult(ENABLE_REQUEST_COARSE_LOCATION, RESULT_OK);
            }
            return locationAllow && locationPermissionAllow;
        }
        return true;
    }

    // android skd > 6.0 must check location when using bluetooth
    protected synchronized void checkLocationEnable() {
        if (isAskingPromission) return;
        isAskingPromission = true;

        if (Build.VERSION.SDK_INT >= M) {
            locationPermissionAllow = LocationPermissionUtil.isLocationPermissionEnable(this);
            if (locationPermissionAllow) {
                locationAllow = LocationPermissionUtil.checkLocationEnable(this);
            } else {
                locationPermissionAllow = LocationPermissionUtil.checkLocationPermission(this);
            }
        } else {
            locationAllow = true;
            locationPermissionAllow = true;
            onLocationEnableResult(ENABLE_REQUEST_COARSE_LOCATION, RESULT_OK);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        isAskingPromission = false;
        onLocationPermissionEnableResult(requestCode, grantResults[0]);
    }


    /**
     * On Permission Result
     */
    protected void onLocationPermissionEnableResult(int requestCode, int resultCode) {
        isAskingPromission = false;
        if (requestCode != LocationPermissionUtil.PERMISSION_REQUEST_COARSE_LOCATION) return;
        if (resultCode != PackageManager.PERMISSION_GRANTED) {
            locationPermissionAllow = false;
            finish();
            return;
        }
        locationAllow = LocationPermissionUtil.setLocationEnable(this);
        startConnectBluetooth();
    }

    protected void onBleEnableResult(int requestCode, int resultCode) {
        isAskingPromission = false;
        if (requestCode != REQUEST_ENABLE_BT) return;
        if (resultCode == Activity.RESULT_CANCELED) {
            //Bluetooth not enabled.
            bluetoothAllow = false;
            finish();
            return;
        }
        bluetoothAllow = true;
        startConnectBluetooth();
    }

    protected void onLocationEnableResult(int requestCode, int resultCode) {
        isAskingPromission = false;

        if (requestCode != ENABLE_REQUEST_COARSE_LOCATION) return;
        locationAllow = LocationPermissionUtil.checkLocationEnable(this);
        startConnectBluetooth();
    }


    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've serviceBound to LocalService, cast the IBinder and get LocalService instance
            BleService.BluetoothBinder binder = (BleService.BluetoothBinder) service;
            bluetoothService = binder.getService();
            serviceBound = true;
            startConnectBluetooth();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            serviceBound = false;
        }
    };

    /**
     * Check all permission enable to start bluetooth scan
     */
    protected void startConnectBluetooth() {
        if (!locationAllow) {
            checkLocationEnable();
            return;
        }
        if (!bluetoothAllow) {
            checkBleEnable();
            return;
        }
        if (!serviceBound) {
            Intent intent = new Intent(this, BleService.class);
            bindService(intent, connection, Context.BIND_AUTO_CREATE);
            return;
        }

        if (bluetoothService.isConnectSucceeded()) {
            onConnectOn();
        } else {
            bluetoothService.startConnectDevice();
        }
    }


    /**
     * Handles various events fired by the Service.
     */
    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;
            final String action = intent.getStringExtra(BLUETOOTH_ACTION);
            Log.v(TAG, "on broadcast receive action : " + action);
            switch (action) {
                case ACTION_DEVICE_CONNECT_SUCCEEDED:
                    onConnectOn();
                    break;

                case ACTION_DEVICE_NOT_CONNECTED:
                    onConnectOff();
                    break;
            }
        }
    };


    protected abstract void onConnectOn();

    protected abstract void onConnectOff();

}