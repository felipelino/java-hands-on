
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

* [JDK Java 1.8](https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
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

Is out of scope talk about docker. So we are going to have here the `docker-compose.yml` with all dependencies:

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
    networks:
      - hands-on-net

  zookeeper:
    image: zookeeper:3.4.13
    container_name: zookeeper
    networks:
      - hands-on-net
    restart: unless-stopped

  kafka:
    image: wurstmeister/kafka:2.12-2.0.1
    container_name: kafka
    networks:
      - hands-on-net
    ports:
      - "9092:9092"
    environment:
      KAFKA_ADVERTISED_HOST_NAME: localhost
      KAFKA_CREATE_TOPICS: "test:1:1"
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
    restart: unless-stopped

networks:
  hands-on-net:
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
CREATE KEYSPACE mykeyspace WITH REPLICATION = {
   'class' : 'NetworkTopologyStrategy',
   'datacenter1' : 1
  } ;

CREATE TABLE IF NOT EXISTS mykeyspace.person (
    email text,
    firstName text,
    lastName text,
    yearBirth int,
    PRIMARY KEY ((email))
) WITH compaction = { 'class' :  'LeveledCompactionStrategy'  };
```

## Spring Boot Initializer

* [Search 'Spring boot initializer' in Google](https://lmgtfy.com/?q=Spring+Boot+Initializer)
* Or direct Link:  https://start.spring.io/

*Add Spring Dependencies*

* [spring-boot-starter-web](https://www.baeldung.com/spring-boot-starters): Allow application to answer REST HTTP 
* [Spring Actuator](https://spring.io/guides/gs/actuator-service/): Add endpoints useful to debug application
* [Spring Cloud Stream](https://spring.io/projects/spring-cloud-stream): To publish and listen kafka topics (or other kind of queues as: Kafka, AWS Kinesis, RabbitMQ)
* [Spring Data Cassandra](https://spring.io/projects/spring-data-cassandra): To access Cassandra


*Steps in Spring IO Initializer*

* Download the ZIP File.
* Unzip the file
* `Open as Project` with IntelliJ selecting the `pom.xml`
* Optionally you can set to download dependencies automatically when pom is updated.
* Run the class with annotation `@SpringBootApplication` as a Main application.
* If fail comment dependency spring-cloud-stream in `pom.xml`
* If everything is OK in output log will appear:
```
Started HandsOnApplication in 7.079 seconds (JVM running for 8.001)
```
* Stop application

*Tip*: Add the two previous CQL scripts to create the Keyspace and other to create the table in the folder `src/main/resources/cql` so you can use them in integration tests.

## About Maven and Edit POM

### Maven Lifecycle

* [Introduction to the Build Lifecycle](http://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html)

> * *validate* - validate the project is correct and all necessary information is available
> * *compile* - compile the source code of the project
> * *test* - test the compiled source code using a suitable unit testing framework. These tests should not require the code be packaged or deployed
> * *package* - take the compiled code and package it in its distributable format, such as a JAR.
> * *verify* - run any checks on results of integration tests to ensure quality criteria are met
> * *install* - install the package into the local repository, for use as a dependency in other projects locally
> * *deploy* - done in the build environment, copies the final package to the remote repository for sharing with other developers and projects.

### Dependencies

#### Other Dependencies

* [Springfox Swagger](https://springfox.github.io/springfox/docs/current/#springfox-swagger-ui): To expose our REST API in a friendly view 
* springfox-swagger2
* [Logback](https://logback.qos.ch/): Library to log
* [Logstash logback](https://github.com/logstash/logstash-logback-encoder): Library to write log in JSON format compatible with [Logstash](https://www.elastic.co/pt/products/logstash)
* [Jackson JSON](https://github.com/FasterXML/jackson): Library to parse JSON to Object and vice-versa.
* [Apache Commons Lang](https://commons.apache.org/proper/commons-lang/): Library with useful methods to manipulate Strings, Collections, Objects

#### Test Dependencies

* [Cassandra Unit Spring](https://github.com/jsevellec/cassandra-unit/wiki/Spring-for-Cassandra-unit): For unit/integration tests with cassandra
* [Mockito](https://site.mockito.org/): For tests using mock for dependencies
* [Spring Cloud Stream Test](https://spring.io/blog/2017/10/24/how-to-test-spring-cloud-stream-applications-part-i): To test applications with cloud stream.
* [Spring Boot Starter Test](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-testing.html):  To test spring boot applications.

#### How to Search and Add Dependencies

* Go to a maven repository like: https://mvnrepository.com/
* Search: 'commons lang'
* Select the desired dependency
* Select the desired (latest stable) version
* Copy XML with maven's dependency

```xml
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-lang3</artifactId>
    <version>3.9</version>
</dependency>
```

#### scope

```xml
<dependency>
    <groupId>org.cassandraunit</groupId>
    <artifactId>cassandra-unit-spring</artifactId>
    <version>3.11.2.0</version>
    <scope>test</scope>
</dependency>
```
  
- By default the scope is compile. With the spring-boot plugin this dependency will be together with the application (fat jAR).
- In Scope `test` the dependency will be only used for tests purpose.  
  

### Plugins

* [spring-boot-maven-plugin](https://docs.spring.io/spring-boot/docs/current/reference/html/build-tool-plugins-maven-plugin.html): Provides Spring Boot support in Maven, letting you package executable jar or war archives and run an application “in-place”. 

#### How to Add and Configure Plugin

* Read the documentation about the Plugin. 
* The documentation normally follow the same pattern.
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

As said before we are going to use Logback. If you want know why we choose it among others like: log4j, commons-log; see [Reasons to Switch](http://logback.qos.ch/reasonsToSwitch.html). 

*Nice features in our specific configuration:*

* Parameters: `${logStdOutLevel:-WARN}` , `${logLevel:-INFO}`, `${logFilePath:-logs}`, `${logFileName:-application}`
* You can override this in VM Options, example: `java -DlogLevel=DEBUG -DlogStdOutLevel=INFO -jar application.jar`
* Output in JSON format when writing to a File
* Output in regular format in standard output

*Tip:*

* Always test and re-run your application 

### Setup logback.xml

File used by the application:

* Log in the File using JSON format with rolling policy
* Log warning and error messages in standard output
* the file should be in `src/main/resources/logback.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="STDOUT-ERROR" class="ch.qos.logback.core.ConsoleAppender">
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>${logStdOutLevel:-ERROR}</level>
        </filter>
        <layout class="ch.qos.logback.classic.PatternLayout">
            <Pattern>
                %d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{1} - %msg%n
            </Pattern>
        </layout>
    </appender>
    <appender name="FILE-JSON" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${logFilePath:-logs}/${logFileName:-application}.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>${logFilePath:-logs}/${logFileName:-application}-%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxFileSize>20MB</maxFileSize>
            <maxHistory>7</maxHistory>
            <totalSizeCap>100MB</totalSizeCap>
        </rollingPolicy>
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
                        "Message":  "%msg"
                        }
                    </pattern>
                </pattern>
            </providers>
        </encoder>
    </appender>
    <appender name="ASYNC-JSON" class="net.logstash.logback.appender.LoggingEventAsyncDisruptorAppender">
        <appender-ref ref="${logAppender:-FILE-JSON}" />
    </appender>
    <root level="${logLevel:-INFO}">
        <appender-ref ref="ASYNC-JSON" />
        <appender-ref ref="STDOUT-ERROR" />
    </root>
