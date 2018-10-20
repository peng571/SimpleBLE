package org.pengyr.demo.simpleble.ble;

import java.util.UUID;

public class BleConstants {

    public final static String BROADCAST_BLUETOOTH = "BleConstants.BROADCAST_BLUETOOTH";
    public final static String BLUETOOTH_ACTION = "BluetoothConstants.BLUETOOTH_EXTRA_ACTION";
    public final static String ACTION_DEVICE_NOT_CONNECTED = "BluetoothConstants.ACTION_DEVICE_NOT_CONNECTED";
    public final static String ACTION_DEVICE_CONNECT_SUCCEEDED = "BluetoothConstants.ACTION_DEVICE_CONNECT_SUCCEEDED";

    static final UUID DEVICE_UUID = UUID.fromString("xxx00001-xxxx-xxxx-xxxx-xxxxxxxxxxx");
    static final UUID WRITE_UUID = UUID.fromString("xxx00002-xxxx-xxxx-xxxx-xxxxxxxxxxx");
    static final UUID NOTIFY_UUIT = UUID.fromString("xxx00003-xxxx-xxxx-xxxx-xxxxxxxxxxx");
}

