package com.example.healthdevice;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.view.MenuItem;
import android.view.View;



import androidx.appcompat.app.AppCompatActivity;

import android.view.Menu;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private LineChart realtimeChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Setup nut noi
        FloatingActionButton floatingActionButton = findViewById(R.id.BLE_btn);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, DeviceScanner.class);
                startActivity(intent);
            }
        });

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBLE = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBLE, 1);
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE is not supported", Toast.LENGTH_SHORT).show();
            finish();
        }
        setupChart();
    }

    private void setupChart() {
        //Setup Chart
        realtimeChart = findViewById(R.id.RealTimeChart);
        //realtimeChart.setOnChartValueSelectedListener(this);

        realtimeChart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                feedMultiple();
            }
        });
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
    private void addEntry() {
        LineData data = realtimeChart.getData();

        if (data != null) {
            ILineDataSet set = data.getDataSetByIndex(0);

            if (set == null) {
                set = createSet();
                data.addDataSet(set);
            }
            data.addEntry(new Entry(set.getEntryCount(),
                            (float) (Math.sin(10 * Math.random()))),
                    0);
            data.notifyDataChanged();
            realtimeChart.notifyDataSetChanged();
            realtimeChart.setVisibleXRangeMaximum(120);
            realtimeChart.moveViewToX(data.getEntryCount());
        }
    }
    private LineDataSet createSet() {
        LineDataSet lineDataSet = new LineDataSet(null, "Du Lieu Thoi Gian Thuc");
        lineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        lineDataSet.setColor(ColorTemplate.getHoloBlue());
        lineDataSet.setLineWidth(2f);
        lineDataSet.setDrawCircles(false);
        return lineDataSet;
    }
    private Thread thread;
    private void feedMultiple() {
        if (thread != null) {
            thread.interrupt();
        }
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                addEntry();
            }
        };

        thread = new Thread((new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i <= 1000; i++) {
                    runOnUiThread(runnable);
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }));

        thread.start();
    }

    //Setup Options Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_scanning:
                Intent intent = new Intent(MainActivity.this, DeviceScanner.class);
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (thread != null) {
            thread.interrupt();
        }
    }
}
