package com.nifty.util;

import com.nifty.dto.Candle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ServiceUtil {

    public List<Candle> extractLastNCandles(List<Candle> candleList,int n){
        List<Candle> finalCandles = new ArrayList<>();
        int size = candleList.size();

        if(n > size){
            log.error("N is greater  then candles List size");
            return new ArrayList<>();
        }

        for(int i = size-n;i<size;i++){
            finalCandles.add(candleList.get(i));
        }
        return finalCandles;
    }


    private double findTrueRange(List<Candle> candleList,int n) {

        // true_range = max (h-l,abs(h-pc),abs(l-pc));

        double trueRange = 0;
        for (int i = 1; i < candleList.size(); i++) {
            Candle currentCandle = candleList.get(i);
            Candle prevCandle = candleList.get(i - 1);

            double h_l = currentCandle.getHigh() - currentCandle.getLow();
            double h_pc = Math.abs(currentCandle.getHigh() - prevCandle.getClose());
            double l_pc = Math.abs(currentCandle.getLow() - prevCandle.getClose());

            double tr = Math.max(h_l, Math.max(h_pc, l_pc));

            tr = tr * 10000;
            tr = Math.round(tr);
            tr = tr / 10000;

            trueRange = tr;
        }
        return trueRange;
    }


    public void findAverageTrueRange(List<Candle> candleList, int n, int index) {
        double sum = 0;

        for (int i = index - n; i < index; i++) {
            sum = sum + candleList.get(i).getTr();
        }

        candleList.get(index-1).setAtr(sum / n);
    }
}
