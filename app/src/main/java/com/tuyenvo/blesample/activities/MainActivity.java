package com.tuyenvo.blesample.activities;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.tuyenvo.blesample.R;
import com.tuyenvo.blesample.adapter.BTLEDeviceAdapter;
import com.tuyenvo.blesample.listeners.ScannedDeviceListener;
import com.tuyenvo.blesample.models.BTLEDevice;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements ScannedDeviceListener {

    public static final String TAG = "MainActivity";
    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;
    public static final String BLE_ADVERTISE_MESSAGE = "BLE";

    private BluetoothAdapter mBluetoothAdapter;
    private int REQUEST_ENABLE_BT = 1;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 5000;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private BluetoothGatt mGatt;
    Boolean mConnected, mInitialized;
    RecyclerView listItemRecyclerView;
    TextView connectedDeviceName, connectedDeviceAddress;
    private BTLEDeviceAdapter adapter;
    private Map<String, BTLEDevice> mBTDevicesMap;
    private List<BTLEDevice> mBTDeviceList;
    private List<BluetoothDevice> rawBTDeviceList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listItemRecyclerView = findViewById(R.id.listItemRecyclerView);
        connectedDeviceName = findViewById(R.id.connectedDeviceName);
        connectedDeviceAddress = findViewById(R.id.connectedDeviceAddress);

        mConnected = false;
        mInitialized = false;
        mBTDeviceList = new ArrayList<>();
        mBTDevicesMap = new HashMap<>();
        rawBTDeviceList = new ArrayList<>();
        adapter = new BTLEDeviceAdapter(mBTDeviceList, this);
        listItemRecyclerView.setAdapter(adapter);

        mHandler = new Handler();
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE Not Supported",
                    Toast.LENGTH_SHORT).show();
            finish();
        }
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        checkLocationPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        /*startScan();

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendMessageToServer();
            }
        }, 10000);*/

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                sendMessageToServer();
                mHandler.postDelayed(this, 10000);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                startScan();
            }
        };

        mHandler.postDelayed(runnable, 10000);
    }

    public void startScan() {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            if (Build.VERSION.SDK_INT >= 21) {
                mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
                settings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build();
                filters = new ArrayList<ScanFilter>();
                ScanFilter filter = new ScanFilter.Builder()
                        .setServiceUuid(new ParcelUuid(UUID.fromString(getString(R.string.ble_uuid))))
                        //.setServiceData(new ParcelUuid(UUID.fromString(getString(R.string.ble_uuid))), BLE_ADVERTISE_MESSAGE.getBytes(Charset.forName("UTF-8")))
                        // Not able to add service data because the byte range is limited
                        .build();
                filters.add(filter);
            }
            scanLeDevice(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            scanLeDevice(false);
        }
    }

    @Override
    protected void onDestroy() {
        if (mGatt == null) {
            return;
        }
        mGatt.disconnect();
        mGatt.close();
        mGatt = null;
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                //Bluetooth not enabled.
                finish();
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            Log.d(TAG, "run: Starting BLE Scan...");
            Toast.makeText(MainActivity.this, "Starting BLE Scan...", Toast.LENGTH_SHORT).show();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT < 21) {
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    } else {
                        mLEScanner.stopScan(mScanCallback);
                    }
                    Toast.makeText(MainActivity.this, "Scanned", Toast.LENGTH_SHORT).show();
                }
            }, SCAN_PERIOD);
            if (Build.VERSION.SDK_INT < 21) {
                mBluetoothAdapter.startLeScan(mLeScanCallback);
            } else {
                mLEScanner.startScan(filters, settings, mScanCallback);
            }
        } else {
            if (Build.VERSION.SDK_INT < 21) {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            } else {
                mLEScanner.stopScan(mScanCallback);
            }
        }
    }

    private ScanCallback mScanCallback = new ScanCallback() {

        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.d("callbackType", String.valueOf(callbackType));
            Log.d("result", result.toString());
            BluetoothDevice btDevice = result.getDevice();
            Log.d(TAG, "onScanResult, run: add device: " + btDevice.getName() + ", " + btDevice.getAddress());
            addDevice(btDevice, result.getRssi());
            rawBTDeviceList.add(btDevice);
            connectToDevice(mBTDeviceList.get(0));
            //connectToDevice(btDevice);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                Log.d("ScanResult - Results", sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("Scan Failed", "Error Code: " + errorCode);
        }
    };

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @RequiresApi(api = Build.VERSION_CODES.M)
                        @Override
                        public void run() {
                            Log.d(TAG, "onLeScan, run: add device: " + device.getName() + ", " + device.getAddress());
                            addDevice(device, rssi);
                            rawBTDeviceList.add(device);
                            connectToDevice(mBTDeviceList.get(0));
                            //connectToDevice(device);
                        }
                    });
                }
            };

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void connectToDevice(BluetoothDevice device) {
        if (mGatt == null) {
            mGatt = device.connectGatt(this, false, gattCallback);
            Log.d(TAG, "connectToDevice: called connectGatt");
            scanLeDevice(false);// will stop after first device detection
        }
    }

    public void addDevice(BluetoothDevice device, int newRssi) {
        String address = device.getAddress();

        if (!mBTDevicesMap.containsKey(address)) {
            BTLEDevice btleDevice = new BTLEDevice(device);
            btleDevice.setRSSI(newRssi);

            mBTDevicesMap.put(address, btleDevice);
            mBTDeviceList.add(btleDevice);
        } else {
            mBTDevicesMap.get(address).setRSSI(newRssi);
        }

        adapter.notifyDataSetChanged();
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d("onConnectionStateChange", "Status: " + status);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.d("gattCallback", "STATE_CONNECTED");
                    mConnected = true;
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e("gattCallback", "STATE_DISCONNECTED");
                    break;
                default:
                    Log.e("gattCallback", "STATE_OTHER");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(UUID.fromString(getString(R.string.ble_uuid)));
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(getString(R.string.ble_uuid)));

                characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                mInitialized = gatt.setCharacteristicNotification(characteristic, true);
                Log.d(TAG, "onServicesDiscovered: set notification request");
            }

        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic
                                                 characteristic, int status) {
            Log.d("onCharacterReadDetail", characteristic.getValue().toString());
            //gatt.disconnect();
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.d(TAG, "onCharacteristicWrite: " + characteristic.getValue() + " written");
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            byte[] messageBytes = characteristic.getValue();
            String messageString = null;
            try {
                messageString = new String(messageBytes, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "Unable to convert message bytes to string");
            }
            Log.d(TAG, "onCharacteristicChanged: " + messageString);
        }


    };

    private void checkLocationPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                                android.Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_CODE_ASK_PERMISSIONS);
                return;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("MainActivity ", "onRequestPermissionsResult: Location Permission granted");
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT)
                            .show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onScannedDeviceClicked(BTLEDevice btleDevice) {
        Toast.makeText(this, "Block clicking", Toast.LENGTH_SHORT).show();

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void connectToDevice(BTLEDevice btleDevice) {
        BluetoothDevice tmpDevice = null;
        if (btleDevice.getName() == null) {
            connectedDeviceName.setText("No Name");
        } else {
            connectedDeviceName.setText(btleDevice.getName());
        }

        connectedDeviceAddress.setText(btleDevice.getAddress());

        for (BluetoothDevice device : rawBTDeviceList) {
            if (device.getAddress().equals(btleDevice.getAddress())) {
                tmpDevice = device;
                Log.d(TAG, "onScannedDeviceClicked: Attempt to connect + " + btleDevice.getName());
                break;
            }
        }

        if (tmpDevice != null) {
            connectToDevice(tmpDevice);
        }
    }

    private void sendMessageToServer() {

        if (!mConnected || !mInitialized) {
            Log.e(TAG, "sendMessageToServer: Connection to server was not establish, make sure Peripheral is advertising");
            return;
        }

        Log.d(TAG, "sendMessageToServer: Connection is ready");

        byte[] byteGreen = "GREEN".getBytes(Charset.defaultCharset());
        byte[] byteRed = "RED".getBytes(Charset.defaultCharset());
        BluetoothGattService service = mGatt.getService(UUID.fromString(getString(R.string.ble_uuid)));
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(getString(R.string.ble_uuid)));

        try {
            characteristic.setValue(byteGreen);
            mGatt.writeCharacteristic(characteristic);
            Thread.sleep(1000);
            characteristic.setValue(byteRed);
            mGatt.writeCharacteristic(characteristic);
            Thread.sleep(1000);
            mGatt.disconnect();
            mGatt.close();
            mGatt = null;
            mConnected = false;
            mInitialized = false;
            Log.d(TAG, "sendMessageToServer: Closed GATT");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

}