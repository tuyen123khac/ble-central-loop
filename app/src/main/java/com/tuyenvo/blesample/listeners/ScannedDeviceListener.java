package com.tuyenvo.blesample.listeners;

import com.tuyenvo.blesample.models.BTLEDevice;

public interface ScannedDeviceListener {
    void onScannedDeviceClicked(BTLEDevice btleDevice);
}
