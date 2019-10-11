# Script de Hands On Java

# Objective

* Java Application with Spring Boot
* REST API
* Persistence Layer Cassandra
* Send Message to a Kafka Queue
* Logging
* Maven to Build
* Swagger Documentation 

# Pre-requisites

* [JDK Java 1.8](https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
* [IntelliJ Community](https://www.jetbrains.com/idea/download/) as IDE
* [Apache Maven](https://maven.apache.org)  to Build
* [Docker](https://docs.docker.com/docker-for-windows/install/) so we can go up with Cassandra and Kafka

# Application Overview

0. Receive a model by REST API
0. Publish this data in Kafka Topic
0. Listen to the Kafka Topic
0. Persist the data in Cassandra
0. Allow Get from the REST API 
0. Documentation exposed by Swagger


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

* https://lmgtfy.com/?q=Spring+Boot+Initializer
* Link:  https://start.spring.io/

*Add Spring Dependencies*

* [Spring Cloud Stream](https://spring.io/projects/spring-cloud-stream): To publish and listen kafka topics (or other kind of queues as: Kafka, AWS Kinesis, RabbitMQ)
* [Spring Data Cassandra](https://spring.io/projects/spring-data-cassandra): To access Cassandra
* [Spring Actuator](https://spring.io/guides/gs/actuator-service/): Add endpoints useful to debug application
* [spring-boot-starter-web](https://www.baeldung.com/spring-boot-starters): Allow application to answer REST HTTP 

*Steps in Spring IO Initializer*

* Download the ZIP File.
* Unzip the file
* `Open as Project` with IntelliJ selecting the `pom.xml`
* Optionally you can set to download dependencies automatically when pom is updated.
* Run the class with annotation `@SpringBootApplication` as a Main application.
* If everything is OK in output log will appear:
```
Started HandsOnApplication in 7.079 seconds (JVM running for 8.001)
```
* Stop application

## About Maven and Edit POM

### Maven Lifecycle

* [Introduction to the Build Lifecycle](http://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html)

> * validate - validate the project is correct and all necessary information is available
* compile - compile the source code of the project
* test - test the compiled source code using a suitable unit testing framework. These tests should not require the code be packaged or deployed
* package - take the compiled code and package it in its distributable format, such as a JAR.
* verify - run any checks on results of integration tests to ensure quality criteria are met
* install - install the package into the local repository, for use as a dependency in other projects locally
* deploy - done in the build environment, copies the final package to the remote repository for sharing with other developers and projects.

### Dependencies

*Other Dependencies:*

* [Springfox Swagger and springfox-swagger2](https://springfox.github.io/springfox/docs/current/#springfox-swagger-ui): To expose our REST API in a friendly view 
* [Logback](https://logback.qos.ch/): Library to log
* [Logstash logback](https://github.com/logstash/logstash-logback-encoder): Library to write log in JSON format compatible with [Logstash](https://www.elastic.co/pt/products/logstash)
* [Jackson JSON](https://github.com/FasterXML/jackson): Library to parse JSON to Object and vice-versa.
* [Apache Commons Lang](https://commons.apache.org/proper/commons-lang/): Library with useful methods to manipulate Strings, Collections, Objects

*Test Dependencies:*

* [Cassandra Unit Spring](https://github.com/jsevellec/cassandra-unit/wiki/Spring-for-Cassandra-unit): For unit/integration tests with cassandra
* [Mockito](https://site.mockito.org/): For tests using mock for dependencies
* [Spring Cloud Stream Test](https://spring.io/blog/2017/10/24/how-to-test-spring-cloud-stream-applications-part-i): To test applications with cloud stream.
* [Spring Boot Starter Test](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-testing.html):  To test spring boot applications.

*How to Search and Add Dependencies*

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

### Plugins

* [spring-boot-maven-plugin](https://docs.spring.io/spring-boot/docs/current/reference/html/build-tool-plugins-maven-plugin.html): Provides Spring Boot support in Maven, letting you package executable jar or war archives and run an application “in-place”. 
* [git-commit-id-plugin](https://github.com/git-commit-id/maven-git-commit-id-plugin/blob/master/docs/using-the-plugin.md): Show useful information

*How to Add and Configure Plugin*

* Read the documentation about the Plugin. 
* The documentation normally follow the same pattern.
* Search plugins that can solve an issue about build, example: generate proto-buf classes based in proto file; automatically change version of a code; run an external command or script

## Setup Logging

* Parameters: `${logStdOutLevel:-WARN}` , `${logLevel:-INFO}`, `${logFilePath:-logs}`, `${logFileName:-application}`
* You can override this in VM Options, example: `java -DlogLevel=DEBUG -DlogStdOutLevel=INFO application.jar`
* Always test and re-run your application 

### Setup logback.xml

File used by the application:

* Log in the File using JSON format with rolling policy
* Log warning and error messages in standard output

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

## Springboot Application

Springboot application encapsulates in a JAR file the server, libraries and the application itself.
By default many things happen as:
* Tomcat Server is choosing to run your application
* Port is 8080
* Load automatically dependencies when is found in classpath: Cassandra, MongoDB, Kafka, among others

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
                .title("REST API ").description("\"REST API for Amazing Service\"").version("v1").build();
    }
}
```

You can access the http://localhost:8080/swagger-ui.html to see all the links already created by your application like the Actuator Endpoints.

## The Model

In the [reference](https://docs.spring.io/spring-data/cassandra/docs/2.2.0.RELEASE/reference/html/#cassandra.repositories) documentation you will find all possibilities to use and configure how the application will work with Cassandra.  

```java
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
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
    List<Person> findByEmail(String email);
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

## The Kafka

* We are using Spring Cloud Stream that abstracts the implementation of the stream's solution.
* So we should check the [documentation](https://cloud.spring.io/spring-cloud-static/spring-cloud-stream/2.2.1.RELEASE/home.html) to know how it work and how we should configure our application.
* Read about the [abstraction](https://cloud.spring.io/spring-cloud-static/spring-cloud-stream/2.2.1.RELEASE/spring-cloud-stream.html) so we know the configuration that don't rely on implementation.
* And read about the [Kafka binder](https://cloud.spring.io/spring-cloud-static/spring-cloud-stream-binder-kafka/2.2.1.RELEASE/spring-cloud-stream-binder-kafka.html) once is the one that we choose.

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
  destination: person-input-topic
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

