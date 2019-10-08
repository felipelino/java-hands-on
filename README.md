# Script de Hands On Java

# Objective

* Java Application with Spring Boot
* REST API
* Persistence Layer Cassandra
* Send Message to a Kafka Queue
* Logging
* Maven to Build

# Pre-requisites

* [JDK Java 1.8](https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
* [IntelliJ Community](https://www.jetbrains.com/idea/download/) as IDE
* [Apache Maven](https://maven.apache.org)  to Build
* [Docker](https://docs.docker.com/docker-for-windows/install/) so we can go up with Cassandra and Kafka

# Application Overview

0. Receive a model by REST API
0. Publish this data in Kafka Topic
0. Listen the Kafka Topic
0. Persist the data in Cassandra
0. Allow Get from the REST API 


# Steps

## External Dependencies

Is out of scope talk about docker. So we are going to have here the `docker-compose.yml` with all dependencies:

* Cassandra
* Kafka
* Zookeeper (because Kafka depends on Zookeeper)

```
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

## Spring Boot Initializer

* https://lmgtfy.com/?q=Spring+Boot+Initializer
* Link:  https://start.spring.io/

*Add Spring Dependencies*

* [Spring Cloud Stream](https://spring.io/projects/spring-cloud-stream): To publish and listen kafka topics (or other kind of queues as: Kafka, AWS Kinesis, RabbitMQ)
* [Spring Data Cassandra](https://spring.io/projects/spring-data-cassandra): To access Cassandra
* [Spring Actuator](https://spring.io/guides/gs/actuator-service/): Add endpoints useful to debug application

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

* [Springfox Swagger](https://springfox.github.io/springfox/docs/current/#springfox-swagger-ui): To expose our REST API in a friendly view 
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

```
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

```
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="STDOUT-ERROR" class="ch.qos.logback.core.ConsoleAppender">
        <filter class="ch.qos.logback.classic.filter.LevelFilter">
            <level>${logStdOutLevel:-WARN}</level>
            <onMatch>ACCEPT</onMatch>
            <onMismatch>DENY</onMismatch>
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

```
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

## Actuator Endpoints

Enable Actuator endpoints

Edit file `application.properties`:
```
management.endpoints.enabled-by-default=true
management.endpoint.shutdown.enabled=false
management.endpoints.web.exposure.include=*
management.endpoint.health.show-details=always
management.info.defaults.enabled=true
management.info.git.mode=full
```
