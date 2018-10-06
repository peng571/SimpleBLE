package org.pengyr.demo.simpleble.ble;

import java.util.UUID;

/**
 * Constants of bluetooth with alchema
 * Created by Peng on 2016/12/2.
 */
public class BleConstants {

    public final static String BROADCAST_BLUETOOTH = "BleConstants.BROADCAST_BLUETOOTH";
    public final static String BLUETOOTH_ACTION = "BluetoothConstants.BLUETOOTH_EXTRA_ACTION";
    public final static String ACTION_DEVICE_NOT_CONNECTED = "BluetoothConstants.ACTION_DEVICE_NOT_CONNECTED";
    public final static String ACTION_DEVICE_CONNECT_SUCCEEDED = "BluetoothConstants.ACTION_DEVICE_CONNECT_SUCCEEDED";


    static final UUID DEVICE_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
    static final UUID WRITE_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");
    static final UUID NOTIFY_UUIT = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");
}

