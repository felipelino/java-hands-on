package com.hands.on;

import com.hands.on.model.Person;
import com.hands.on.repository.PersonRepository;
import com.hands.on.stream.PersonProducer;
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
@Import(TestChannelBinderConfiguration.class) // Enable kafka binder for test
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
		this.input.send(MessageBuilder.withPayload(json).build(), "person-topic");

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
