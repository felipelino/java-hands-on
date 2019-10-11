package com.hands.on.repository;

import com.hands.on.model.Person;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

@Repository
@Component
public interface PersonRepository extends CrudRepository<Person, String> {
    Person findByEmail(String email);
}
