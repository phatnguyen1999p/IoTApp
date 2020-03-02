package com.example.healthdevice;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DeviceScanner extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BLE = 1;
    private BluetoothAdapter bluetoothAdapter;
    Handler mHandler;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private boolean mScanning = false;
    private static final long SCAN_PERIOD = 10000;
    private ListView listView;
    private BluetoothLeScanner bluetoothLeScanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_scanner);

        mHandler = new Handler();

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        mLeDeviceListAdapter = new LeDeviceListAdapter();

        listView = findViewById(R.id.device_list);
        listView.setAdapter(mLeDeviceListAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BluetoothDevice bluetoothDeviceSelected = mLeDeviceListAdapter.getDevice(position);

                String str = "Selected:" + bluetoothDeviceSelected.getName();
                Toast.makeText(getApplicationContext(),str,Toast.LENGTH_SHORT).show();

                final Intent intent = new Intent(DeviceScanner.this, DeviceControlActivity.class);
                intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME,bluetoothDeviceSelected.getName());
                intent.putExtra(DeviceControlActivity.EXTRA_DEVICE_ADDRESS,bluetoothDeviceSelected.getAddress());
                if (mScanning){
                    scanBLEDevice(false);
                    mScanning = false;
                }
                mLeDeviceListAdapter.clear();
                startActivity(intent);
            }
        });

        getPairedDevice();
    }
    @Override
    protected void onResume(){
        super.onResume();

        if (!bluetoothAdapter.isEnabled()){
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent,REQUEST_ENABLE_BLE);
        }
        listView.setAdapter(mLeDeviceListAdapter);
        scanBLEDevice(true);
        getPairedDevice();
    }
    private void getPairedDevice(){
        Set<BluetoothDevice> bluetoothDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice bluetoothDevice : bluetoothDevices) {
            mLeDeviceListAdapter.addDevice(bluetoothDevice);
        }
    }
    private void scanBLEDevice(final boolean enable){
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    bluetoothLeScanner.stopScan(mLeScanCallBack);
                    Log.d("BLE", "STOP_SCANNING");
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            bluetoothLeScanner.startScan(mLeScanCallBack);
            Log.d("BLE", "START_SCANNING");
        } else {
            mScanning = false;
            bluetoothLeScanner.stopScan(mLeScanCallBack);
        }
        invalidateOptionsMenu();
    }
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;
        public LeDeviceListAdapter(){
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = DeviceScanner.this.getLayoutInflater();
        }
        public void clear(){
            mLeDevices.clear();
        }

        void addDevice(BluetoothDevice device){
            if(!mLeDevices.contains(device)){
                mLeDevices.add(device);
            }
        }
        public BluetoothDevice getDevice(int position){
            return mLeDevices.get(position);
        }
        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int position) {
            return mLeDevices.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;

            if (convertView == null){
                convertView = mInflator.inflate(R.layout.device_list, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = convertView.findViewById(R.id.deviceAddress);
                viewHolder.deviceName = convertView.findViewById(R.id.deviceName);
                viewHolder.isAvailable = convertView.findViewById(R.id.isAvaible);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            BluetoothDevice device = mLeDevices.get(position);
            final String deviceName = device.getName();
            final String deviceAddress = device.getAddress();
            viewHolder.deviceAddress.setText(deviceAddress);
            if (deviceName != null && deviceName.length() > 0){
                viewHolder.deviceName.setText(deviceName);
            } else{
                viewHolder.deviceName.setText("UNKNOWN_DEVICE");
            }
            Integer state = device.getBondState();
            if (state == BluetoothDevice.BOND_BONDED) {
                viewHolder.isAvailable.setText("PAIRED");
                if(state == BluetoothDevice.BOND_NONE){
                    viewHolder.isAvailable.setText("AVAILABLE");
                }
            } else if(state == BluetoothDevice.BOND_NONE){
                viewHolder.isAvailable.setText("AVAILABLE");
            }
            return convertView;
        }
    }
    static class ViewHolder{
        TextView deviceName;
        TextView deviceAddress;
        TextView isAvailable;
    }
    private ScanCallback mLeScanCallBack = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            mLeDeviceListAdapter.addDevice(result.getDevice());
            mLeDeviceListAdapter.notifyDataSetChanged();
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Toast.makeText(getApplicationContext(),"SCAN FAILED",Toast.LENGTH_SHORT).show();
            Log.d("SCAN_RESULT", "onScanFailed");
        }
    };
}