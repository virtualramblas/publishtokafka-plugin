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
	 */
	public SimpleKafkaProducer(String bootstapServers, 
			String metadataBrokerList, String acks) {
		init(bootstapServers, metadataBrokerList, acks);
	}
	
	/**
	 * Initializes the class.
	 */
	private void init(String bootstapServers, 
			String metadataBrokerList, String acks) {
		Properties props = new Properties();
		props.put("bootstrap.servers", bootstapServers);
		props.put("metadata.broker.list", metadataBrokerList);
		props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		props.put("acks", acks);
		
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
