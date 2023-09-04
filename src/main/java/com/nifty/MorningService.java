package com.nifty;

import com.angelbroking.smartapi.SmartConnect;
import com.nifty.angelbroking.AngelConnector;
import com.nifty.dto.Candle;
import com.nifty.task.FetchAndUpdateCandlesTask;
import com.nifty.util.SuperTrendIndicator;
import lombok.extern.slf4j.Slf4j;
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
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
public class MorningService {

    @Autowired
    AngelConnector angelConnector;

    @Autowired
    MorningConsumer morningConsumer;

    @Value("${morning.url}")
    private String url;

    private Candle createCandlesData(String url, SuperTrendIndicator superTrendIndicator) {
        Candle lastCandle = null;
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
            lastCandle = morningConsumer.consume(resource, superTrendIndicator);
        } catch (Exception e) {
            System.out.println("EXCEPTION WHILE INVOKING API KEY REQUEST : " + e);
        }
        return lastCandle;
    }

    public void performIntradayTrading() {

        SuperTrendIndicator superTrendIndicator = new SuperTrendIndicator();
        log.info("App started ..fetching last day data");
        Candle lastCandle = createCandlesData(url, superTrendIndicator);
        log.info("last day data fetched.. closed at = " , lastCandle.close);
        startTrading(superTrendIndicator,lastCandle);
    }

    private void startTrading(SuperTrendIndicator superTrendIndicator, Candle lastCandle) {
        SmartConnect smartConnect = AngelConnector.connectWithAngel();

        final boolean[] isStarted = {false};

        AtomicReference<Double> open = new AtomicReference<>();

        if (!superTrendIndicator.getSeries().isEmpty() && !isStarted[0]) {
            open.set(lastCandle.close);
        }

           /*
            Below code runs every 10 seconds and calculate high/low,
            so it runs 6 times in 1 min   ...... 30 times in 5 mins
            After 5 min ... it creates a new candle with OHLC and add it to BarSeries
         */

        AtomicLong startTime = new AtomicLong(System.currentTimeMillis());

        FetchAndUpdateCandlesTask task = new FetchAndUpdateCandlesTask(startTime,
                superTrendIndicator, smartConnect, isStarted, open);

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

        executor.scheduleAtFixedRate(task,
                0, 10, TimeUnit.SECONDS);
    }

}
