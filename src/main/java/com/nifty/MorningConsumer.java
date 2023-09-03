package com.nifty;


import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.nifty.dto.Candle;
import com.nifty.util.SuperTrendIndicator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class MorningConsumer {

    public Candle consume(String message,SuperTrendIndicator superTrendIndicator) {
        List<Candle> candleList = new ArrayList<>();

        JsonArray kiteResponseArray = message == null ? null :
                StringUtils.isBlank(message) ?
                        null : new Gson().fromJson(message, JsonArray.class);

        int i = -1;

        for (JsonElement kiteElement : kiteResponseArray.get(0).getAsJsonObject().get("data").getAsJsonObject().get("candles").getAsJsonArray()) {

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
        List<Candle> last100Candles = startWith100Candles(candleList);
        createBarSeries(last100Candles,superTrendIndicator);
        return last100Candles.get(last100Candles.size()-1);
    }

    private List<Candle> startWith100Candles(List<Candle> candleList){
        Collections.reverse(candleList);

        System.out.println(candleList.size());

        List<Candle> lastNCandles = new ArrayList<>();
        for (int j = 0; j < candleList.size(); j++) {
            if (j < 101) {
                lastNCandles.add(candleList.get(j));
            }
        }
        Collections.reverse(lastNCandles);
        return lastNCandles;
    }
    private BarSeries createBarSeries(List<Candle> candleList,SuperTrendIndicator superTrendIndicator) {
        BarSeries series = new BaseBarSeriesBuilder().withName("my_2023_series").build();

        for (Candle candle : candleList) {
            //series.addBar(endTime, 105.42, 112.99, 104.01, 111.42);
            LocalDateTime dateTime = LocalDateTime.parse(candle.date.substring(0, 19));
            ZonedDateTime zonedDateTime = ZonedDateTime.of(dateTime.getYear(), dateTime.getMonthValue(), dateTime.getDayOfMonth(),
                    dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond(), 0, ZoneId.of("Asia/Kolkata"));

            series.addBar(zonedDateTime, candle.open, candle.high, candle.low, candle.close);
        }

        superTrendIndicator.setSeries(series);
        superTrendIndicator.setLength(10); superTrendIndicator.setMultiplier(3.0);
        superTrendIndicator.calculate();
        superTrendIndicator.getSignal(candleList.size() - 1);

        return series;
    }
}
