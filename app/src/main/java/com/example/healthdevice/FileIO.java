package com.example.healthdevice;

import java.io.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import android.os.Environment;
import android.util.Log;

class FileIO{
    private final static String path = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "/HeartRate";
    private final static File DIRECTION = new File(path);
    private static ArrayList<File> fileData = new ArrayList<>();

    static Data readData(String FileName){
        ArrayList<Double> tg = new ArrayList<>();
        ArrayList<Double> dt = new ArrayList<>();
        String line;
        Data d = null;
        try{
            FileInputStream fileInputStream = new FileInputStream (new File(DIRECTION +"/"+FileName));
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader Buffer = new BufferedReader(inputStreamReader);

            while((line = Buffer.readLine()) != null){
                String[] a = line.split(";");
                tg.add(Double.parseDouble(a[0]));
                dt.add(Double.parseDouble(a[1]));
            }
            Double[] ThoiGian = tg.toArray(new Double[tg.size()]);
            Double[] DataValues = dt.toArray(new Double[dt.size()]);
            d = new Data(ThoiGian, DataValues, FileName);

            fileInputStream.close();
            Buffer.close();
        }
        catch(FileNotFoundException ex) {
            ex.printStackTrace();
        }
        catch(IOException ex){
            ex.printStackTrace();
        }
        return d;
    }
    static boolean saveData(Double[] Time, Double[] Data){

        LocalDateTime now = LocalDateTime.now();
        String FileName = String.valueOf(now) + ".txt";

        try{
            File file = new File(DIRECTION + "/" + FileName);
            file.createNewFile();

            FileOutputStream fileOutputStream = new FileOutputStream(file,true);
            for (int i = 0; i<Time.length;i++)
            fileOutputStream.write((Time[i] + ";" + Data[i] + System.getProperty("line.separator")).getBytes());
            return true;
        }
        catch (FileNotFoundException ex){
            ex.printStackTrace();
        }
        catch (IOException ex){
            ex.printStackTrace();
        }
        return false;
    }
    static boolean saveData(String[] Data){
        LocalDateTime now = LocalDateTime.now();
        String FileName = String.valueOf(now) + ".txt";

        try {
            File file = new File(DIRECTION + "/" + FileName);
            file.createNewFile();

            FileOutputStream fileOutputStream = new FileOutputStream(file,true);
            for (int i = 1; i < Data.length; i++) {
                fileOutputStream.write((Data[i] + System.getProperty("line.separator")).getBytes());
            }
            return true;
        }
        catch (FileNotFoundException ex){
            ex.printStackTrace();
            return false;
        }
        catch (IOException ex){
            ex.printStackTrace();
            return false;
        }

    }
    static ArrayList<File> findTxtFiles(){
        fileData.clear();
        File direction = new File(path);
        File[] files = direction.listFiles();
        for (File file : files) {
            if (file.getPath().endsWith(".txt") || file.getPath().endsWith(".TXT")) {
                fileData.add(file);
            }
        }
        return fileData;
    }
    static void createDir(){
        File direction = new File(path);
        if (!direction.exists()){
            if (direction.mkdir()){
                Log.d("DIRECTION","CREATE SUCESSFULLY");
            }
            else {
                Log.d("DIRECTION","CREATE UNSUCESSFULLY");
            }
        }
    }
}