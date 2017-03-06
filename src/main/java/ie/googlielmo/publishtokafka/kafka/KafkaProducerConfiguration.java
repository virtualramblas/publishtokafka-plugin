package ie.googlielmo.publishtokafka.kafka;

import java.io.Serializable;

/**
 * Simple Kafka Producer configuration.
 * 
 * @author Guglielmo Iozzia
 *
 */
public class KafkaProducerConfiguration implements Serializable {

	private static final long serialVersionUID = 8330910709563702315L;

	/** A comma separated list of bootstrap servers (hostname:port) */
	private String bootstrapServers;
	/** The comma separated list of the metadata brokers (hostname:port) */
	private String metadataBrokerList;
	/** The number of acknowledgments the producer requires */
	private String acks;
	/** The number of attempts the producer has to do when sending messages. */
	private int retries;
	/** A string id to pass to the Kafka server when making requests in order to be able then to track the source of requests.*/
	private String clientId;
	/** Idle connections automatically close after the number of milliseconds specified in this field. */
	private long connectionsMaxIdle;
	/** The maximum amount of time (in milliseconds) the client will wait for the response of a request. */
	private int requestTimeout;
	/** The maximum amount of time (in milliseconds) the Kafka server will wait for acknowledgments from followers. */
	private int timeout;
	/** The maximum time (in milliseconds) to fetch metadata in order to know which servers host the topic's partitions. */
	private long metadataFetchTimeout;
	/** The period of time (in milliseconds) after which a refresh of metadata is forced any way. */
	private long metadataMaxAge;
	
	/** 
	 * Default constructor.
	 */
	public KafkaProducerConfiguration() {
		this.retries = 0;
		this.clientId = "";
		this.connectionsMaxIdle = 540000;
		this.requestTimeout = 30000;
		this.timeout = 30000;
		this.metadataFetchTimeout = 60000;
		this.metadataMaxAge = 300000;
	}

	public String getBootstrapServers() {
		return bootstrapServers;
	}

	public void setBootstrapServers(String bootstrapServers) {
		this.bootstrapServers = bootstrapServers;
	}

	public String getMetadataBrokerList() {
		return metadataBrokerList;
	}

	public void setMetadataBrokerList(String metadataBrokerList) {
		this.metadataBrokerList = metadataBrokerList;
	}

	public String getAcks() {
		return acks;
	}

	public void setAcks(String acks) {
		this.acks = acks;
	}

	public int getRetries() {
		return retries;
	}

	public void setRetries(int retries) {
		this.retries = retries;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public long getConnectionsMaxIdle() {
		return connectionsMaxIdle;
	}

	public void setConnectionsMaxIdle(long connectionsMaxIdle) {
		this.connectionsMaxIdle = connectionsMaxIdle;
	}

	public int getRequestTimeout() {
		return requestTimeout;
	}

	public void setRequestTimeout(int requestTimeout) {
		this.requestTimeout = requestTimeout;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public long getMetadataFetchTimeout() {
		return metadataFetchTimeout;
	}

	public void setMetadataFetchTimeout(long metadataFetchTimeout) {
		this.metadataFetchTimeout = metadataFetchTimeout;
	}

	public long getMetadataMaxAge() {
		return metadataMaxAge;
	}

	public void setMetadataMaxAge(long metadataMaxAge) {
		this.metadataMaxAge = metadataMaxAge;
	}
}
