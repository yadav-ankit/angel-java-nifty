package com.nifty;

import com.angelbroking.smartapi.SmartConnect;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nifty.angelbroking.AngelConnector;
import com.nifty.dto.Candle;
import com.nifty.util.ServiceUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class MorningService {

    @Autowired
    AngelConnector angelConnector;

    @Autowired
    ServiceUtil serviceUtil;

    @Value("${morning.url}")
    private String url;

    public List<Candle> createCandlesData(String url) {

        List<Candle> candleList = new ArrayList<>();
        HttpEntity<String> request = new HttpEntity<>(new HttpHeaders());

        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(url);


        String urlTemplate = uriComponentsBuilder.uriVariables(new HashMap<>()).toUriString();
        RestTemplate restTemplate = new RestTemplate();

        DefaultUriBuilderFactory defaultUriBuilderFactory = new DefaultUriBuilderFactory();
        defaultUriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);

        restTemplate.setUriTemplateHandler(defaultUriBuilderFactory);
        restTemplate.getMessageConverters()
                .add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));

        ResponseEntity apiResponse;
        String resource = null;
        try {
            apiResponse = restTemplate.exchange(
                    urlTemplate,
                    HttpMethod.GET,
                    request,
                    String.class
            );
            resource = apiResponse.getBody().toString();
            candleList = consume(resource);
        } catch (Exception e) {
            System.out.println("EXCEPTION WHILE INVOKING API KEY REQUEST : " + e);
        }
        return candleList;
    }

    private List<Candle> consume(String message) {
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

        findTrueRange(lastNCandles);

        System.out.println(lastNCandles.size());

        setAverageTrueRange(lastNCandles, n, 10);

        System.out.println(lastNCandles.size());

        return lastNCandles;
    }

    private void findTrueRange(List<Candle> lastNCandles) {

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

    public void performIntradayTrading() {

        List<Candle> candleList = createCandlesData(url);

        SmartConnect smartConnect = angelConnector.connectWithAngel();

        int n = 10;

        /*
        every 10 seconds start with count = 1...
        so it runs 6 times in 1 min   ...... 30 times in 5 mins
         */

        AtomicLong startTime = new AtomicLong(System.currentTimeMillis());
        final boolean[] isStarted = {false};

        AtomicReference<Double> open = new AtomicReference<>();

        if (!candleList.isEmpty() && !isStarted[0]) {
            open.set(candleList.get(candleList.size() - 1).close);
        }

        Runnable periodicRunnable = getRunnableInstance(candleList, smartConnect, n, startTime, isStarted, open);

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(periodicRunnable, 0, 10, TimeUnit.SECONDS);

    }

    @NotNull
    private Runnable getRunnableInstance(List<Candle> candleList, SmartConnect smartConnect, int n, AtomicLong startTime, boolean[] isStarted, AtomicReference<Double> open) {
        AtomicReference<Double> close = new AtomicReference<>();
        AtomicReference<Double> low = new AtomicReference(999999.23);
        AtomicReference<Double> high = new AtomicReference(-999.23);


        Runnable periodicRunnable = () -> {
            try {
                String ltp = angelConnector.getNiftyltp(smartConnect);

                double niftyLtp = Double.parseDouble(ltp);

                if (!isStarted[0]) {
                    open.set(niftyLtp);
                    isStarted[0] = true;
                } else {
                    high.set(Math.max(niftyLtp, high.get()));
                    low.set(Math.min(niftyLtp, low.get()));
                }

                if (System.currentTimeMillis() - startTime.get() >= 300000) {
                    close.set(niftyLtp);

                    Candle candle = Candle.builder()
                            .low(low.get())
                            .high(high.get())
                            .open(open.get())
                            .close(close.get())
                            .build();

                    candleList.add(candle);

                    int size = candleList.size();
                    candleList.get(size - 1).tr = (serviceUtil.extractTrueRange(candleList, n));
                    serviceUtil.setAverageTrueRange(candleList, n, size);

                    startTime.set(System.currentTimeMillis());
                    open.set(niftyLtp);

                    if (!candleList.isEmpty()) {
                        Candle printt = candleList.get(candleList.size() - 1);
                        System.out.println("close " + printt.close);
                        System.out.println("open " + printt.open);
                        System.out.println("high " + printt.high);
                        System.out.println("low " + printt.low);
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        return periodicRunnable;
    }
}
