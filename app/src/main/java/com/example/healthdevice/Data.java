package com.example.healthdevice;

public class Data{
    private Double[] ThoiGian = null;
    private Double[] DataValues = null;
    private String FileName = null;
    public Data(Double[] ThoiGian, Double[] DataValues, String FileName) {
        this.ThoiGian = ThoiGian;
        this.DataValues = DataValues;
        this.FileName = FileName;
    }
    public Data(){
    }
    public Data(Double[] ThoiGian, Double[] DataValues){
        this.ThoiGian = ThoiGian;
        this.DataValues = DataValues;
    }
    public Double[] getThoiGian(){return this.ThoiGian; }
    public Double[] getData(){
        return this.DataValues;
    }
    public String getFileName(){return this.FileName; }
}
