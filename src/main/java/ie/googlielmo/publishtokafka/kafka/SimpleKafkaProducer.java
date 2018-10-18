package ie.googlielmo.publishtokafka.kafka;

import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

/**
 * Custom Kafka producer client.
 * 
 * @author Guglielmo Iozzia
 *
 */
public class SimpleKafkaProducer {
	/** Kafka native producer */
	private KafkaProducer<String, String> producer; 
    
	/**
	 * Default constructor.
	 * @param producerConfig The configuration
	 */
	public SimpleKafkaProducer(KafkaProducerConfiguration producerConfig) {
		init(producerConfig);
	}
	
	/**
	 * Initializes the class.
	 */
	private void init(KafkaProducerConfiguration producerConfig) {
		Properties props = new Properties();
		props.put("bootstrap.servers", producerConfig.getBootstrapServers());
		props.put("metadata.broker.list", producerConfig.getMetadataBrokerList());
		props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		props.put("acks", producerConfig.getAcks());
		
		// Set up advanced properties
		props.put("retries", String.valueOf(producerConfig.getRetries()));
		props.put("client.id", producerConfig.getClientId());
		props.put("connections.max.idle.ms", String.valueOf(producerConfig.getConnectionsMaxIdle()));
		props.put("request.timeout.ms", String.valueOf(producerConfig.getRequestTimeout()));
		props.put("timeout.ms", String.valueOf(producerConfig.getTimeout()));
		props.put("metadata.fetch.timeout.ms", String.valueOf(producerConfig.getMetadataFetchTimeout()));
		props.put("metadata.max.age.ms", String.valueOf(producerConfig.getMetadataMaxAge()));
		
		producer = new KafkaProducer<String, String>(props);
	}
	
	/**
	 * Closes the producer connection with the broker.
	 */
	public void closeConnection() {
		if(producer != null) {
			// Close the producer connection with the broker.
			producer.close();
		}
	}
	
	/**
	 * Publishes a message to a given topic.
	 * 
	 * @param topicName The name of the topic
	 * @param message	The message to publish
	 */
	public void sendMessage(String topicName, String message) {
		// Publish the message
		producer.send(
				new ProducerRecord<String, String>(topicName, topicName, message));
	}
}
