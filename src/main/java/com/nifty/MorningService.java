package com.nifty;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.nifty.dto.Candle;
import org.apache.commons.lang3.StringUtils;
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

@Service
public class MorningService {

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
                        candle.setDate(candleDetails.getAsString());
                        continue;
                    case 1:
                        candle.setOpen(candleDetails.getAsDouble());
                        continue;
                    case 2:
                        candle.setHigh(candleDetails.getAsDouble());
                        continue;
                    case 3:
                        candle.setLow(candleDetails.getAsDouble());
                        continue;
                    case 4:
                        candle.setClose(candleDetails.getAsDouble());
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


        findAverageTrueRange(lastNCandles, n, 10);

        System.out.println(lastNCandles.size());

        return lastNCandles;
    }

    private void findTrueRange(List<Candle> lastNCandles) {

        // true_range = max (h-l,abs(h-pc),abs(l-pc));

        for (int i = 1; i < lastNCandles.size(); i++) {
            Candle currentCandle = lastNCandles.get(i);
            Candle prevCandle = lastNCandles.get(i - 1);

            double h_l = currentCandle.getHigh() - currentCandle.getLow();
            double h_pc = Math.abs(currentCandle.getHigh() - prevCandle.getClose());
            double l_pc = Math.abs(currentCandle.getLow() - prevCandle.getClose());

            double tr = Math.max(h_l, Math.max(h_pc, l_pc));

            tr = tr * 10000;
            tr = Math.round(tr);
            tr = tr / 10000;

            currentCandle.setTr(tr);
        }
    }

    private void findAverageTrueRange(List<Candle> candleList, int n, int index) {
        double sum = 0;

        for (int i = index - n; i < index; i++) {
            sum = sum + candleList.get(i).getTr();
        }

        candleList.get(index-1).setAtr(sum / n);
    }
}
