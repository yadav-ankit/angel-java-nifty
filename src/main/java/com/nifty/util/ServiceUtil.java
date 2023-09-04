package com.nifty.util;

import com.angelbroking.smartapi.SmartConnect;
import com.angelbroking.smartapi.models.Order;
import com.google.gson.*;
import com.nifty.angelbroking.AngelConnector;
import com.nifty.dto.PnlDto;
import com.trading.Index;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ServiceUtil {

    public String niftyLtp;

    @Autowired
    PnlHelper pnlHelper;

    private static HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, "AWSALBAPP-0=_remove_; AWSALBAPP-1=_remove_; AWSALBAPP-2=_remove_; AWSALBAPP-3=_remove_; AWSALB=2W/GV5RHWOnI1eKt8uaZZGlniavOP19H51ajcdACjRFKUYg1pXtMdosTOJYBMUm5+t7gFPQCFoYTeyXiJ3BXMHmdNqTcb7RnnW+S6BJaaay0rQtaJbdMrb8ii5NN; AWSALBAPP-0=_remove_; AWSALBAPP-1=_remove_; AWSALBAPP-2=_remove_; AWSALBAPP-3=_remove_; AWSALBCORS=2W/GV5RHWOnI1eKt8uaZZGlniavOP19H51ajcdACjRFKUYg1pXtMdosTOJYBMUm5+t7gFPQCFoYTeyXiJ3BXMHmdNqTcb7RnnW+S6BJaaay0rQtaJbdMrb8ii5NN");
        headers.add(HttpHeaders.ACCEPT_LANGUAGE, "en,gu;q=0.9,hi;q=0.8");
        headers.add(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate, br");
        headers.add(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/98.0.4758.102 Safari/537.36");
        headers.add(HttpHeaders.CONNECTION, "keep-alive");

        return headers;
    }

    public static double roundFigure(double number) {
        number = number * 10000;
        number = Math.round(number);
        number = number / 10000;

        return number;
    }

    public static void main(String[] args) {
        List<Index> optionsList = new ArrayList<>();
        Index a = new Index();
        a.setLtp("23.33");
        Index b = new Index();
        b.setLtp("12.01");
        Index c = new Index();
        c.setLtp("15.33");
        Index d = new Index();
        d.setLtp("3.23");
        Index e = new Index();
        e.setLtp("18.33");
        optionsList.add(a);
        optionsList.add(b);
        optionsList.add(c);
        optionsList.add(d);
        optionsList.add(e);

        Index ans = null;//getNearestPremiumMatched(optionsList);
    }

    private static String getSingleData(JsonElement angelElement, String byName) {
        return initializeParams(((JsonObject) angelElement).get(byName));
    }

    private static String initializeParams(JsonElement element) {
        return element == null || element instanceof JsonNull ? "" : element.getAsString();
    }

    private static Date formatToUTCTime(String timeStamp) throws ParseException {
        SimpleDateFormat dateParser = new SimpleDateFormat("ddMMMyyyy");
        return dateParser.parse(timeStamp);
    }

    private static boolean isValidExpiry(Date expiry) {

        Date today = new Date();

        long difference_In_Time
                = expiry.getTime() - today.getTime();

        long daysToExpire
                = (difference_In_Time
                / (1000 * 60 * 60 * 24))
                % 365;

        // 0-6 (current week) | 7-13(next week) | 14-20 (next to next week)
        return ((daysToExpire + 1) <= 6);
    }

    public List<Index> intializeSymbolTokenMap(SmartConnect smartConnect) {
        HttpEntity<String> request = new HttpEntity<>(getHeaders());

        String url = "https://margincalculator.angelbroking.com/OpenAPI_File/files/OpenAPIScripMaster.json";

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
        } catch (Exception e) {
            System.out.println("EXCEPTION WHILE INVOKING API KEY REQUEST : " + e);
        }

        return businessLogic(resource, smartConnect);
    }

    private List<Index> businessLogic(String resource, SmartConnect smartConnect) {
        JsonArray angelIndexArray = resource == null ? null :
                StringUtils.isBlank(resource) ?
                        null : new Gson().fromJson(resource, JsonArray.class);

        JsonArray niftyArray = new JsonArray();
        for (JsonElement angelElement : angelIndexArray) {
            if (getSingleData(angelElement, "name").equalsIgnoreCase("NIFTY")
                    && (getSingleData(angelElement, "instrumenttype").equalsIgnoreCase("OPTIDX"))) {
                niftyArray.add(angelElement);
            }
        }

        List<Index> niftyList = new ArrayList<>();
        for (JsonElement ele : niftyArray) {
            Index id = new Index();
            try {
                id.setExpiry(formatToUTCTime(getSingleData(ele, "expiry")));
            } catch (Exception e) {
                e.printStackTrace();
            }
            id.setExpiryString(getSingleData(ele, "expiry"));
            id.setInstrumenttype(getSingleData(ele, "instrumenttype"));
            id.setName(getSingleData(ele, "name"));
            id.setLotsize(getSingleData(ele, "lotsize"));
            id.setSymbol(getSingleData(ele, "symbol"));
            Integer strike = ((int) Double.parseDouble(getSingleData(ele, "strike"))) / 100;
            id.setStrike(strike);
            id.setToken(getSingleData(ele, "token"));
            id.setExchSeg(getSingleData(ele, "exch_seg"));

            niftyList.add(id);
        }

        String currStrikePriceString = null;

        currStrikePriceString = AngelConnector.getNiftyltp(smartConnect);
        this.niftyLtp = currStrikePriceString;

        int currStrikePrice = (currStrikePriceString == null || "".equals(currStrikePriceString))
                ? Integer.parseInt(System.getenv("CURR_STRIKE_PRICE")) : (int) Double.parseDouble(currStrikePriceString);


        // filter out only top 5 & last 5 strike prices 17500 - 18500
        // filter out this week and next week expiry

        String reachAbleStrike = System.getenv("REACHABLE_STRIKES");
        int reachAbleStrikeInt = 500;
        try {
            reachAbleStrikeInt = Integer.parseInt(reachAbleStrike);
        } catch (Exception e) {
            reachAbleStrikeInt = 500;
        }
        int finalReachableStrikeInt = reachAbleStrikeInt;
        List<Index> filteredList = niftyList.stream()
                .filter(t -> Math.abs(t.getStrike() - currStrikePrice) < finalReachableStrikeInt)
                .collect(Collectors.toList());


        List<Index> finalList = new ArrayList<>();
        for (Index ele : filteredList) {
            if (isValidExpiry(ele.getExpiry())) {
                finalList.add(ele);
            }
        }
        return finalList;
    }

    public Index getAtleastPointsAwayFromATM(List<Index> optionsList, String optionType, int min_distance_from_atm) {
        int mini = 100000000;
        Index answerElement = null;
        optionType= "CE";
        for (Index ele : optionsList) {
            String sym = ele.getSymbol();
            if (sym.contains(optionType)) {

                if (optionType.equals("PE") && ele.getStrike() < (int) Double.parseDouble(niftyLtp)) {
                    int kitnaDuur = Math.abs(ele.getStrike() - (int) Double.parseDouble(niftyLtp));
                    if (Math.abs(kitnaDuur - min_distance_from_atm) < mini) {
                        answerElement = ele;
                        mini = Math.abs(kitnaDuur - min_distance_from_atm);
                    }
                } else if (optionType.equals("CE") && ele.getStrike() > (int) Double.parseDouble(niftyLtp)) {
                    int kitnaDuur = Math.abs(ele.getStrike() - (int) Double.parseDouble(niftyLtp));
                    if (Math.abs(kitnaDuur - min_distance_from_atm) < mini) {
                        answerElement = ele;
                        mini = Math.abs(kitnaDuur - min_distance_from_atm);
                    }
                }
            }
        }
        return answerElement;
    }

    public boolean isTimeInBetween(String startTime, String endTime, String checkTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.US);
        LocalTime startLocalTime = LocalTime.parse(startTime, formatter);

        LocalTime endLocalTime = LocalTime.parse(endTime, formatter);
        LocalTime checkLocalTime = LocalTime.parse(checkTime, formatter);

        boolean isInBetween = false;
        if (endLocalTime.isAfter(startLocalTime)) {
            if (startLocalTime.isBefore(checkLocalTime) && endLocalTime.isAfter(checkLocalTime)) {
                isInBetween = true;
            }
        } else if (checkLocalTime.isAfter(startLocalTime) || checkLocalTime.isBefore(endLocalTime)) {
            isInBetween = true;
        }
        return isInBetween;
    }


    public int getDistanceFromATM(DayOfWeek day){

        switch (day){
            case THURSDAY:
                return 200;
            case FRIDAY:
                return 500;
            case MONDAY:
                return 400;
            case TUESDAY:
                return 300;
            case WEDNESDAY:
                return 200;
        }
        return 300;
    }

    public List<PnlDto> fetchExistionPositions(){
        return pnlHelper.existingPositions;
    }

    public List<PnlDto> addIntoPosition(PnlDto pnlDto){
        pnlHelper.existingPositions.add(pnlDto);
        return pnlHelper.existingPositions;
    }

    public double runningPnl(double ltp,String tradingSymbol){
       return  pnlHelper.runningPnl(ltp,tradingSymbol);
    }


}
