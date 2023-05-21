package com.nifty.dto;

public class Candle {

    private String date;
    private double open;
    private double high;
    private double low;
    private double close;
    private double useless1;
    private double useless2;
    private double tr;
    private double atr;

    public double getTr() {
        return tr;
    }

    public void setTr(double tr) {
        this.tr = tr;
    }

    public double getAtr() {
        return atr;
    }

    public void setAtr(double atr) {
        this.atr = atr;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public double getOpen() {
        return open;
    }

    public void setOpen(double open) {
        this.open = open;
    }

    public double getHigh() {
        return high;
    }

    public void setHigh(double high) {
        this.high = high;
    }

    public double getLow() {
        return low;
    }

    public void setLow(double low) {
        this.low = low;
    }

    public double getClose() {
        return close;
    }

    public void setClose(double close) {
        this.close = close;
    }

    public double getUseless1() {
        return useless1;
    }

    public void setUseless1(double useless1) {
        this.useless1 = useless1;
    }

    public double getUseless2() {
        return useless2;
    }

    public void setUseless2(double useless2) {
        this.useless2 = useless2;
    }
}