</configuration>
``` 

### Setup logback-test.xml

File used when running unit tests:

* Log all messages in standard output in a regular format
* the file should be in `src/test/resources/logback-test.xml`

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

* Use async appender: To avoid block your application when logging messages
* Standard Out: use carefully some infrastructures doesn't allow developers to see it  

## Spring-boot Application

Spring-boot application encapsulates in a JAR file the server, libraries and the application itself.
By default many things happen as:
* Tomcat Server to run your application
* Port is 8080
* Load automatically dependencies when is found in classpath: Cassandra, MongoDB, Kafka, among others
* But, you can override all the things as you wish

## Spring Framework

> The Spring Framework provides a comprehensive programming and configuration model for modern Java-based enterprise applications - on any kind of deployment platform.

> A key element of Spring is infrastructural support at the application level: Spring focuses on the "plumbing" of enterprise applications so that teams can focus on application-level business logic, without unnecessary ties to specific deployment environments.

* Inversion of Control (IoC)
* Dependency Injection
* Libraries and more libraries 

## Actuator Endpoints

Enable Actuator endpoints

Edit file `application.properties`:
```properties
management.endpoints.enabled-by-default=true
management.endpoint.shutdown.enabled=false
management.endpoints.web.exposure.include=*
management.endpoint.health.show-details=always
management.info.defaults.enabled=true
management.info.git.mode=full
```

If you try to access the endpoint: `http://localhost:8080/actuator/health`

