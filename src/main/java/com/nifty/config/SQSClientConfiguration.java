package com.nifty.config;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Slf4j
@Configuration
public class SQSClientConfiguration {

    private final String awsAccessKeyId = "";
    private final String awsSecretKeyId = "";

    private AmazonSQS client;

    @PostConstruct
    private void initializeAmazonSqsClient() {
        this.client =
                AmazonSQSClientBuilder.standard()
                        .withCredentials(getAwsCredentialProvider())
                        .withRegion(Regions.US_EAST_2)
                        .build();

        log.info("QUEUE Connection SUCCESS !!!!!!!");
    }

    private AWSCredentialsProvider getAwsCredentialProvider() {
        AWSCredentials awsCredentials = new BasicAWSCredentials(awsAccessKeyId, awsSecretKeyId);
        return new AWSStaticCredentialsProvider(awsCredentials);
    }

    public AmazonSQS getClient() {
        return client;
    }
}
