package com.example.healthdevice;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class BluetoothLeService extends Service {
    private final static String TAG =
            BluetoothLeService.class.getSimpleName();
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;

    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED = "com.example.healthdevice.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.example.healthdevice.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICE_DISCOVERED = "com.example.healthdevice.ACTION_GATT_SERVICE_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "com.example.healthdevice.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA = "com.example.healthdevice.EXTRA_DATA";

    public final static UUID UUID_DEVICE =
            UUID.fromString(SampleGattAttributes.DEVICE);
    public final static UUID UUID_HEART_RATE_CHARACTERISTIC =
            UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT_TX);

    private final BluetoothGattCallback mGattCallBack =
            new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    super.onConnectionStateChange(gatt, status, newState);
                    String intentAction;
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        intentAction = ACTION_GATT_CONNECTED;
                        mConnectionState = STATE_CONNECTED;

                        Log.i(TAG, "CONNECTED TO GATT SERVER");
                        Log.i(TAG, "Attemting to start service discovery:" +
                                mBluetoothGatt.discoverServices());
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        intentAction = ACTION_GATT_DISCONNECTED;
                        mConnectionState = STATE_DISCONNECTED;
                        Log.i(TAG, "DISCONNECTED FROM GATT SERVER");
                        broadcastUpdate(intentAction);
                    }
                }

                @Override
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    super.onServicesDiscovered(gatt, status);
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        broadcastUpdate(ACTION_GATT_SERVICE_DISCOVERED);
                    } else {
                        Log.w(TAG, "OnServiceDiscovered received" + status);
                    }
                }

                @Override
                public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    super.onCharacteristicRead(gatt, characteristic, status);
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                        readCharacteristic(characteristic);
                    }
                }

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                    super.onCharacteristicChanged(gatt, characteristic);
                    broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                    readCharacteristic(characteristic);
                }
            };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    long NumberOfPackage;
    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        //Data MeasurementPackage = new Data();


        if (UUID_HEART_RATE_CHARACTERISTIC.equals(characteristic.getUuid())) {
            final byte[] data = characteristic.getValue();
            if (data != null && data.length >0) {

                /*System.arraycopy(data,0,FirstValue,0,4);
                System.arraycopy(data,4,SecondValue,0,4);
                float value = ByteBuffer.wrap(FirstValue).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                long time = ByteBuffer.wrap(SecondValue).order(ByteOrder.LITTLE_ENDIAN).getLong();*/


                intent.putExtra(EXTRA_DATA,data);
                /*Log.d("DATA", String.valueOf(time));
                Log.d("DATA", String.valueOf(value));
                Log.d("Number Of Package", String.valueOf(NumberOfPackage++));*/
            }
        } else {
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for (byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));

                //send data to intent as string and HEX format
                intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
            }
        }

        sendBroadcast(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            return false;
        }

        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, " Trying to use an existing mBluetoothGatt for connection.");

            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        mBluetoothGatt = device.connectGatt(this, false, mGattCallBack);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    public boolean initialize() {
            // For API level 18 and above, get a reference to BluetoothAdapter through
            // BluetoothManager.
            if (mBluetoothManager == null) {
                mBluetoothManager = (BluetoothManager) getSystemService(getApplicationContext().BLUETOOTH_SERVICE);
                if (mBluetoothManager == null) {
                    Log.e(TAG, "Unable to initialize BluetoothManager.");
                    return false;
                }
            }
            mBluetoothAdapter = mBluetoothManager.getAdapter();
            if (mBluetoothAdapter == null) {
                Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
                return false;
            }
            return true;
        }

    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;
        return mBluetoothGatt.getServices();
    }

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled){
        if (mBluetoothAdapter == null || mBluetoothGatt == null){
            Log.w(TAG,"BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic,enabled);
        // This is specific to Heart Rate Measurement.
        if (UUID_HEART_RATE_CHARACTERISTIC.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }
    public void sendData(BluetoothGattCharacteristic characteristic){
        mBluetoothGatt.writeCharacteristic(characteristic);
    }
}

