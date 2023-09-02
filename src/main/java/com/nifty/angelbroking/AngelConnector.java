package com.nifty.angelbroking;

import com.angelbroking.smartapi.SmartConnect;
import com.angelbroking.smartapi.http.SessionExpiryHook;
import com.angelbroking.smartapi.models.User;
import de.taimos.totp.TOTP;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Hex;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

@Service
public class AngelConnector {

    public static SmartConnect connectWithAngel() {
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

        User user = smartConnect.generateSession("A844782", "Any@1801*#@@", getTOTPCode("1ff1b793-a7b1-4f4a-b8c5-e6e5c7837a31"));

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

    public static String getNiftyltp(SmartConnect smartConnect) {
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


}
