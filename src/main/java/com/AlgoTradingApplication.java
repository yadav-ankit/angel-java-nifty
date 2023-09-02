package com;

import com.nifty.MorningService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;


@Slf4j
@SpringBootApplication(scanBasePackages = {"com"}, exclude = {SecurityAutoConfiguration.class})
public class AlgoTradingApplication implements ApplicationRunner {

    @Autowired
    MorningService morningService;

    //  mvn spring-boot:run -Dspring-boot.run.arguments=--morning.url=http://localhost:8090/tree

    public static void main(String[] args) throws Exception {
        SpringApplication.run(AlgoTradingApplication.class, args);

    }

    @Override
    public void run(ApplicationArguments args) {
        morningService.performIntradayTrading();
    }
}
