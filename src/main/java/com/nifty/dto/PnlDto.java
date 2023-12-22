package com.nifty.dto;

import lombok.Builder;

@Builder
public class PnlDto {

    public double buyPrice;

    public double sellPrice;

    public double realisedPnl;

    public String tradingSymbol;

    public String symbolToken;

    public boolean isExecuted;

    public boolean isCompleted;

    public int quantity;
}
