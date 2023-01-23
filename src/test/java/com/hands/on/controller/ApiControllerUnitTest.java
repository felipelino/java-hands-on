package com.hands.on.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hands.on.model.Person;
import com.hands.on.repository.PersonRepository;
import com.hands.on.stream.PersonProducer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.http.ResponseEntity;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

public class ApiControllerUnitTest {

    private ApiController apiController;
    private PersonProducer personProducer;
    private PersonRepository personRepository;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void initMocks() {
        this.personProducer = Mockito.mock(PersonProducer.class);
        this.personRepository = Mockito.mock(PersonRepository.class);
        this.apiController = new ApiController(this.personProducer, this.personRepository);
    }

    @Test
    public void getPerson() throws Exception {
        String email =    "john.doe@company.com";

        // Act
        ResponseEntity responseEntity = this.apiController.get(email);

        // Assert
        Assertions.assertNotNull(responseEntity);
        Assertions.assertEquals(404, responseEntity.getStatusCodeValue());
        Mockito.verify(this.personRepository, Mockito.times(1)).findByEmail(eq(email));
        Mockito.verifyNoMoreInteractions(this.personRepository);
        Mockito.verifyNoMoreInteractions(this.personProducer);
    }

    @Test
    public void postPerson() throws Exception {

        String email = "john.doe@company.com";
        String json = "{\"email\": \""+email+"\", \"firstName\": \"Edsger\", \"lastName\": \"Dijkstra\", \"yearBirth\": 1930 }";
        Person person = this.objectMapper.readValue(json, Person.class);

        Mockito.when(this.personProducer.produce(any(Person.class))).thenReturn(true);

        // Act
        ResponseEntity responseEntity = this.apiController.insert(person);

        // Assert
        Assertions.assertNotNull(responseEntity);
        Assertions.assertEquals(204, responseEntity.getStatusCodeValue());
        ArgumentCaptor<Person> captor = ArgumentCaptor.forClass(Person.class);
        Mockito.verify(this.personProducer, Mockito.times(1)).produce(captor.capture());
        Mockito.verifyNoMoreInteractions(this.personProducer);
        Mockito.verifyNoMoreInteractions(this.personRepository);

        Person personInPayload = captor.getValue();
        Assertions.assertEquals(person, personInPayload); // Only will work if we implement the method Equals in class Person
        JSONAssert.assertEquals(json, this.objectMapper.writeValueAsString(personInPayload), true);
    }

    @Test
    public void postPersonWhenFailsToProduce() throws Exception {

        String email = "john.doe@company.com";
        String json = "{\"email\": \""+email+"\", \"firstName\": \"Edsger\", \"lastName\": \"Dijkstra\", \"yearBirth\": 1930 }";
        Person person = this.objectMapper.readValue(json, Person.class);

        Mockito.when(this.personProducer.produce(any(Person.class))).thenReturn(false);

        // Act
        ResponseEntity responseEntity = this.apiController.insert(person);

        // Assert
        Assertions.assertNotNull(responseEntity);
        Assertions.assertEquals(500, responseEntity.getStatusCodeValue());
        Mockito.verify(this.personProducer, Mockito.times(1)).produce(any(Person.class));
        Mockito.verifyNoMoreInteractions(this.personProducer);
        Mockito.verifyNoMoreInteractions(this.personRepository);
    }
}
