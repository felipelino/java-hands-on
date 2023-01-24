package com.hands.on.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.config.CqlSessionFactoryBean;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;

/*
    Reference: https://docs.spring.io/spring-data/cassandra/docs/current/reference/html/#cassandra.connectors
 */
@EnableCassandraRepositories(basePackages = "com.hands.on")
@Configuration
public class CassandraConfig {

    /*
     * Factory bean that creates the com.datastax.oss.driver.api.core.CqlSession instance
     */
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
