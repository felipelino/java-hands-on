package com.hands.on.stream;

import com.hands.on.model.Person;
import com.hands.on.repository.PersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
public class PersonListener {

    private PersonRepository repository;

    @Autowired
    public PersonListener(PersonRepository repository) {
        this.repository = repository;
    }

    // the methods name "consume" matches the "consumer-in-0" in application.yaml
    @Bean
    Consumer<Person> consume() {
        return person -> repository.save(person);
    }
}