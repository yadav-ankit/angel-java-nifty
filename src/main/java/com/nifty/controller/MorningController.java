package com.nifty.controller;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@RequestMapping("nifty")
@RestController
public class MorningController {


    @GetMapping("/reloadCache")
    public String reloadCache() {
        HttpEntity<String> request = new HttpEntity<>(new HttpHeaders());

        String url = "http://localhost:3000/nifty";

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
            // System.out.println(resource);
            consume(resource);
        } catch (Exception e) {
            System.out.println("EXCEPTION WHILE INVOKING API KEY REQUEST : " + e);
        }


        return "Cache Reloaded Successfully";
    }

    private void consume(String message) {
        List<Candle> candleList = new ArrayList<>();
        JsonObject kiteResponse = message == null ? null :
                StringUtils.isBlank(message) ?
                        null : new Gson().fromJson(message, JsonObject.class);
        int i = -1;

        for (JsonElement kiteElement : kiteResponse.getAsJsonObject("data").getAsJsonArray("candles")) {

            JsonArray candleDetailsArray = kiteElement.getAsJsonArray();
            Candle candle = new Candle();
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

                candleList.add(candle);
            }
            i = 0;
        }

        System.out.println(candleList.size());

    }
}

