package com.nifty.util;

import com.angelbroking.smartapi.models.Order;
import com.nifty.dto.PnlDto;
import com.trading.Index;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class PnlHelper {


    public List<PnlDto> existingPositions;

    public double runningPnl(double ltp,String tradingSymbol){
        double pnl = 0.0;
        double sellPriceofSymbolinPicture = 0.0;

        for(PnlDto pnlDto : existingPositions){
            if(pnlDto.tradingsymbol != tradingSymbol){
                pnl = pnl + pnlDto.realisedPnl;
            }else{
                sellPriceofSymbolinPicture = pnlDto.sellPrice;
            }
        }
        pnl = pnl + (sellPriceofSymbolinPicture - ltp);
        return pnl;
    }

    public void sqaureOffCalculate(double ltp , String tradingSymbol){
        for(PnlDto pnlDto : existingPositions){
            if(tradingSymbol.equals(pnlDto.tradingsymbol)){
                pnlDto.realisedPnl = pnlDto.sellPrice - ltp;
            }
        }
    }
}
