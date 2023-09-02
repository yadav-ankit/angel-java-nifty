package com.nifty;

import com.angelbroking.smartapi.SmartConnect;
import com.nifty.angelbroking.AngelConnector;
import com.nifty.dto.Candle;
import com.nifty.util.ServiceUtil;
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
    MorningConsumer morningConsumer;

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
            candleList = morningConsumer.consume(resource);
        } catch (Exception e) {
            System.out.println("EXCEPTION WHILE INVOKING API KEY REQUEST : " + e);
        }
        return candleList;
    }

    public void performIntradayTrading() {

        List<Candle> candleList = createCandlesData(url);

        SmartConnect smartConnect = AngelConnector.connectWithAngel();

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
                String ltp = AngelConnector.getNiftyltp(smartConnect);

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
