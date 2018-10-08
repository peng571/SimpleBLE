package org.pengyr.demo.simpleble.ble;

import android.annotation.TargetApi;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTING;
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTED;
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTING;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.os.Build.VERSION_CODES.M;


/**
 * Service to connect to BLE device as client.
 * Use when android os version >= LOLLIPOP (21)
 */

@TargetApi(LOLLIPOP)
public class BleService extends Service {

    private final static String TAG = "BleService";

    private static final long CONNECT_TIMEOUT_MS = 30000;
    private long startConnectTimestamp = 0;

    private boolean isAlive;  // service alive
    private final IBinder binder = new BluetoothBinder();
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattService bluetoothGattService;
    private BluetoothGattCharacteristic bluetoothWriteCharacteristic;
    private BluetoothGattCharacteristic bluetoothNotifyCharacteristic;
    static BluetoothAdapter bluetoothAdapter;

    private static Handler backgroundThread;
    private int connectState = STATE_DISCONNECTED;

    @Override
    public void onCreate() {
        BluetoothManager bleManager = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE));
        if (bleManager == null || bleManager.getAdapter() == null) return;
        bluetoothAdapter = bleManager.getAdapter();
        backgroundThread = new Handler();
        connectState = STATE_DISCONNECTED;
        isAlive = true;
    }

    @Override
    public void onDestroy() {
        stopScan();
        disconnectDevice();
        isAlive = false;
        super.onDestroy();
    }

    public boolean isConnectSucceeded() {
        return connectState == STATE_CONNECTED;
    }

    /**
     * Main Entry to start connect device
     */
    public void startConnectDevice() {
        if (!isAlive) return;
        if (!bluetoothAdapter.isEnabled()) return;
        if (isConnectSucceeded()) return;
        startConnectTimestamp = System.currentTimeMillis();
        backgroundThread.postDelayed(scanTimeoutCallback, CONNECT_TIMEOUT_MS + 20);
        connectState = STATE_CONNECTING;
        startScan();
    }

    /**
     * Main Entry to start disconnect device
     */
    public void disconnectDevice() {
        if (!isAlive) return;
        connectState = STATE_DISCONNECTING;
        bluetoothGattService = null;
        bluetoothNotifyCharacteristic = null;
        bluetoothWriteCharacteristic = null;
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
    }


    private Runnable scanTimeoutCallback = new Runnable() {
        @Override
        public void run() {
            if (!isAlive) return;
            if (isConnectSucceeded()) return;

            // ignore if not in this time of scan
            if (System.currentTimeMillis() - startConnectTimestamp < CONNECT_TIMEOUT_MS) return;
            restartScan();
        }
    };


    /**
     * scan device
     */
    private synchronized void startScan() {
        if (!isAlive) return;
        if (bluetoothAdapter == null) return;
        BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();

        // call stopScan to reset scan callback
        scanner.stopScan(scanCallback);

        // scan with UUID filter
        ArrayList<ScanFilter> filters = new ArrayList<>();
        filters.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(BleConstants.DEVICE_UUID)).build());
        ScanSettings.Builder settingBuilder = new ScanSettings.Builder();
        if (Build.VERSION.SDK_INT > M) {
            settingBuilder.setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH);
        }
        settingBuilder.setScanMode(ScanSettings.SCAN_MODE_BALANCED);
        scanner.startScan(filters, settingBuilder.build(), scanCallback);
    }

    /**
     * restart scan when disconnect or failed
     */
    private void restartScan() {
        disconnectDevice();
        startScan();
    }

    private synchronized void stopScan() {
        if (!isAlive) return;
        if (bluetoothAdapter == null) return;
        if (!bluetoothAdapter.isEnabled()) return;
        BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();
        if (scanner != null) scanner.stopScan(scanCallback);
    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (!isAlive) return;

            BluetoothDevice device = result.getDevice();
            Log.v(TAG, "onScan Success " + device.toString());
            Log.v(TAG, "Advertisement: Device name: " + device.getName() + ", address: " + device.getAddress());
            stopScan();
            connectToDevice(device);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            restartScan();
        }
    };


    /**
     * connect device
     */
    private void connectToDevice(@NonNull final BluetoothDevice device) {
        if (!isAlive) return;
        bluetoothGatt = device.connectGatt(this, false, gattCallback);
    }


    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        @Override
        public synchronized void onConnectionStateChange(final BluetoothGatt gatt, int status, int newState) {
            Log.v(TAG, "onConnectionStateChange " + status + ", " + newState);
            if (!isAlive) return;
            if (status != GATT_SUCCESS) {
                // get gatt status not success
                broadcastUpdate(BleConstants.ACTION_DEVICE_NOT_CONNECTED);
                restartScan();
                return;
            }

            switch (newState) {
                case STATE_CONNECTED:
                    if (gatt == null) return;
                    // Get GATT server, start to discover and register services
                    bluetoothGatt = gatt;
                    bluetoothGatt.discoverServices();
                    break;
                case STATE_DISCONNECTED:
                    // Disconnected from GATT server.
                    connectState = STATE_DISCONNECTED;
                    broadcastUpdate(BleConstants.ACTION_DEVICE_NOT_CONNECTED);
                    restartScan();
                    break;
            }
        }

        /**
         * Find Gatt service & Register Characteristic
         * and register Write Characteristic & Notify Characteristic
         * broadcast ACTION_DEVICE_READY if success
         */
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (!isAlive) return;
            // check status
            if (status != BluetoothGatt.GATT_SUCCESS) {
                restartScan();
                return;
            }

            // check service
            bluetoothGattService = gatt.getService(BleConstants.DEVICE_UUID);
            if (bluetoothGattService == null) {
                restartScan();
                return;
            }

            // check write characteristic
            bluetoothWriteCharacteristic = bluetoothGattService.getCharacteristic(BleConstants.WRITE_UUID);
            if (bluetoothWriteCharacteristic == null) {
                restartScan();
                return;
            }

            // check notify characteristic
            bluetoothNotifyCharacteristic = bluetoothGattService.getCharacteristic(BleConstants.NOTIFY_UUIT);
            if (bluetoothNotifyCharacteristic == null) {
                restartScan();
                return;
            }

            bluetoothGatt.setCharacteristicNotification(bluetoothNotifyCharacteristic, true);
            connectState = STATE_CONNECTED;
            broadcastUpdate(BleConstants.ACTION_DEVICE_CONNECT_SUCCEEDED);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic c, int status) {
            getNotify(c);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic c) {
            getNotify(c);
        }

        /**
         * on bluetooth result
         * @param c c.getValue() to get byte[]
         */

        private synchronized void getNotify(BluetoothGattCharacteristic c) {
            if (!isAlive) return;
            if (BleConstants.NOTIFY_UUIT.toString().equals(c.getUuid().toString())) {
                // do when get notify from device
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic c, int status) { }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {}
    };


    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }


    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class BluetoothBinder extends Binder {
        public BleService getService() {
            // Return this instance of LocalService so clients can call public methods
            return BleService.this;
        }

    }


    /**
     * call when gatt status change
     *
     * @param action extra_action
     */
    private void broadcastUpdate(final String action) {
        sendBroadcast(new Intent(BleConstants.BROADCAST_BLUETOOTH).putExtra(BleConstants.BLUETOOTH_ACTION, action));
    }

}