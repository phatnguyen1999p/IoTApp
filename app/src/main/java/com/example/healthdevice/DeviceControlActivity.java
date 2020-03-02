package com.example.healthdevice;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import static com.example.healthdevice.SampleGattAttributes.HEART_RATE_MEASUREMENT_RX;
import static com.example.healthdevice.SampleGattAttributes.HEART_RATE_MEASUREMENT_TX;

public class DeviceControlActivity extends AppCompatActivity {
    private static final String TAG = DeviceControlActivity.class.getSimpleName();
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRA_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private TextView ConnectionState_TV;
    private TextView mDataField;

    private String mDeviceAddress;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private BluetoothGattCharacteristic mWriteCharacteristic;
    private BluetoothLeService mBluetoothLeService;
    private List<BluetoothGattCharacteristic> gattCharacteristics;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Initialized Bluetooth");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBluetoothLeService = null;
        }
    };

    Long time;
    Float value;
    Integer numberOfPackage;
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                boolean mConnected = true;
                updateConnectionState("CONNECTED");
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                updateConnectionState("DISCONNECTED");
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICE_DISCOVERED.equals(action)) {
                updateConnectionState("GATT_SERVICE_DISCOVERED");
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
                numberOfPackage = 0;
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                updateConnectionState("ACTION_DATA_AVAILABLE");

                numberOfPackage++;

                byte[] bytes = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                if (bytes != null && bytes.length > 0) {


                    byte[] FirstValue = new byte[8];
                    byte[] SecondValue = new byte[8];

                    System.arraycopy(bytes,0,FirstValue,0,4);
                    System.arraycopy(bytes,4,SecondValue,0,4);
                    value = ByteBuffer.wrap(FirstValue).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                    time = ByteBuffer.wrap(SecondValue).order(ByteOrder.LITTLE_ENDIAN).getLong();
                    addEntry();
                    setNumberOfPackage();
                }
            }
        }
    };

    private void updateConnectionState(final String resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ConnectionState_TV.setText(resourceId);
            }
        });
    }

    private void clearUI() {
        mDataField.setText("0");
    }

    private LineChart realtimeChart;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_control);

        final Intent intent = getIntent();
        mDeviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS);
        String mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);

        ConnectionState_TV = findViewById(R.id.deviceStatus_text);
        TextView deviceName_TV = findViewById(R.id.deviceName_text);
        TextView deviceAddress_TV = findViewById(R.id.deviceAddress_text);
        mDataField = findViewById(R.id.dataField_TV);

        deviceAddress_TV.setText(mDeviceAddress);
        deviceName_TV.setText(mDeviceName);


        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

        final Button requestData = findViewById(R.id.requestdata_btn);
        requestData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enableNotifycation();
            }
        });

        final Button sendData_btn = findViewById(R.id.sendData);
        sendData_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                writeCharacteristic("DATA FROM ANDROID");
            }
        });

        SetLineChart();
    }

    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            if (result) {
                ConnectionState_TV.setText("CONNECTED");
            } else {
                ConnectionState_TV.setText("UNABLE TO CONNECT");
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBluetoothLeService.disconnect();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    private void SetLineChart(){
        realtimeChart = findViewById(R.id.realtimeLineChart);
        //realtimeChart.setOnChartValueSelectedListener(this);

        /*realtimeChart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                feedMultiple();
            }
        });*/
        // enable description text
        realtimeChart.getDescription().setEnabled(true);

        // enable touch gestures
        realtimeChart.setTouchEnabled(true);

        // enable scaling and dragging
        realtimeChart.setDragEnabled(true);
        realtimeChart.setScaleEnabled(true);
        realtimeChart.setDrawGridBackground(false);

        // if disabled, scaling can be done on x- and y-axis separately
        realtimeChart.setPinchZoom(true);

        // set an alternative background color
        realtimeChart.setBackgroundColor(Color.DKGRAY);

        // set Text color
        LineData data = new LineData();
        data.setHighlightEnabled(true);
        data.setValueTextColor(Color.WHITE);

        realtimeChart.setData(data);

        // set legend
        Legend legend = realtimeChart.getLegend();
        legend.setForm(Legend.LegendForm.LINE);
        // Dat kieu chu
        //legend.setTypeface(tfLight);
        legend.setTextColor(Color.WHITE);

        XAxis xAxis = realtimeChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM_INSIDE);
        xAxis.setDrawAxisLine(true);
        xAxis.setDrawGridLinesBehindData(true);
        xAxis.setTextColor(Color.WHITE);

        YAxis yAxisLeft = realtimeChart.getAxisLeft();
        yAxisLeft.setDrawZeroLine(true);
        yAxisLeft.setTextColor(Color.YELLOW);

        YAxis yAxisRight = realtimeChart.getAxisRight();
        yAxisRight.setEnabled(false);
    }

    private void setNumberOfPackage() {
            mDataField.setText(String.valueOf(numberOfPackage));
    }
    private void addEntry(){
        LineData lineData = realtimeChart.getData();

        if (lineData != null){
            ILineDataSet set = lineData.getDataSetByIndex(0);

            if (set == null){
                //Tao du lieu mang thuoc tinh du lieu cu
                set = createSet();
                lineData.addDataSet(set);
            }
            //Them du lieu vao lineData
            lineData.addEntry(new Entry(time.floatValue()/1000,value), 0);
            //Thong bao cap nhat du lieu
            lineData.notifyDataChanged();
            //Thong bao cap nhat do thi
            realtimeChart.notifyDataSetChanged();

            //Khoang hien thi
            realtimeChart.setVisibleXRangeMaximum(5);

            //Chay do thi
            realtimeChart.moveViewToX(time.floatValue());
        }
    }

    private LineDataSet createSet(){
        LineDataSet lineDataSet = new LineDataSet(null,"Du Lieu Thoi Gian Thuc");
        lineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        lineDataSet.setColor(ColorTemplate.getHoloBlue());
        lineDataSet.setLineWidth(2f);
        lineDataSet.setDrawCircles(false);
        return lineDataSet;
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICE_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        for (BluetoothGattService bluetoothGattService : gattServices) {
            gattCharacteristics = bluetoothGattService.getCharacteristics();
            findCharacteristicProperties();
        }
    }

    private void findCharacteristicProperties() {
        if (gattCharacteristics == null){
            return;
        }
        for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
            Log.d("Characterristic UUID", gattCharacteristic.getUuid().toString());
            if ((gattCharacteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0) {
                if(gattCharacteristic.getUuid().toString().equalsIgnoreCase(HEART_RATE_MEASUREMENT_RX)){
                    mWriteCharacteristic = gattCharacteristic;
                    Log.d("Characterristic UUID", "PROPERTY_WRITE_NR");
                }
            }
            if ((gattCharacteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                if(gattCharacteristic.getUuid().toString().equalsIgnoreCase(HEART_RATE_MEASUREMENT_TX)){
                    mNotifyCharacteristic = gattCharacteristic;
                    Log.d("Characterristic UUID", "PROPERTY_NOTIFY");
                }
            }
        }
    }

    private void enableNotifycation() {
        if(mNotifyCharacteristic != null) {
            String UUID = mNotifyCharacteristic.getUuid().toString();
            if (UUID.equalsIgnoreCase(HEART_RATE_MEASUREMENT_TX)) {
                mBluetoothLeService.setCharacteristicNotification(mNotifyCharacteristic, true);
                mBluetoothLeService.readCharacteristic(mNotifyCharacteristic);
            }
        }
    }

    public void writeCharacteristic(String Data) {
        if (mWriteCharacteristic != null) {
            byte[] dataBytes = Data.getBytes();
            mWriteCharacteristic.setValue(dataBytes);
            mBluetoothLeService.sendData(mWriteCharacteristic);
        }
    }
}
