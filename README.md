
# Script de Hands On Java

# Objective

* Java Application with Spring Boot
* REST API
* Persistence Layer Cassandra
* Listening Message from a Kafka Queue
* Logging
* Maven to Build
* Swagger Documentation

# Pre-requisites

* [JDK Java 17](https://jdk.java.net/archive/)
* [IntelliJ Community](https://www.jetbrains.com/idea/download/) as IDE
* [Apache Maven](https://maven.apache.org)  to Build
* [Docker](https://docs.docker.com/docker-for-windows/install/) so we can go up with Cassandra and Kafka

# Application Overview

1. Receive a model by REST API OR receive from a Kafka Topic
1. Listen to the Kafka Topic
1. Persist the data in Cassandra
1. Allow Get from the REST API
1. Documentation exposed by Swagger

# Steps

## External Dependencies

It's out of scope talk about docker. So we are going to have here the `docker-compose.yml` with all dependencies:

* Cassandra
* Kafka
* Zookeeper (because Kafka depends on Zookeeper)

```yaml
version: "3"

services:
  cassandra:
    image: cassandra:3.11
    container_name: cassandra_custom
    environment:
      MAX_HEAP_SIZE: 128M
      HEAP_NEWSIZE: 24M
      CASSANDRA_CLUSTER_NAME: docker cluster
      CASSANDRA_DC: docker
      CASSANDRA_SEEDS: cassandra
    ports:
      - 9042:9042
    restart: unless-stopped

  zookeeper:
    image: zookeeper:3.4.13
    container_name: zookeeper
    restart: unless-stopped

  kafka:
   image: wurstmeister/kafka:2.13-2.8.1
   container_name: kafka
   ports:
    -    "9092:9092"
   environment:
    KAFKA_ADVERTISED_HOST_NAME: localhost
    KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
   volumes:
    - /var/run/docker.sock:/var/run/docker.sock
   restart: unless-stopped
```

*Start Dependencies*

```
docker-compose -f .\docker-compose.yml build
docker-compose -f .\docker-compose.yml up -d
```

## Prepare database

1. Execute the command: `docker exec -it cassandra_custom cqlsh`
1. Copy and Paste the following CQL Script
```cql
CREATE KEYSPACE IF NOT EXISTS mykeyspace WITH REPLICATION = {
   'class' : 'NetworkTopologyStrategy',
   'datacenter1' : 1
  };

CREATE TABLE IF NOT EXISTS mykeyspace.person (
    email text,
    firstName text,
    lastName text,
    yearBirth int,
    PRIMARY KEY ((email))
) WITH compaction = { 'class' :  'LeveledCompactionStrategy'  };
```

## Spring Boot Initializer

* [Search 'Spring boot initializer' in Google](https://letmegooglethat.com/?q=Spring+boot+initializer)
* Or direct Link:  https://start.spring.io/

*Add Spring Dependencies*

* [spring-boot-starter-web](https://www.baeldung.com/spring-boot-starters): Allow application to answer REST HTTP
* [Spring Actuator](https://spring.io/guides/gs/actuator-service/): Adds endpoints that are useful to monitor or debug your application
* [Spring Cloud Stream](https://spring.io/projects/spring-cloud-stream): To publish and listen to kafka topics (or other kind of queues as: Kafka, AWS Kinesis, RabbitMQ)
* [Spring Data Cassandra](https://spring.io/projects/spring-data-cassandra): To access Cassandra

[Click to retrive the same configuration](https://start.spring.io/#!type=maven-project&language=java&platformVersion=3.0.2&packaging=jar&jvmVersion=17&groupId=com.hands.on&artifactId=hands-on&name=hands-on&description=&packageName=com.hands.on&dependencies=web,actuator,cloud-stream,data-cassandra)


*Steps in Spring IO Initializer*

* Download the ZIP File.
* Unzip the file
* `Open as Project` with IntelliJ selecting the `pom.xml`
* Optionally you can set to download dependencies automatically when pom is updated.
* Run the class with annotation `@SpringBootApplication` as a Main application.
* If it fails comment dependency `spring-boot-starter-data-cassandra` in `pom.xml`
* If everything is OK in output log will appear:
```
Started HandsOnApplication in 2.297 seconds (process running for 2.669)
```
* Stop application

*Tip*: Add the two previous CQL scripts to create the Keyspace and other to create the table in the folder `src/main/resources/cql` so you can use them in integration tests.

## About Maven and Edit POM

### Maven Lifecycle

* [Introduction to the Build Lifecycle](http://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html)

> * *validate* - validate if the project is correct and all necessary information is available
> * *compile* - compile the source code of the project
> * *test* - test the compiled source code using a suitable unit testing framework. These tests should not require the code be packaged or deployed
> * *package* - take the compiled code and package it in its distributable format, such as a JAR.
> * *verify* - run any checks on results of integration tests to ensure quality criteria are met
> * *install* - install the package into the local repository, for use as a dependency in other projects locally
> * *deploy* - done in the build environment, copies the final package to the remote repository for sharing with other developers and projects.

### Dependencies

#### Other Dependencies

* [Springdoc Swagger](https://springdoc.org/v2/): To expose our REST API in a friendly view
* [Logstash logback](https://github.com/logstash/logstash-logback-encoder): Library to write log in JSON format compatible with [Logstash](https://www.elastic.co/pt/products/logstash)


#### Indirect Dependencies

* [Logback](https://logback.qos.ch/): Logging library
* [Jackson JSON](https://github.com/FasterXML/jackson): Library to parse JSON to Object and vice-versa.
* [Apache Commons Lang](https://commons.apache.org/proper/commons-lang/): Library with useful methods to manipulate Strings, Collections, Objects

Run `mvn dependency:tree`

This command will show you the direct dependencies and indirect.

```
[INFO] +- ch.qos.logback:logback-classic:jar:1.4.5:compile
[INFO] |  +- ch.qos.logback:logback-core:jar:1.4.5:compile
[INFO] |  \- org.slf4j:slf4j-api:jar:2.0.6:compile
[INFO] +- net.logstash.logback:logstash-logback-encoder:jar:7.2:compile
[INFO] |  \- com.fasterxml.jackson.core:jackson-databind:jar:2.14.1:compile
[INFO] |     +- com.fasterxml.jackson.core:jackson-annotations:jar:2.14.1:compile
[INFO] |     \- com.fasterxml.jackson.core:jackson-core:jar:2.14.1:compile
```

*Tip*: Always before add a new dependency, check if it exists as indirect dependency.

#### Test Dependencies

* [Mockito](https://site.mockito.org/): For tests using mock for dependencies
* [Spring Cloud Stream Test](https://spring.io/blog/2017/10/24/how-to-test-spring-cloud-stream-applications-part-i): To test applications with cloud stream.
* [Spring Boot Starter Test](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-testing.html):  To test spring boot applications.

If you run the command to see the dependency tree you will realize that these dependencies are already there.

#### How to Search and Add Dependencies

* Go to a maven repository like: https://mvnrepository.com/
* Search: 'commons lang'
* Select the desired dependency
* Select the desired (latest stable) version
* Copy XML with maven's dependency

```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.2</version>
</dependency>
```

#### scope

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

- By default the scope is compile. With the spring-boot plugin this dependency will be packed in the application archive (fat jAR).
- With scope `test` the dependency will be only used for test purposes.


### Plugins

* [spring-boot-maven-plugin](https://docs.spring.io/spring-boot/docs/current/reference/html/build-tool-plugins-maven-plugin.html): Provides Spring Boot support in Maven, letting you package executable jar or war archives and run an application “in-place”.

#### How to Add and Configure Plugin

* Read the documentation about the Plugin.
* The documentation normally follows the same pattern.
* Search plugins that can solve an issue about build, example: generate proto-buf classes based in proto file; automatically change version of a code; run an external command or script

### Folder Structure

Convention over configuration

```
/src
    /main
        /java
        /resources
    /test
        /java
        /resources
```

## Setup Logging

As mentioned before we are going to use Logback. If you want know why we choose it among others like: log4j, commons-log; see [Reasons to Switch](http://logback.qos.ch/reasonsToSwitch.html).

*Nice features in our specific configuration:*

* Parameters: `${logAppender:-STDOUT}` , `${logLevel:-INFO}`
* You can override this in VM Options, example: `java -DlogLevel=DEBUG -logAppender=STDOUT -jar application.jar`
* Output in regular format when writing to standard output. Default is JSON format.

*Tip:*

* Always test and re-run your application

### Setup logback.xml

File used by the application:

* Log in the File using JSON format with rolling policy
* Log warning and error messages in standard output
* The file should be in `src/main/resources/logback.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>${logLevel:-ERROR}</level>
        </filter>
        <layout class="ch.qos.logback.classic.PatternLayout">
            <Pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{1} - %msg%n</Pattern>
        </layout>
    </appender>
    <appender name="STDOUT-JSON" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
            <providers>
                <timestamp>
                    <fieldName>Timestamp</fieldName>
                    <pattern>yyyy-MM-dd'T'HH:mm:ss.SSSZZ</pattern>
                </timestamp>
                <logstashMarkers></logstashMarkers>
                <arguments></arguments>
                <!-- provides the fields in the configured pattern -->
                <pattern>
                    <!-- the pattern that defines what to include -->
                    <pattern>
                    {
                        "EventSeverity": "%level",
                        "Thread": "%thread",
                        "Logger": "%logger",
                        "HostName": "${HOSTNAME}",
                        "Message": "%msg"
                    }
                    </pattern>
                </pattern>
            </providers>
        </encoder>
    </appender>
    <appender name="ASYNC-JSON" class="net.logstash.logback.appender.LoggingEventAsyncDisruptorAppender">
        <appender-ref ref="${logAppender:-STDOUT-JSON}" />
    </appender>
    <root level="${logLevel:-INFO}">
        <appender-ref ref="ASYNC-JSON" />
    </root>
</configuration>
``` 

### Setup logback-test.xml

File used when running unit tests:

* Log all messages in standard output in a regular format
* The file should be in `src/test/resources/logback-test.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <layout class="ch.qos.logback.classic.PatternLayout">
            <Pattern>
                %d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n
            </Pattern>
        </layout>
    </appender>
    <root level="${logLevel:-INFO}">
        <appender-ref ref="STDOUT"/>
    </root>
</configuration>
```

### A little bit about Log

* Use async appender: To avoid blocking your application when logging messages
* Standard Out: Use carefully, some infrastructures don't allow developers to see it

## Spring-boot Application

Spring-boot application encapsulates in a JAR file the server, libraries and the application itself.
By default, many things happen:
* Tomcat Server to run your application
* Port is 8080
* Automatic loading of dependencies when found in classpath: Cassandra, MongoDB, Kafka, among others
* But, you can override all the things as you wish

## Spring Framework

> The Spring Framework provides a comprehensive programming and configuration model for modern Java-based enterprise applications - on any kind of deployment platform.

> A key element of Spring is infrastructural support at the application level: Spring focuses on the    "plumbing" of enterprise applications so that teams can focus on application-level business logic, without unnecessary ties to specific deployment environments.

* Inversion of Control (IoC)
* Dependency Injection
* Libraries and more libraries

## Actuator Endpoints

Try to access the endpoint: http://localhost:8080/actuator/health

Now, enable Actuator endpoints:

Edit file `application.properties`:
```properties
management.endpoints.enabled-by-default=true
management.endpoint.shutdown.enabled=false
management.endpoints.web.exposure.include=*
management.endpoint.health.show-details=always
management.info.defaults.enabled=true
management.endpoint.env.show-values=always
```

Run the application again and access the endpoint: http://localhost:8080/actuator/health

Expected return:
```json
{
	"status": "UP",
	"components": {
		"cassandra": {
			"status": "UP",
			"details": {
				"version": {
					"major": 3,
					"minor": 11,
					"patch": 14,
					"dsepatch": -1
				}
			}
		},
		"ping": {
			"status": "UP"
		}
	}
}
```


Actuator provides many helpful endpoints, such as:

* env
* beans
* health
* info
* loggers
* mappings
* threaddump

You can add and customize endpoints, details can be found here: [Spring Boot Actuator: Production-ready features](https://docs.spring.io/spring-boot/docs/current/reference/html/production-ready-endpoints.html)

## Enable Swagger

[Here](https://springdoc.org/v2/) you have a good reference to setup your Swagger.

You can access http://localhost:8080/swagger-ui.html to see all the links already created by your application.

If you want show the    "actuator" endpoints you can enable it
```properties
springdoc.show-actuator=true
```

## The Model

In the [reference](https://docs.spring.io/spring-data/cassandra/docs/4.0.1/reference/html/#cassandra.repositories) documentation
you will find all the possibilities to use and configure how the application will work with Cassandra.

```java
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

@Table("person")
public class Person {
    @PrimaryKeyColumn(type = PrimaryKeyType.PARTITIONED)
    private String email;

    @Column
    private String firstName;

    @Column
    private String lastName;

    @Column
    private int yearBirth;
    
    // Getters and Setters
} 
```

## The Repository

The Spring will automatically create an instance of this Interface and implement it's methods.
Everything is convention over configuration. So the method's name tell him to use the Field `email` from the `Person` entity.

```java
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonRepository extends CrudRepository<Person, String> {
    Person findByEmail(String email);
}
```

### Setup Cassandra

Followin the [reference](https://docs.spring.io/spring-data/cassandra/docs/4.0.1/reference/html/#cassandra.connectors), create the following class

```java
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.config.CqlSessionFactoryBean;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;

@EnableCassandraRepositories(basePackages = "com.hands.on") // Be sure that you set your package here
@Configuration
public class CassandraConfig {

    @Bean
    public CqlSessionFactoryBean session(@Value("${cassandra.contact-points}") String contactPoints,
                                         @Value("${cassandra.port}") int port,
                                         @Value("${cassandra.keyspace-name}") String keyspace,
                                         @Value("${cassandra.local-datacenter}") String localDatacenter) {

        CqlSessionFactoryBean session = new CqlSessionFactoryBean();
        session.setContactPoints(contactPoints);
        session.setPort(port);
        session.setLocalDatacenter(localDatacenter);
        session.setKeyspaceName(keyspace);
        return session;
    }
}
```

Edit/add file: `application.yaml` in `resources` folder.

```yaml
cassandra:
 contact-points: localhost
 port: 9042
 keyspace-name: mykeyspace
 local-datacenter: datacenter1
```

You can override and compose configurations using:
* environment variables
* external files
* other files in classpath
* default values in code

Check the documentation [Externalized Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html).

## The Kafka

* We are using Spring Cloud Stream that abstracts the implementation of the stream's solution.
* So we should check the [documentation](https://docs.spring.io/spring-cloud-stream/docs/current/reference/html/)
  to know how it works and how we should configure our application.
* Read about the [The Binder Abstraction](https://docs.spring.io/spring-cloud-stream/docs/current/reference/html/spring-cloud-stream.html#spring-cloud-stream-overview-binder-abstraction)
  so we know the configuration that doesn't rely on implementation.
* And read about the [Apache Kafka Binder](https://cloud.spring.io/spring-cloud-stream-binder-kafka/spring-cloud-stream-binder-kafka.html#_apache_kafka_binder)
  which is the one that we will use.

After read about the _Kafka Binder_ we notice that we need to add this dependency:
```xml
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-stream-binder-kafka</artifactId>
</dependency>
```

### Binding our topic

Since the version 3.x the binding is automatically done trough functional programming link the name of the function and the configuration.
So pay attention to the binding names `consume-in-0` and `produce-out-0`. The methods should have the name `consume` and `produce`.
The `in` and `out` is to indicate if is to consume or produce. The cardinal `0` is because we can have multiple consumers and producers to the same topic.
```yaml

spring.cloud.stream.bindings:
  produce-out-0:
    destination: person-topic
    contentType: application/json
  consume-in-0:
    destination: person-topic
```

### Listen the Topic

Our listener is attached to our topic and should persist the data in the Cassandra database.

```java
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
```

### Producer to the topic

This [documentation](https://docs.spring.io/spring-cloud-stream/docs/3.1.0/reference/html/spring-cloud-stream.html#_sending_arbitrary_data_to_an_output_e_g_foreign_event_driven_sources) shows how to produce to any topic.

```java
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
```

### Configuring our Kafka topic

As you read in the [related documentation](https://cloud.spring.io/spring-cloud-stream-binder-kafka/spring-cloud-stream-binder-kafka.html#_configuration_options) you can override the default configuration and:

* set the content-type of the message in the topic
* the topic name
* concurrency
* auto-commit
* retries
* many others

You only need to add a new file in the resources folder: `application.yaml` or edit the `application.properties`.

```yaml
spring.cloud.stream.bindings:
  produce-out-0:
    destination: person-topic
    contentType: application/json
  consume-in-0:
    destination: person-topic
    group: handsonapp
    contentType: application/json
    consumer:
      partitioned: true
      concurrency: 2
      autoRebalanceEnabled: true
      autoCommitOffset: true
      startOffset: earliest
      max-attempts: 3
``` 

The contentType `application/json` will serialize the object using an instance of `ObjectMapper` to JSON.

## Controller

```java
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
```
___

# Unit and Integration Tests

## Definitions

### Integration Test

Your application is like a BOX and you only deal with the input and output. In our example the input and output is the REST API.

### Unit Test

The test is about the piece inside your application, you are going to test in method level, looking:
* Input and output of the method
* Or behaviour, if the method calls the service, repository with the expected parameters.

We are going to use JUnit 5 to run our tests. You can read the [User Guide](https://junit.org/junit5/docs/current/user-guide).

## Unit tests with Mockito

Notes:
* Try to avoid running with Spring Context, run only with Junit instead
* With Mockito we can: verify expectations, capture arguments or return something
* Instantiate your class and mock dependencies
* Use constructor to know all dependencies

```java
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

```

## Prepare Integration Tests

### Cassandra

We are going to run the Integration Tests    "against" the docker instance of Cassandra.

Add a file `test/resources/application-test.properties` to setup the connection with the cassandra:
```properties
cassandra.contact-points=localhost
cassandra.port=9042
cassandra.local-datacenter=datacenter1
```

### Helpful dependencies for test

Add to the `pom.xml`:

```xml
<!-- Has the FileUtils, help us to load Files -->
<dependency>
    <groupId>org.apache.ant</groupId>
    <artifactId>ant</artifactId>
    <version>1.10.13</version>
    <scope>test</scope>
</dependency>
```

### Integration test for Controller

* Start connection with Cassandra using `@BeforeTestClass`
* Stop connection with Cassandra using `@AfterTestClass`
* `@SpringBootTest` to boot application
* `@TestPropertySource` to override configuration for tests
* `@AutoConfigureMockMvc` and `MockMvc` to perform HTTP operations

```java
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.datastax.oss.driver.api.core.CqlSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.tools.ant.util.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.util.Properties;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ContextConfiguration(classes = {HandsOnApplication.class})
@AutoConfigureMockMvc // When application boots the test will instantiate a MockMvc
@TestPropertySource("classpath:application-test.properties") // Override configuration only for tests
@ExtendWith(SpringExtension.class)
@SpringBootTest	// The application will startup
public class HandsOnApplicationTests {

	@Autowired
	private MockMvc mockMvc; // This object allow us to perform HTTP operations in our application

	@Autowired
	private PersonRepository repository;

	@BeforeAll
	public static void setupCassandra() throws Exception {

		// Connect to Cassandra
		Properties prop = new Properties();
		prop.load(HandsOnApplicationTests.class.getClassLoader().getResourceAsStream("application-test.properties"));
		String contactPoints = prop.getProperty("cassandra.contact-points");
		int port = Integer.parseInt(prop.getProperty("cassandra.port"));
		String localDatacenter = prop.getProperty("cassandra.local-datacenter");

		CqlSession session = CqlSession.builder()
				.addContactPoint(new InetSocketAddress(contactPoints, port))
				.withLocalDatacenter(localDatacenter)
				.build();

		// Create resources at Database
		String cqlScript1 = FileUtils.readFully(new InputStreamReader(HandsOnApplicationTests.class.getClassLoader().getResourceAsStream("cqls/create_keyspace.cql")));
		session.execute(cqlScript1);
		String cqlScript2 = FileUtils.readFully(new InputStreamReader(HandsOnApplicationTests.class.getClassLoader().getResourceAsStream("cqls/create_table.cql")));
		session.execute(cqlScript2);

		session.close();
	}


	@Test
	public void postAndGetPerson() throws Exception {

		// Prepare
		String email = "edsger.dijkstra@company.com";
		Person p = new Person();
		p.setEmail(email);
		this.repository.delete(p);

		String contentToPost = "{\"email\": \""+email+"\", \"firstName\": \"Edsger\", \"lastName\": \"Dijkstra\", \"yearBirth\": 1930 }";

		// Act/Assert - POST
		MvcResult mvcResult = this.mockMvc.perform(post("/api/person")
						.content(contentToPost).contentType("application/json"))
				.andDo(print())
				.andExpect(status().isNoContent()).andReturn();

		Thread.sleep(2000);

		// Act/Assert - GET
		mvcResult = this.mockMvc.perform(get("/api/person")
						.param("email", email)
						.accept("application/json"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(content().json(contentToPost, true))
				.andReturn();
	}
}
```

### Integration test for Kafka

Imagine that our topic is public and other applications can publish to it. So our application should persist the data too.

Reference: https://docs.spring.io/spring-cloud-stream/docs/current/reference/html/spring-cloud-stream.html#_testing

```java
@Import(TestChannelBinderConfiguration.class) // Enable kafka binder for test
public class HandsOnApplicationTests {
 // ...

	/* Tests for Kafka */
	@Autowired
	private InputDestination input; // it's a kind of    "mock" for the topics

	@Autowired
	private PersonProducer personProducer;

	@Autowired
	private ObjectMapper objectMapper; // The default instance created by Spring

	@Test
	public void publishMessageToTopicAndCheckRepository() throws Exception {

		// Prepare
		String email = "james.watt@company.com";
		Person p = new Person();
		p.setEmail(email);
		this.repository.delete(p);

		String json = "{\"email\": \""+email+"\", \"firstName\": \"James\", \"lastName\": \"Watt\", \"yearBirth\": 1736 }";

		// Act
		this.input.send(MessageBuilder.withPayload(json).build(),"person-topic");

		// Assert
		Person person = this.repository.findByEmail(email);
		Assertions.assertNotNull(person);
		String jsonFromRepository = this.objectMapper.writeValueAsString(person);
		JSONAssert.assertEquals(json, jsonFromRepository, true);
	}

	@Test
	public void produceMessageAndCheckRepository() throws Exception {

		// Prepare
		Person person = new Person();
		person.setEmail("john.doe@acme.com");
		person.setFirstName("John");
		person.setLastName("Doe");
		person.setYearBirth(2023);

		this.repository.delete(person);

		// Act
		this.personProducer.produce(person);

		// Assert
		Person findPerson = this.repository.findByEmail(person.getEmail());
		Assertions.assertNotNull(findPerson);
		Assertions.assertEquals(person, findPerson);
	}
}
```
___

# Pack, Deliver and Run

1. `mvn clean package`
1. `java -jar target/hands-on-0.0.1-SNAPSHOT.jar`
1. Check if application booted in: `http://localhost:8080/actuator/info` , should return HTTP 2xx.
1. You can access the Swagger Documentation `http://localhost:8080/swagger-ui/index.html`
1. POST and GET a sample
```shell script
curl -v -X POST 'http://localhost:8080/api/person' -H 'Content-Type: application/json' -d '{"email": "ada.lovelace@gmail.com", "firstName": "Augusta", "lastName": "King", "yearBirth": 1815}'
curl -v -X GET 'http://localhost:8080/api/person?email=ada.lovelace@gmail.com' -H 'accept: application/json'
```

___

# More

## Spring useful libraries

The list here is a subset of libraries and guides useful in any project:

* [Spring boot Starter Cache](https://spring.io/guides/gs/caching/): Use cache in-memory abstracting the implementation and uses annotation to cache objects.
* [Asynchronous Methods](https://spring.io/guides/gs/async-method/): How to use spring with `@Async` annotation to work with asynchronous methods.
* [Consuming a REST APIs](https://spring.io/guides/gs/consuming-rest/): How to consume REST APIs.
* [Scheduling Tasks](https://spring.io/guides/gs/scheduling-tasks/): Schedule execution of a method using something like a `cron`.
* [Spring Retry](https://github.com/spring-projects/spring-retry): How to use spring to retry the execution of a method in case of a specific condition (some errors).
* [ConditionalOnProperty](https://docs.spring.io/spring-boot/docs/current/api/org/springframework/boot/autoconfigure/condition/ConditionalOnProperty.html): Instantiate spring bean conditionally.

## Separate Unit and Integration Tests

* [How to Split JUnit Tests in a Continuous Integration Environment](https://semaphoreci.com/community/tutorials/how-to-split-junit-tests-in-a-continuous-integration-environment)

 ___

# Do it by yourself

* Add cache to avoid retrieving the same data from cassandra.

* Force cache expiration whenever the data is updated

* Allow to enable/disable Swagger by configuration
