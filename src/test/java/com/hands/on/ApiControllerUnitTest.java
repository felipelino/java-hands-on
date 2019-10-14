package com.hands.on;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hands.on.controller.ApiController;
import com.hands.on.model.Person;
import com.hands.on.repository.PersonRepository;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Matchers.eq;

@RunWith(JUnit4.class)
public class ApiControllerUnitTest {

    private ApiController apiController;
    private MessageChannel messageChannel;
    private PersonRepository personRepository;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void initMocks() {
        this.messageChannel = Mockito.mock(MessageChannel.class);
        this.personRepository = Mockito.mock(PersonRepository.class);
        this.apiController = new ApiController(this.messageChannel, this.personRepository);
    }

    @Test
    public void getPerson() throws Exception {
        String email = "john.doe@company.com";

        // Act
        ResponseEntity responseEntity = this.apiController.get(email);

        // Assert
        Assert.assertNotNull(responseEntity);
        Assert.assertEquals(404, responseEntity.getStatusCodeValue());
        Mockito.verify(this.personRepository, Mockito.times(1)).findByEmail(eq(email));
        Mockito.verifyNoMoreInteractions(this.personRepository);
        Mockito.verifyZeroInteractions(this.messageChannel);
    }

    @Test
    public void postPerson() throws Exception {

        String email = "john.doe@company.com";
        String json = "{\"email\": \""+email+"\", \"firstName\": \"Edsger\", \"lastName\": \"Dijkstra\", \"yearBirth\": 1930 }";
        Person person = this.objectMapper.readValue(json, Person.class);

        Mockito.when(this.messageChannel.send(any(Message.class))).thenReturn(true);

        // Act
        ResponseEntity responseEntity = this.apiController.insert(person);

        // Assert
        Assert.assertNotNull(responseEntity);
        Assert.assertEquals(204, responseEntity.getStatusCodeValue());
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        Mockito.verify(this.messageChannel, Mockito.times(1)).send(captor.capture());
        Mockito.verifyNoMoreInteractions(this.messageChannel);
        Mockito.verifyZeroInteractions(this.personRepository);

        Message message = captor.getValue();
        Person personInPayload = (Person) message.getPayload();
        Assert.assertEquals(person, personInPayload); // Only will work if we implements the method Equals in class Person
        JSONAssert.assertEquals(json, this.objectMapper.writeValueAsString(personInPayload), true);
    }
}


