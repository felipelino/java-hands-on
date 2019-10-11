package com.hands.on.stream;

import com.hands.on.model.Person;
import com.hands.on.repository.PersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.stereotype.Component;

@Component
public class PersonListener {

    private PersonRepository repository;

    @Autowired
    public PersonListener(PersonRepository repository) {
        this.repository = repository;
    }

    @StreamListener(Topics.INPUT)
    public void handlePerson(Person person) {
        repository.save(person);
    }
}
