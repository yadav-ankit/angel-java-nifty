package com.nifty;


import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nifty.dto.Candle;
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
        JsonObject kiteResponse = message == null ? null :
                StringUtils.isBlank(message) ?
                        null : new Gson().fromJson(message, JsonObject.class);
        int i = -1;

        for (JsonElement kiteElement : kiteResponse.getAsJsonObject("data").getAsJsonArray("candles")) {

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
        int n = 10;
        Collections.reverse(candleList);

        System.out.println(candleList.size());

        List<Candle> lastNCandles = new ArrayList<>();
        for (int j = 0; j < candleList.size(); j++) {
            if (j < n) {
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

        System.out.println(lastNCandles.size());

        setAverageTrueRange(lastNCandles, n, 10);

        setBasicBands(lastNCandles, n, 10);

        setFinalBands(lastNCandles, n, 10);

        setSuperTrend(lastNCandles, n, 10);

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

            double tr = Math.max(h_l, Math.max(h_pc, l_pc));

            tr = tr * 10000;
            tr = Math.round(tr);
            tr = tr / 10000;

            currentCandle.tr = tr;
        }
    }

    private void setAverageTrueRange(List<Candle> candleList, int n, int index) {
        double sum = 0;

        for (int i = index - n; i < index; i++) {
            sum = sum + candleList.get(i).tr;
        }
        candleList.get(index - 1).atr = (sum / n);
    }

    private void setBasicBands(List<Candle> candleList, int n, int index) {
       /*
            Basic Upperband  =  (High + Low) / 2 + Multiplier * ATR
            Basic Lowerband =  (High + Low) / 2 – Multiplier * ATR
        */
        for (int i = index - n; i < index; i++) {
            Candle candle = candleList.get(index);

            candle.basicLowerBand = (candle.high + candle.low) / 2 + 10 * candle.atr;
            candle.basicUpperBand = (candle.high + candle.low) / 2 - 10 * candle.atr;
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
        for (int i = index - n; i < index; i++) {

            Candle candle = candleList.get(index);
            Candle prevCandle = candleList.get(index - 1);

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
    }

    private void setSuperTrend(List<Candle> candleList, int n, int index) {
       /*
            SUPERTREND = IF(Current Close <= Current Final Upperband )
                            Then
                                Current Final Upperband
                            ELSE
                                Current  Final Lowerband
        */
        for (int i = index - n; i < index; i++) {
            Candle candle = candleList.get(index);

            if(candle.close <= candle.finalUpperBand){
                candle.superTrend = candle.finalUpperBand;
            }else{
                candle.superTrend = candle.finalLowerBand;
            }
        }
    }


}
