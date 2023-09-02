package com.nifty;


import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.nifty.dto.Candle;
import com.nifty.util.ServiceUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class MorningConsumer {

    public List<Candle> consume(String message) {
        List<Candle> candleList = new ArrayList<>();

        JsonArray kiteResponseArray = message == null ? null :
                StringUtils.isBlank(message) ?
                        null : new Gson().fromJson(message, JsonArray.class);

        int i = -1;

        for (JsonElement kiteElement : kiteResponseArray.get(0).getAsJsonObject().get("data").getAsJsonObject().get("candles").getAsJsonArray()) {

            JsonArray candleDetailsArray = kiteElement.getAsJsonArray();
            Candle candle = Candle.builder().build();
            for (JsonElement candleDetails : candleDetailsArray) {
                i++;
                switch (i) {
                    case 0:
                        candle.date = (candleDetails.getAsString());
                        continue;
                    case 1:
                        candle.open = (candleDetails.getAsDouble());
                        continue;
                    case 2:
                        candle.high = (candleDetails.getAsDouble());
                        continue;
                    case 3:
                        candle.low = (candleDetails.getAsDouble());
                        continue;
                    case 4:
                        candle.close = (candleDetails.getAsDouble());
                        continue;
                }
            }
            candleList.add(candle);
            i = -1;
        }

        List<Candle> lastNCandles = getCandlesHelper(candleList);

        return lastNCandles;
    }

    @NotNull
    private List<Candle> getCandlesHelper(List<Candle> candleList) {
        int SUPERTREND_LENGTH = 10;
        Collections.reverse(candleList);

        System.out.println(candleList.size());

        List<Candle> lastNCandles = new ArrayList<>();
        for (int j = 0; j < candleList.size(); j++) {
            if (j < SUPERTREND_LENGTH*2 + 1) {
                lastNCandles.add(candleList.get(j));
            }
        }

        Collections.reverse(lastNCandles);
        System.out.println(lastNCandles.size());

        // add a temp candle
        /*
        Candle tempCandle = new Candle();
        tempCandle.setOpen(18201.23);
        tempCandle.setHigh(18210.43);
        tempCandle.setLow(18190.83);
        tempCandle.setClose(18195.53);

        lastNCandles.add(tempCandle);
         */

        setTrueRange(lastNCandles);

        setAverageTrueRange(lastNCandles, SUPERTREND_LENGTH, 11);

        setBasicBands(lastNCandles, SUPERTREND_LENGTH, 11);

        setFinalBands(lastNCandles, SUPERTREND_LENGTH, 11);

        setSuperTrend(lastNCandles, SUPERTREND_LENGTH, 11);

        System.out.println(lastNCandles.size());
        return lastNCandles;
    }

    private void setTrueRange(List<Candle> lastNCandles) {

        // true_range = max (h-l,abs(h-pc),abs(l-pc));

        for (int i = 1; i < lastNCandles.size(); i++) {
            Candle currentCandle = lastNCandles.get(i);
            Candle prevCandle = lastNCandles.get(i - 1);

            double h_l = currentCandle.high - currentCandle.low;
            double h_pc = Math.abs(currentCandle.high - prevCandle.close);
            double l_pc = Math.abs(currentCandle.low - prevCandle.close);

            currentCandle.tr = ServiceUtil.roundFigure(Math.max(h_l, Math.max(h_pc, l_pc)));
        }
    }

    private void setAverageTrueRange(List<Candle> candleList, int SUPERTREND_LENGTH, int startFrom) {

        for(int i=startFrom; i < candleList.size() ; i++){
            double sum = 0;
            double atr = 0;
            for (int j = i; j >= i - SUPERTREND_LENGTH;  j--) {
                sum = sum + candleList.get(j).tr;
            }
            candleList.get(i).atr = ServiceUtil.roundFigure(sum / SUPERTREND_LENGTH);
        }
    }

    private void setBasicBands(List<Candle> candleList, int SUPERTREND_LENGTH, int index) {
       /*
            Basic Upperband  =  (High + Low) / 2 + Multiplier * ATR
            Basic Lowerband =  (High + Low) / 2 – Multiplier * ATR
        */
        for (int i = index; i<candleList.size();i++) {
            Candle candle = candleList.get(i);

            candle.basicUpperBand = ServiceUtil.roundFigure(((candle.high + candle.low) / 2) + (SUPERTREND_LENGTH * candle.atr));
            candle.basicLowerBand = ServiceUtil.roundFigure(((candle.high + candle.low) / 2) - (SUPERTREND_LENGTH * candle.atr));
        }
    }

    private void setFinalBands(List<Candle> candleList, int n, int index) {
       /*
            Final Upperband = IF( (Current Basic Upperband  < Previous Final Upperband) and
                                    (Previous Close > Previous Final Upperband))
                                Then
                                    (Current Basic Upperband)
                                ELSE
                                    (Previous Final Upperband)
        */

        double finalUpperBand = 0;
        double finalLowerBand = 0;
        for (int i = index; i < candleList.size(); i++) {

            Candle candle = candleList.get(i);
            Candle prevCandle = candleList.get(i - 1);


            if(prevCandle.finalUpperBand == 0){
                // FIRST CASE when code starts
                candle.finalUpperBand =  candle.basicUpperBand;
                candle.finalLowerBand = candle.basicLowerBand;
            }else {

                if ((candle.basicUpperBand < prevCandle.finalUpperBand) && (prevCandle.close > prevCandle.finalUpperBand)) {
                    finalUpperBand = candle.basicUpperBand;
                } else {
                    finalUpperBand = prevCandle.finalUpperBand;
                }
                candle.finalUpperBand = finalUpperBand;


            /*
             Final Lowerband = IF( (Current Basic Lowerband  > Previous Final Lowerband)
                                and (Previous Close < Previous Final Lowerband))
                                Then
                                    (Current Basic Lowerband)
                                 ELSE
                                    (Previous Final Lowerband)
             */

                if ((candle.basicLowerBand > prevCandle.finalLowerBand) && (prevCandle.close < prevCandle.finalLowerBand)) {
                    finalLowerBand = candle.basicLowerBand;
                } else {
                    finalLowerBand = prevCandle.finalLowerBand;
                }
                candle.finalLowerBand = finalLowerBand;
            }

            candle.finalLowerBand = ServiceUtil.roundFigure(candle.finalLowerBand);
            candle.finalUpperBand = ServiceUtil.roundFigure(candle.finalUpperBand);
        }
    }

    private void setSuperTrend(List<Candle> candleList, int n, int index) {
       /*
            SUPERTREND = IF(Current Close <= Current Final Upperband )
                            Then
                                Current Final Upperband
                            ELSE
                                Current  Final Lowerband
        */
        for (int i = index; i < candleList.size(); i++) {
            Candle candle = candleList.get(i);

            if(candle.close <= candle.finalUpperBand){
                candle.superTrend = candle.finalUpperBand;
            }else{
                candle.superTrend = candle.finalLowerBand;
            }
        }
    }


}
