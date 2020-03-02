package com.example.healthdevice;

import java.util.HashMap;

public class SampleGattAttributes {
    private static HashMap<String,String> attributes = new HashMap<>();
    public static String DEVICE = "6E400001-B5A3-F393-E0A9-E50E24DCCA9E";
    public static String HEART_RATE_MEASUREMENT_RX = "6E400002-B5A3-F393-E0A9-E50E24DCCA9E";
    public static String HEART_RATE_MEASUREMENT_TX = "6E400003-B5A3-F393-E0A9-E50E24DCCA9E";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    static {
        attributes.put(DEVICE, "DEVICE_SERVICE");
    }
    public static String lookup(String uuid, String defaultName){
        String name = attributes.get(uuid);
        return name == null? defaultName:name;
    }
}
