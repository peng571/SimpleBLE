package org.pengyr.demo.simpleble.ble;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;

import static android.os.Build.VERSION_CODES.LOLLIPOP;


@TargetApi(LOLLIPOP)
public class BlePermissionUtil {

    public static final int REQUEST_ENABLE_BT = 71;

    public static boolean isBluetoothEnable() {
        return BleService.bluetoothAdapter != null && BleService.bluetoothAdapter.isEnabled();
    }

    public static void enableBluetooth(Activity activity) {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

}