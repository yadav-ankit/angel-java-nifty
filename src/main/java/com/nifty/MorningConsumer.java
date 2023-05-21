package com.nifty;


import io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy;
import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import org.springframework.stereotype.Component;

@Component
public class MorningConsumer {

    @SqsListener(value = "${sqs.morning.queue}", deletionPolicy = SqsMessageDeletionPolicy.DEFAULT)
    public void receiveMessage(String event) {
    }
}
