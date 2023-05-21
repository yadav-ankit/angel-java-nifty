package com.trading;

import java.util.Date;

public class Index {

    private String token;
    private String symbol;
    private String name;
    private Date expiry;
    private String expiryString;

    public String getExpiryString() {
        return expiryString;
    }

    public void setExpiryString(String expiryString) {
        this.expiryString = expiryString;
    }

    private String ltp;

    private Integer strike;
    private String lotsize;
    private String instrumenttype;
    private String exchSeg;


    public Date getExpiry() {
        return expiry;
    }

    public void setExpiry(Date expiry) {
        this.expiry = expiry;
    }
    public Integer getStrike() {
        return strike;
    }

    public void setStrike(Integer strike) {
        this.strike = strike;
    }


    public String getExchSeg() {
        return exchSeg;
    }

    public void setExchSeg(String exchSeg) {
        this.exchSeg = exchSeg;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLotsize() {
        return lotsize;
    }

    public void setLotsize(String lotsize) {
        this.lotsize = lotsize;
    }

    public String getInstrumenttype() {
        return instrumenttype;
    }

    public void setInstrumenttype(String instrumenttype) {
        this.instrumenttype = instrumenttype;
    }
    public String getLtp() {
        return ltp;
    }

    public void setLtp(String ltp) {
        this.ltp = ltp;
    }

}
