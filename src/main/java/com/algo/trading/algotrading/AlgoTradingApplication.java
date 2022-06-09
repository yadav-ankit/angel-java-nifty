package com.algo.trading.algotrading;

import com.angelbroking.smartapi.SmartConnect;
import com.angelbroking.smartapi.http.SessionExpiryHook;
import com.angelbroking.smartapi.models.TokenSet;
import com.angelbroking.smartapi.models.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AlgoTradingApplication {

	@Value("${angel.username}")
	private static String username;

	@Value("${angel.password}")
	private static String password;


	public static void main(String[] args) {

		// Initialize Samart API using clientcode and password.
		SmartConnect smartConnect = new SmartConnect();

		// Provide your api key here
		smartConnect.setApiKey("<api_key>");

		// Set session expiry callback.
		smartConnect.setSessionExpiryHook(new SessionExpiryHook() {
			@Override
			public void sessionExpired() {
				System.out.println("session expired");
			}
		});

		User user = smartConnect.generateSession(username, password);
		System.out.println(user.toString());
		smartConnect.setAccessToken(user.getAccessToken());
		smartConnect.setUserId(user.getUserId());

		// token re-generate

		TokenSet tokenSet = smartConnect.renewAccessToken(user.getAccessToken(),
				user.getRefreshToken());
		smartConnect.setAccessToken(tokenSet.getAccessToken());

		SpringApplication.run(AlgoTradingApplication.class, args);
	}

}
