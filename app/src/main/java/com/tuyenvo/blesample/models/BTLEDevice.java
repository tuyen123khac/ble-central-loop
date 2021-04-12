package com.tuyenvo.blesample.models;

import android.bluetooth.BluetoothDevice;

public class BTLEDevice {

    private BluetoothDevice bluetoothDevice;
    private int rssi;

    public BTLEDevice(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
    }

    public String getAddress(){
        return bluetoothDevice.getAddress();
    }

    public String getName(){
        return bluetoothDevice.getName();
    }

    public void setRSSI(int rssi){
        this.rssi = rssi;
    }

    public int getRssi(){
        return rssi;
    }
}
