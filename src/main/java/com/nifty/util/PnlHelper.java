package com.nifty.util;

import com.nifty.dto.PnlDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class PnlHelper {

    public List<PnlDto> existingPositions;

    public double runningPnl() {
        double currentRunnngPnl = 0.0;
        for (PnlDto pnlDto : existingPositions) {
           currentRunnngPnl = currentRunnngPnl + pnlDto.realisedPnl;
        }
        return currentRunnngPnl;
    }

    public void squareOffCalculate(double ltp, String tradingSymbol) {
        for (PnlDto pnlDto : existingPositions) {
            if (tradingSymbol.equals(pnlDto.tradingSymbol)) {
                pnlDto.realisedPnl = pnlDto.sellPrice - ltp;
            }
        }
    }
}
