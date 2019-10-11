package com.hands.on.controller;

import com.hands.on.model.Person;
import com.hands.on.repository.PersonRepository;
import com.hands.on.stream.Topics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    private MessageChannel messageChannel;
    private PersonRepository repository;

    @Autowired
    public ApiController(@Qualifier(Topics.INPUT) MessageChannel messageChannel,
                         PersonRepository repository
    ) {
        this.messageChannel = messageChannel;
        this.repository = repository;
    }

    @RequestMapping(path = "/person", method = RequestMethod.POST)
    public ResponseEntity<?> insert(@RequestBody Person person) {
        Message<Person> message =  MessageBuilder.withPayload(person).build();
        boolean isSuccess = this.messageChannel.send(message);
        if(isSuccess) {
            return ResponseEntity.noContent().build();
        }
        else {
            Map json = new HashMap<>();
            json.put("errorMessage", "fail to publish Person in topic");
            return ResponseEntity.unprocessableEntity().body(json);
        }
    }

    @RequestMapping(path = "/person", method = RequestMethod.GET)
    public ResponseEntity<?> get(@RequestParam(required = true) String email) {
         Person person = this.repository.findByEmail(email);
         if(person != null) {
             return ResponseEntity.ok(person);
         }
         else {
             return ResponseEntity.notFound().build();
         }
    }
}
