package com.nifty.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class SQSClientConfiguration {
    private final String awsAccessKeyId = "AKIAY6BZCFHO6WU7FG5X";
    private final String awsSecretKeyId = "wdhwo+vlTlSq28dAYsiQ8qs+csxnuYRD5vJH1XNN";

    @Bean
    public AmazonSQS amazonSQSClient() {
        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(awsAccessKeyId, awsSecretKeyId);
        return AmazonSQSClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .withRegion("us-east-2").build();
    }
}
