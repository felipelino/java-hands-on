package com.hands.on;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hands.on.model.Person;
import com.hands.on.repository.PersonRepository;
import com.hands.on.stream.Topics;
import org.apache.tools.ant.util.FileUtils;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.InputStreamReader;
import java.util.Properties;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ContextConfiguration(classes = {HandsOnApplication.class})
@AutoConfigureMockMvc // When application boots the test will instantiate a MockMvc
@TestPropertySource("classpath:application-test.properties") // Override configuration only for tests
@RunWith(SpringRunner.class)
@SpringBootTest	// The application will startup
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
		String cqlScript1 = FileUtils.readFully(new InputStreamReader(HandsOnApplicationTests.class.getClassLoader().getResourceAsStream("cqls/create_keyspace.cql")));
		session.execute(cqlScript1);
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

		// Act/Assert - GET
		mvcResult = this.mockMvc.perform(get("/api/person")
				.param("email", email)
				.accept("application/json"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(content().json(contentToPost, true))
				.andReturn();
	}

	/* Tests for Kafka */
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
