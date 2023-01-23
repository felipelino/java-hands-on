package com.hands.on.stream;
import com.hands.on.model.Person;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

@Component
public class PersonProducer {

    private StreamBridge streamBridge;

    @Autowired
    PersonProducer(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    public boolean produce(Person person) {
        // "produce-out-0" is the same on application.yaml
        return this.streamBridge.send("produce-out-0", person);
    }
}