package com.nifty.angelbroking;

import com.angelbroking.smartapi.SmartConnect;
import com.angelbroking.smartapi.http.SessionExpiryHook;
import com.angelbroking.smartapi.http.exceptions.SmartAPIException;
import com.angelbroking.smartapi.models.Order;
import com.angelbroking.smartapi.models.OrderParams;
import com.angelbroking.smartapi.models.User;
import com.angelbroking.smartapi.utils.Constants;
import de.taimos.totp.TOTP;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Hex;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

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

    public static String getNiftyltp(SmartConnect smartConnect) {
        JSONObject indexObj = smartConnect.getLTP("NSE", "NIFTY", "99926000");
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

    /** Place order. */
    public static Order placeOrder(SmartConnect smartConnect,OrderParams orderParams) throws SmartAPIException, IOException {

        orderParams.variety = "NORMAL";
       // orderParams.quantity = 1;
      //  orderParams.symboltoken = "3045";
        orderParams.exchange = Constants.EXCHANGE_NFO;
        orderParams.ordertype = Constants.ORDER_TYPE_MARKET;
    //    orderParams.tradingsymbol = "SBIN-EQ";
        orderParams.producttype = Constants.DURATION_DAY;
        orderParams.duration = Constants.DURATION_DAY;
       // orderParams.transactiontype = Constants.TRANSACTION_TYPE_SELL;
       // orderParams.price = 122.2;
        orderParams.squareoff = "0";
        orderParams.stoploss = "0";

        Order order = smartConnect.placeOrder(orderParams, Constants.VARIETY_NORMAL);

        return order;
    }


}
