package com.nifty.task;

import com.angelbroking.smartapi.SmartConnect;
import com.angelbroking.smartapi.http.exceptions.SmartAPIException;
import com.angelbroking.smartapi.models.OrderParams;
import com.nifty.angelbroking.AngelConnector;
import com.nifty.dto.Candle;
import com.nifty.util.ServiceUtil;
import com.nifty.util.SuperTrendIndicator;
import com.trading.Index;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.ta4j.core.BarSeries;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class FetchAndUpdateCandlesTask implements Runnable {

    private final ServiceUtil serviceUtil;

    private final AtomicLong startTime;

    private final SuperTrendIndicator superTrendIndicator;

    private final SmartConnect smartConnect;

    private final boolean[] isStarted;

    private final AtomicReference<Double> open;

    AtomicReference<Double> low = new AtomicReference(999999.23);
    AtomicReference<Double> high = new AtomicReference(-999.23);

    public FetchAndUpdateCandlesTask(AtomicLong startTime, SuperTrendIndicator superTrendIndicator,
                                     SmartConnect smartConnect, boolean[] isStarted, AtomicReference<Double> open
                                     ,ServiceUtil serviceUtil) {
        this.startTime = startTime;
        this.superTrendIndicator = superTrendIndicator;
        this.smartConnect = smartConnect;
        this.isStarted = isStarted;
        this.open = open;
        this.serviceUtil = serviceUtil;
    }

    @Override
    public void run() {
        AtomicReference<Double> close = new AtomicReference<>();
        try {
            String ltp = AngelConnector.getNiftyltp(smartConnect);

            double niftyLtp = Double.parseDouble(ltp);

            // comment below 5 lines for production
            /*
                Random random = new Random();
                double lowt = 19420.44;
                double hight = 19500.33;
                niftyLtp= lowt + (hight - lowt) * random.nextDouble();
                System.out.println("random no is = " + niftyLtp);
             */

            if (isStarted[0]) {
                open.set(niftyLtp);
                isStarted[0] = true;
            } else {
                //high.set(Math.max(niftyLtp, high.get()));
                high.set(Math.max(open.get(),Math.max(niftyLtp, high.get())));
                low.set(Math.min(open.get(),Math.min(niftyLtp, low.get())));
            }

            if (System.currentTimeMillis() - startTime.get() >= 60000) {
                log.info("5 minutes over ..new candle formed");

                close.set(niftyLtp);

                Candle candle = Candle.builder()
                        .low(low.get())
                        .high(high.get())
                        .open(open.get())
                        .close(close.get())
                        .build();

                BarSeries barSeries = superTrendIndicator.getSeries();
                barSeries.addBar(ZonedDateTime.now(), candle.open, candle.high, candle.low, candle.close);

                superTrendIndicator.setSeries(barSeries);
                superTrendIndicator.setLength(10); superTrendIndicator.setMultiplier(3.0);
                superTrendIndicator.calculate();

                startTime.set(System.currentTimeMillis());
                open.set(niftyLtp);

                printCandleLiveData(candle);
                checkAndtakeActualTrade();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void printCandleLiveData(Candle candle) {
        if (candle != null) {
            log.info("close " + candle.close);
            log.info("open " + candle.open);
            log.info("high " + candle.high);
            log.info("low " + candle.low);
            log.info("------------------");
        }
    }


    private void checkAndtakeActualTrade(){
        List<Index> indexList =  serviceUtil.intializeSymbolTokenMap(smartConnect);

      //  Index strikePriceToTrade = serviceUtil.getNearestPremiumMatched(indexList);

        OrderParams orderParams = new OrderParams();
        orderParams.symbolToken =   "45037"; //strikePriceToTrade.getToken();
        orderParams.symboltoken =  "45037"; //strikePriceToTrade.getToken();
        orderParams.tradingsymbol =   "NIFTY07SEP2319700CE"; //strikePriceToTrade.getSymbol();

        if(superTrendIndicator.getSignal(superTrendIndicator.getSeries().getBarCount() - 1).equals("")){

            try{
                AngelConnector.placeOrder(smartConnect,orderParams);
            }catch (Exception | SmartAPIException e){
                e.printStackTrace();
            }
        }
    }
}
