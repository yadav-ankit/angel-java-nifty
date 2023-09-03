package com.nifty.task;

import com.angelbroking.smartapi.SmartConnect;
import com.nifty.angelbroking.AngelConnector;
import com.nifty.dto.Candle;
import com.nifty.util.SuperTrendIndicator;
import org.ta4j.core.BarSeries;

import java.time.ZonedDateTime;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class FetchAndUpdateCandlesTask implements Runnable {

    private final AtomicLong startTime;

    private final SuperTrendIndicator superTrendIndicator;

    private final SmartConnect smartConnect;

    private final boolean[] isStarted;

    private final AtomicReference<Double> open;

    AtomicReference<Double> low = new AtomicReference(999999.23);
    AtomicReference<Double> high = new AtomicReference(-999.23);

    public FetchAndUpdateCandlesTask(AtomicLong startTime, SuperTrendIndicator superTrendIndicator,
                                     SmartConnect smartConnect, boolean[] isStarted, AtomicReference<Double> open) {
        this.startTime = startTime;
        this.superTrendIndicator = superTrendIndicator;
        this.smartConnect = smartConnect;
        this.isStarted = isStarted;
        this.open = open;
    }

    @Override
    public void run() {
        AtomicReference<Double> close = new AtomicReference<>();
        try {
            String ltp = "19423.88";//AngelConnector.getNiftyltp(smartConnect);

            double niftyLtp = Double.parseDouble(ltp);

            // comment below 5 lines for production
            Random random = new Random();
            double lowt = 19420.44;
            double hight = 19500.33;
            niftyLtp= lowt + (hight - lowt) * random.nextDouble();
            System.out.println("random no is = " + niftyLtp);

            if (isStarted[0]) {
                open.set(niftyLtp);
                isStarted[0] = true;
            } else {
                //high.set(Math.max(niftyLtp, high.get()));
                high.set(Math.max(open.get(),Math.max(niftyLtp, high.get())));
                low.set(Math.min(open.get(),Math.min(niftyLtp, low.get())));
            }

            if (System.currentTimeMillis() - startTime.get() >= 30000) {
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
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void printCandleLiveData(Candle candle) {
        if (candle != null) {
            System.out.println("close " + candle.close);
            System.out.println("open " + candle.open);
            System.out.println("high " + candle.high);
            System.out.println("low " + candle.low);
            System.out.println("------------------");
        }
    }
}
