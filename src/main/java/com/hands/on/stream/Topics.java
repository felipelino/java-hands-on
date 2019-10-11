package com.hands.on.stream;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.messaging.SubscribableChannel;

public interface Topics {

    String INPUT = "person-in";
    @Input(Topics.INPUT)
    SubscribableChannel input();
}
