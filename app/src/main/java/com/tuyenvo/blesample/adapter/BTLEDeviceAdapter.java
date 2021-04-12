package com.tuyenvo.blesample.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.tuyenvo.blesample.R;
import com.tuyenvo.blesample.listeners.ScannedDeviceListener;
import com.tuyenvo.blesample.models.BTLEDevice;

import java.util.List;

public class BTLEDeviceAdapter extends RecyclerView.Adapter<BTLEDeviceAdapter.BTLEDeviceViewHolder> {
    List<BTLEDevice> btleDevices;
    ScannedDeviceListener scannedDeviceListener;

    public BTLEDeviceAdapter(List<BTLEDevice> btleDeviceList, ScannedDeviceListener scannedDeviceListener){
        this.btleDevices = btleDeviceList;
        this.scannedDeviceListener = scannedDeviceListener;
    }

    @NonNull
    @Override
    public BTLEDeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new BTLEDeviceViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_scanned_ble_device, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull BTLEDeviceViewHolder holder, int position) {
        holder.bindDeviceInfo(btleDevices.get(position));
    }

    @Override
    public int getItemCount() {
        return btleDevices.size();
    }

    class BTLEDeviceViewHolder extends RecyclerView.ViewHolder{

        private TextView deviceName, deviceAddr, deviceRssi;

        public BTLEDeviceViewHolder(View itemView){
            super(itemView);
            deviceName = itemView.findViewById(R.id.deviceName);
            deviceAddr = itemView.findViewById(R.id.deviceAddress);
            deviceRssi = itemView.findViewById(R.id.deviceRssi);
        }

        void bindDeviceInfo(BTLEDevice btleDevice){
            deviceName.setText(btleDevice.getName());
            deviceAddr.setText(btleDevice.getAddress());
            deviceRssi.setText(Integer.toString(btleDevice.getRssi()));

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    scannedDeviceListener.onScannedDeviceClicked(btleDevice);
                }
            });
        }
    }
}
