package com.example.myfirstapp;

import android.bluetooth.BluetoothDevice;

import cc.noharry.blelib.data.BleDevice;

public class AdvancedBleDevice extends BleDevice {
    boolean isScanned = false;
    boolean isConnected = false;

    public AdvancedBleDevice(BluetoothDevice bluetoothDevice, byte[] scanRecord, int rssi, long timestampNanos) {
        super(bluetoothDevice, scanRecord, rssi, timestampNanos);
    }

    public AdvancedBleDevice(BleDevice bleDevice) {
        super(bleDevice.getBluetoothDevice(),bleDevice.getScanRecord(),bleDevice.getRssi(),bleDevice.getTimestampNanos());
    }

    public AdvancedBleDevice(BleDevice bleDevice,boolean isConnected) {
        super(bleDevice.getBluetoothDevice(),bleDevice.getScanRecord(),bleDevice.getRssi(),bleDevice.getTimestampNanos());
        this.isConnected = isConnected;
    }
}
