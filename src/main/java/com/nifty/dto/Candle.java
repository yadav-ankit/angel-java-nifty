package com.nifty.dto;

import lombok.Builder;

@Builder
public class Candle {

    public String date;
    public double open;
    public double high;
    public double low;
    public double close;
    public double basicUpperBand;
    public double basicLowerBand;
    public double tr;
    public double atr;
    public double finalUpperBand;
    public double finalLowerBand;

    public double superTrend;

}
