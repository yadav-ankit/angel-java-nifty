package com;

import com.angelbroking.smartapi.SmartConnect;
import com.angelbroking.smartapi.http.SessionExpiryHook;
import com.angelbroking.smartapi.models.User;
import com.nifty.MorningService;
import com.nifty.dto.Candle;
import com.nifty.util.ServiceUtil;
import de.taimos.totp.TOTP;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Hex;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@SpringBootApplication(scanBasePackages = {"com"}, exclude = {SecurityAutoConfiguration.class})
public class AlgoTradingApplication implements ApplicationRunner {

    @Autowired
    MorningService morningService;


    @Autowired
    ServiceUtil serviceUtil;

    @Value("${morning.url}")
    private String url;

    //  mvn spring-boot:run -Dspring-boot.run.arguments=--morning.url=http://localhost:8090/tree

    public static void main(String[] args) throws Exception {
        SpringApplication.run(AlgoTradingApplication.class, args);

    }

    public void execute(List<Candle> candleList) {
        SmartConnect smartConnect = connectWithAngel();

        int n = 10;

        /*
        every 10 seconds  start with count = 1...6 times in 1 min   ...... 30 times in 5 mins
         when count becomes 30  ...do this
             close_curr = ltp = open_next
            curr.high = high;
            curr.low = low
         */

        AtomicLong startTime = new AtomicLong(System.currentTimeMillis());
        final boolean[] isStarted = {false};

        AtomicReference<Double> open = new AtomicReference<>();

        if (!candleList.isEmpty() && !isStarted[0]) {
            open.set(candleList.get(candleList.size() - 1).getClose());
        }

        AtomicReference<Double> close = new AtomicReference<>();
        AtomicReference<Double> low = new AtomicReference(999999.23);
        AtomicReference<Double> high = new AtomicReference(-999.23);


        Runnable periodicRunnable = () -> {
            try {
                String ltp = getNiftyltp(smartConnect);

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
                    candleList.get(size-1).setTr(serviceUtil.extractTrueRange(candleList,n));
                    serviceUtil.setAverageTrueRange(candleList,n,size);

                    startTime.set(System.currentTimeMillis());
                    open.set(niftyLtp);

                    if (!candleList.isEmpty()) {
                        Candle printt = candleList.get(candleList.size() - 1);
                        System.out.println("close " + printt.getClose());
                        System.out.println("open " + printt.getOpen());
                        System.out.println("high " + printt.getHigh());
                        System.out.println("low " + printt.getLow());
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(periodicRunnable, 0, 10, TimeUnit.SECONDS);

    }

    private static SmartConnect connectWithAngel() {
        SmartConnect smartConnect = new SmartConnect();

        // Provide your api key here
        smartConnect.setApiKey("a5Dd7nxn");

        // Set session expiry callback.
        smartConnect.setSessionExpiryHook(new SessionExpiryHook() {
            @Override
            public void sessionExpired() {
                System.out.println("session expired");
            }
        });

        User user = smartConnect.generateSession("A844782", "9725", getTOTPCode("4TLPUQ4SFZKRXMEYSFRBKGKFOY"));

        String feedToken = user.getFeedToken();
        System.out.println(feedToken);

        System.out.println(user.toString());
        smartConnect.setAccessToken(user.getAccessToken());
        smartConnect.setUserId(user.getUserId());
//
//        // token re-generate
//
//        TokenSet tokenSet = smartConnect.renewAccessToken(user.getAccessToken(),
//                user.getRefreshToken());
//        smartConnect.setAccessToken(tokenSet.getAccessToken());

        return smartConnect;
    }

    public static String getTOTPCode(String secretKey) {
        Base32 base32 = new Base32();
        byte[] bytes = base32.decode(secretKey);
        String hexKey = Hex.encodeHexString(bytes);
        return TOTP.getOTP(hexKey);
    }

    private static String getNiftyltp(SmartConnect smartConnect) {
        JSONObject indexObj = smartConnect.getLTP("NSE", "NIFTY", "26000");
        // int niftyLtp = Integer.parseInt(indexObj.get("ltp").toString().substring(0, 5));

        double niftyLtp = Double.parseDouble(indexObj.get("ltp").toString());


        /*
        int mod = (niftyLtp) % 100;
        System.out.println("nifty ltp is " + niftyLtp);
        if (mod > 50) {
            niftyLtp = niftyLtp + (100 - mod);
        } else {
            niftyLtp = niftyLtp - (mod);
        }

         */
        return String.valueOf(niftyLtp);
    }

    @Override
    public void run(ApplicationArguments args) {
        System.out.println("url: " + url);
        List<Candle> candleList = morningService.createCandlesData(url);

        execute(candleList);
    }


}