Expected return:
```json
{
    "status": "UP",
    "details": {
        "diskSpace": {
            "status": "UP",
            "details": {
                "total": 252839981056,
                "free": 66116206592,
                "threshold": 10485760
            }
        },
        "cassandra": {
            "status": "UP",
            "details": {
                "version": "3.11.4"
            }
        }
    }
}
```

Actuator give you many helpful endpoints, some of them:

* env
* beans
* health
* info
* loggers
* mappings
* threaddump

You can customize endpoints, details can be found here: [Spring Boot Actuator: Production-ready features](https://docs.spring.io/spring-boot/docs/current/reference/html/production-ready-endpoints.html)

## Enable Swagger

Create a `Bean` in Spring to enable Swagger and add some configuration.
[Here](https://www.baeldung.com/swagger-2-documentation-for-spring-rest-api) you have a good reference to setup your Swagger.

```java
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;
import java.util.Arrays;
import java.util.TreeSet;

@Configuration
@EnableSwagger2
public class SwaggerConfig {
    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2).select().apis(RequestHandlerSelectors.any()).build()
                .consumes(new TreeSet<>(Arrays.asList("application/json")))
                .produces(new TreeSet<>(Arrays.asList("application/json")))
                .apiInfo(metaData());
    }

    private ApiInfo metaData() {
        return new ApiInfoBuilder()
                .title("REST API ").description("\"REST API for an Amazing Service\"").version("v1").build();
    }
}
```

You can access the http://localhost:8080/swagger-ui.html to see all the links already created by your application like the Actuator Endpoints.

## The Model

In the [reference](https://docs.spring.io/spring-data/cassandra/docs/2.2.0.RELEASE/reference/html/#cassandra.repositories) documentation 
you will find all possibilities to use and configure how the application will work with Cassandra.  

```java
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

The Spring will automatically create an instance of this Interface and implement his methods.
Everything is convention over configuration. So the method's name tell him to use the Field `email` from the `Person` entity. 

```java
@Repository
@Component
public interface PersonRepository extends CrudRepository<Person, String> {
    Person findByEmail(String email);
}
```

### Setup Cassandra

Edit/add file: `application.yaml` in `resources` folder. 

```yaml
spring.data.cassandra:
  contact-points: localhost
  port: 9042
  keyspace-name: mykeyspace
```

You can override and compose configurations using:
* environment variables
* external files
* other files in classpath
* default values in code

Check the documentation [Externalized Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html). 

## The Kafka

* We are using Spring Cloud Stream that abstracts the implementation of the stream's solution.
* So we should check the [documentation](https://cloud.spring.io/spring-cloud-static/spring-cloud-stream/2.2.1.RELEASE/home.html) 
to know how it works and how we should configure our application.
* Read about the [abstraction](https://cloud.spring.io/spring-cloud-static/spring-cloud-stream/2.2.1.RELEASE/spring-cloud-stream.html) 
so we know the configuration that doesn't rely on implementation.
* And read about the [Kafka binder](https://cloud.spring.io/spring-cloud-static/spring-cloud-stream-binder-kafka/2.2.1.RELEASE/spring-cloud-stream-binder-kafka.html) 
once is the one that we choose.

### Binding our topic

```java
public interface Topics {

    String INPUT = "person-in";
    @Input(Topics.INPUT)
    SubscribableChannel input();
}
```

Add in your main class: `@EnableBinding(Topics.class)`
  
### Listen the Topic

Our listener is attached to our topic and should persist the data in the Cassandra database.

```java
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
```

### Configuring our Kafka topic

As you read in the related documentation you can override the default configuration and:

* set the content-type of the message in the topic
* the topic name
* concurrency
* auto-commit
* retries
* many others

You only need to add a new file in the resources folder: `application.yaml` or edit the `application.properties`.

```yaml
spring.cloud.stream.kafka.binder:
  brokers: localhost
  defaultBrokerPort: 9092
  configuration.commit.interval.ms: 1000
  requiredAcks: 1
  autoCreateTopics: true
  autoAddPartitions: true
spring.cloud.stream.bindings.person-in:
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
```
___

# Unit and Integration Tests

## Definitions

### Integration Test

Your application is like a BOX and you deal with only by the input and output. In our example the input and output is the REST API.

### Unit Test

The test is about the piece inside your application, you are going to test in method level, looking:
* Input and output of the method
* Or behaviour, if the method calls the service, repository with the expected parameters. 

## Unit tests with Mockito

Notes:
* Try to avoid run with Spring Context but only with Junit: `@RunWith(JUnit4.class)`
* With Mockito we can: verify expectations, capture arguments or return something
* Instantiate your class and mock dependencies
* Use constructor to know all dependencies

```java

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
```

## Prepare Integration Tests

### Cassandra

Add a file `test/resources/cassandra-test.yaml` based on the file provided by [Cassandra Unit - cassandra-test.yaml](https://github.com/jsevellec/cassandra-unit/blob/master/cassandra-unit/src/main/resources/cu-cassandra.yaml)
In the documentation of [Cassandra Unit](https://github.com/jsevellec/cassandra-unit/wiki/Spring-for-Cassandra-unit) you have a lot of options to use. 


Edit the port to avoid conflict with cassandra locally:
```yaml
native_transport_port: 9242
```

Add a file `test/resources/application-test.properties` to setup the connection with the cassandra-unit:
```properties
spring.data.cassandra.contact-points=localhost
spring.data.cassandra.port=9242
spring.data.cassandra.jmx-enabled=false
```

### Integration test for Controller

* Start Embedded Cassandra with `@BeforeClass`
* Stop Embedded Cassandra with `@AfterClass`
* `@SpringBootTest` to boot application
* `@TestPropertySource` to override configuration for tests
* `@AutoConfigureMockMvc` and `MockMvc` to perform HTTP operations
* You can use other options to start the EmbeddedCassandra 

```java
@ContextConfiguration(classes = {HandsOnApplication.class})
@AutoConfigureMockMvc // When application boots the test will instantiate a MockMvc
@TestPropertySource("classpath:application-test.properties") // Override configuration only for tests
@RunWith(SpringRunner.class)
@SpringBootTest // The application will startup
public class HandsOnApplicationTests {

    @Autowired
    private MockMvc mockMvc; // This object allow us to perform HTTP operations in our application

    @Autowired
    private PersonRepository repository;

    @BeforeClass
    public static void initCassandra() throws Exception {
        Properties prop = new Properties();
        prop.load(HandsOnApplicationTests.class.getClassLoader().getResourceAsStream("application-test.properties"));
        String cassandraHosts = prop.getProperty("spring.data.cassandra.contact-points");
        String cassandraPort = prop.getProperty("spring.data.cassandra.port");

        // Start Cassanda Unit
        EmbeddedCassandraServerHelper.startEmbeddedCassandra("cassandra-test.yaml", 20000);
        Cluster cluster = Cluster.builder()
                .addContactPoints(cassandraHosts)
                .withPort(Integer.parseInt(cassandraPort))
                .withoutJMXReporting()
                .build();
        // Connect and execute CQL Script
        Session session = cluster.connect();

        // CQL to create Keyspace
        String cqlScript1 = FileUtils.readFully(new InputStreamReader(HandsOnApplicationTests.class.getClassLoader().getResourceAsStream("cqls/create_keyspace.cql")));
        session.execute(cqlScript1);
    
        // CQL to create table
        String cqlScript2 = FileUtils.readFully(new InputStreamReader(HandsOnApplicationTests.class.getClassLoader().getResourceAsStream("cqls/create_table.cql")));
        session.execute(cqlScript2);
    }

    @AfterClass
    public static void cleanCassandra() throws Exception {
        EmbeddedCassandraServerHelper.cleanEmbeddedCassandra();
    }

    @Test
    public void postAndGetPerson() throws Exception {

        // Prepare
        String email = "jhon.doe@company.com";
        Person p = new Person();
        p.setEmail(email);
        this.repository.delete(p);

        String contentToPost = "{\"email\": \""+email+"\", \"firstName\": \"Jhon\", \"lastName\": \"Doe\", \"yearBirth\": 1975 }";

        // Act/Assert - POST
        MvcResult mvcResult = this.mockMvc.perform(post("/api/person")
                .content(contentToPost).contentType("application/json"))
                .andDo(print())
                .andExpect(status().isNoContent()).andReturn();

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

```java
public class HandsOnApplicationTests {
    // ...

    @Autowired
    @Qualifier(Topics.INPUT)
    private MessageChannel messageChannel;

    @Autowired
    private MessageCollector messageCollector;

    @Autowired
    private ObjectMapper objectMapper; // The default instance created by Spring

    @Test
    public void publishMessageToChannelAndCheckRepository() throws Exception {

        // Prepare
        String email = "james.watt@company.com";
        Person p = new Person();
        p.setEmail(email);
        this.repository.delete(p);

        String json = "{\"email\": \""+email+"\", \"firstName\": \"James\", \"lastName\": \"Watt\", \"yearBirth\": 1736 }";
        Message<String> message =  MessageBuilder.withPayload(json).build();

        // Act
        this.messageChannel.send(message);

        // Assert
        Person person = this.repository.findByEmail(email);
        Assert.assertNotNull(person);
        String jsonFromRepository = this.objectMapper.writeValueAsString(person);
        JSONAssert.assertEquals(json, jsonFromRepository, true);
    }
}
```
___

# Pack, Deliver and Run

1. `mvn clean package`
1. `java -jar target\hands-on-0.0.1-SNAPSHOT.jar`
1. Check if application booted in: `http://localhost:8080/actuator/info` , should return HTTP 200 with a JSON.
1. You can access the Swagger Documentation `http://localhost:8080/swagger-ui.html`
1. POST and GET a sample
```shell script
curl -v -X POST 'http://localhost:8080/api/person' -H 'Content-Type: application/json' -d '{"email": "james.watt@gmail.com", "firstName": "James", "lastName": "Watt", "yearBirth": 1736}'
curl -v -X GET 'http://localhost:8080/api/person?email=james.watt@gmail.com' -H 'accept: application/json'
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

 * Add cache to avoid get the same data always going to retrive from cassandra.

 * Force to expire the cache whenever the data is updated

 * Allow to enable/disable Swagger by configuration
