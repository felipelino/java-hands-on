package com.hands.on.controller;
import com.hands.on.model.Person;
import com.hands.on.repository.PersonRepository;
import com.hands.on.stream.PersonProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    private PersonProducer personProducer;
    private PersonRepository repository;

    @Autowired
    public ApiController(PersonProducer personProducer,
                         PersonRepository repository
    ) {
        this.personProducer = personProducer;
        this.repository = repository;
    }

    @RequestMapping(path = "/person", method = RequestMethod.POST)
    public ResponseEntity<?> insert(@RequestBody Person person) {
        boolean isSuccess = personProducer.produce(person);
        if(!isSuccess) {
            Map json = new HashMap<>();
            json.put("errorMessage", "fail to publish Person in topic");
            return ResponseEntity.internalServerError().body(json);
        }
        return ResponseEntity.noContent().build();
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
