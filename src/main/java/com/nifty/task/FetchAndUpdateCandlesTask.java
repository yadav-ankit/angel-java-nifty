package com.nifty.task;

import com.angelbroking.smartapi.SmartConnect;
import com.angelbroking.smartapi.http.exceptions.SmartAPIException;
import com.angelbroking.smartapi.models.Order;
import com.angelbroking.smartapi.models.OrderParams;
import com.nifty.angelbroking.AngelConnector;
import com.nifty.dto.Candle;
import com.nifty.dto.PnlDto;
import com.nifty.util.ServiceUtil;
import com.nifty.util.SuperTrendIndicator;
import com.trading.Index;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.ta4j.core.BarSeries;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class FetchAndUpdateCandlesTask implements Runnable {

    private final ServiceUtil serviceUtil;

    private final AtomicLong startTime;

    private final SuperTrendIndicator superTrendIndicator;

    private final SmartConnect smartConnect;

    private final AtomicReference<Double> open;

    AtomicReference<Double> low = new AtomicReference(999999.23);
    AtomicReference<Double> high = new AtomicReference(-999.23);

    public FetchAndUpdateCandlesTask(AtomicLong startTime, SuperTrendIndicator superTrendIndicator,
                                     SmartConnect smartConnect,AtomicReference<Double> open, ServiceUtil serviceUtil) {
        this.startTime = startTime;
        this.superTrendIndicator = superTrendIndicator;
        this.smartConnect = smartConnect;
        this.open = open;
        this.serviceUtil = serviceUtil;
    }

    @Override
    public void run() {

        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        String currentTime = dateFormat.format(date);

        if (serviceUtil.isTimeInBetween("09:15:00", "15:20:00", currentTime)) {
            executeRun();
        } else {
            // exit all positions
        }
    }

    private void executeRun() {
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

            if (!serviceUtil.firstTradeTaken) {
                takeFirstTrade();
                open.set(niftyLtp);
            } else {
                //high.set(Math.max(niftyLtp, high.get()));
                high.set(Math.max(open.get(), Math.max(niftyLtp, high.get())));
                low.set(Math.min(open.get(), Math.min(niftyLtp, low.get())));
            }
            // for 5 mins change to 300_000
            if (System.currentTimeMillis() - startTime.get() >= 20_000) {
                log.info("20 Seconds over ..new candle formed");

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
                superTrendIndicator.setLength(10);
                superTrendIndicator.setMultiplier(3.0);
                superTrendIndicator.calculate();

                startTime.set(System.currentTimeMillis());
                open.set(niftyLtp);

                printCandleLiveData(candle);
                checkAndTakeActualTrade();
                findPnlForTesting();
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


    private void checkAndTakeActualTrade() {
        List<Index> indexList = serviceUtil.intializeSymbolTokenMap(smartConnect);
        Order order = null;
        int length = superTrendIndicator.getSeries().getBarCount();
        String optionType = Double.parseDouble(serviceUtil.niftyLtp) > superTrendIndicator.getValue(length - 1)
                ? "PE" : "CE";
        int distance_from_atm = serviceUtil.getDistanceFromATM(LocalDate.now().getDayOfWeek());
        Index strikePriceToTrade = serviceUtil.getAtLeastPointsAwayFromATM(indexList, optionType, distance_from_atm);

        OrderParams orderParams = new OrderParams();
        orderParams.symbolToken = strikePriceToTrade.getToken();
        orderParams.symboltoken = strikePriceToTrade.getToken();
        orderParams.tradingsymbol = strikePriceToTrade.getSymbol();
        orderParams.quantity = 700;

        // in PROD ..change this to != ""
        if (!superTrendIndicator.getSignal(superTrendIndicator.getSeries().getBarCount() - 1).equals("")) {
            // first exit all Positions
            try {
                order = AngelConnector.placeOrder(smartConnect, orderParams);
            } catch (Exception | SmartAPIException e) {
                e.printStackTrace();
            }

            // since avgPrice = null so as of now use a constant val
            // sellPrice(Double.parseDouble(order.averagePrice))
            PnlDto pnlDto = PnlDto.builder()
                    .sellPrice(18.22)
                    .isCompleted(false)
                    .isExecuted(true)
                    .tradingSymbol(orderParams.tradingsymbol)
                    .symbolToken(orderParams.symbolToken)
                    .quantity(700)
                    .build();

            serviceUtil.addIntoPosition(pnlDto);
        }
    }

    private void findPnlForTesting() {
        List<PnlDto> existingPositions = serviceUtil.fetchExistingPositions();

        if (existingPositions.isEmpty()) {
            return;
        }

        PnlDto runningOrder = existingPositions.stream().filter(s -> !s.isCompleted).findAny().orElse(null);
        JSONObject ltp = smartConnect.getLTP("NFO", runningOrder.tradingSymbol, runningOrder.symbolToken);
        double ltpPrice = Double.parseDouble(ltp.get("ltp").toString());

        runningOrder.realisedPnl = (runningOrder.sellPrice - ltpPrice) * runningOrder.quantity;
        serviceUtil.getPnlHelper().addRunningPositonPNLInExistingPositionArray(runningOrder.realisedPnl);

        // existing position + new order ke ltp se
        double currentRunningPnl = serviceUtil.runningPnl();
        log.info("Current running Position = {} , sold at {} , at Ltp price : {}" , runningOrder.tradingSymbol , runningOrder.sellPrice , ltpPrice);
        log.info("total MTM today is {}", ServiceUtil.roundFigure(currentRunningPnl));
    }

    private void takeFirstTrade() {
        List<Index> indexList = serviceUtil.intializeSymbolTokenMap(smartConnect);

        int length = superTrendIndicator.getSeries().getBarCount();
        String optionType = Double.parseDouble(serviceUtil.niftyLtp) > superTrendIndicator.getValue(length - 1)
                ? "PE" : "CE";

        int distance_from_atm = serviceUtil.getDistanceFromATM(LocalDate.now().getDayOfWeek());
        Index strikePriceToTrade = serviceUtil.getAtLeastPointsAwayFromATM(indexList, optionType, distance_from_atm);

        OrderParams orderParams = new OrderParams();
        orderParams.symbolToken = strikePriceToTrade.getToken();
        orderParams.symboltoken = strikePriceToTrade.getToken();
        orderParams.tradingsymbol = strikePriceToTrade.getSymbol();
        orderParams.quantity = 700;

        // if it is 9 25
        Date date = new Date();
        Order order = null;
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        String currentTime = dateFormat.format(date);

        if (smartConnect.getPosition().get("data").equals(null) && serviceUtil.isTimeInBetween("09:25:00", "15:29:00", currentTime)) {
            try {
                order = AngelConnector.placeOrder(smartConnect, orderParams);
            } catch (Exception | SmartAPIException e) {
                e.printStackTrace();
            }
            // since avgPrice = null so as of now use a constant val
            // sellPrice(Double.parseDouble(order.averagePrice))
            PnlDto pnlDto = PnlDto.builder()
                    .sellPrice(18.22)
                    .isCompleted(false)
                    .isExecuted(true)
                    .tradingSymbol(orderParams.tradingsymbol)
                    .symbolToken(orderParams.symbolToken)
                    .quantity(700)
                    .build();

            log.info("First trade taken wth Symbol : {} at price : {}" , orderParams.tradingsymbol , 18);

            serviceUtil.addIntoPosition(pnlDto);
            serviceUtil.firstTradeTaken = true;
        }
    }
}
