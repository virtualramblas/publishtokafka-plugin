package ie.googlielmo.publishtokafka;
import hudson.Launcher;
import hudson.EnvVars;
import hudson.Extension;
import hudson.util.FormValidation;
import ie.googlielmo.publishtokafka.kafka.KafkaProducerConfiguration;
import ie.googlielmo.publishtokafka.kafka.SimpleKafkaProducer;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sample {@link Builder}.
 *
 * <p>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked
 * and a new {@link PublishToKafkaBuilder} is created. The created
 * instance is persisted to the project configuration XML by using
 * XStream, so this allows you to use instance fields (like {@link #name})
 * to remember the configuration.
 *
 * <p>
 * When a build is performed, the {@link #perform} method will be invoked. 
 *
 * @author Guglielmo Iozzia
 */
public class PublishToKafkaBuilder extends Recorder {

	/** The comma separated list of bootstrap servers (hostname:port) */
    private final String bootstrapServers;
    /** The comma separated list of the metadata brokers (hostname:port) */
    private final String metadataBrokerList;
    /** The number of acknowledgments the producer requires */
    private final String acks;
    /** The name of the Kafka topic where to publish the data */
    private final String topic;
    /** Flag to exclude the plugin from build job execution */
    private final boolean excludePlugin;
    /** Flag to modify the build job execution status: 
     * if checked, any failure in the plugin execution will mark the build job as failed. */
    private final boolean changeBuildStatus;
    /** The number of attempts the producer has to do when sending messages. */
    private final int retries;
    /** A string id to pass to the Kafka server when making requests in order to be able then to track the source of requests.*/
    private final String clientId;
    /** Idle connections automatically close after the number of milliseconds specified in this field. */
	private final long connectionsMaxIdle;
	/** The maximum amount of time (in milliseconds) the client will wait for the response of a request. */
	private final int requestTimeout;
	/** The maximum amount of time (in milliseconds) the Kafka server will wait for acknowledgments from followers. */
	private final int timeout;
	/** The maximum time (in milliseconds) to fetch metadata in order to know which servers host the topic's partitions. */
	private final long metadataFetchTimeout;
	/** The period of time (in milliseconds) after which a refresh of metadata is forced any way. */
	private final long metadataMaxAge;
	
    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public PublishToKafkaBuilder(String bootstrapServers, String metadataBrokerList,
    		String acks, String topic, boolean excludePlugin, 
    		boolean changeBuildStatus, int retries,
    		String clientId, long connectionsMaxIdle,
    		int requestTimeout, int timeout,
    		long metadataFetchTimeout, long metadataMaxAge) {
        this.bootstrapServers = bootstrapServers;
        this.metadataBrokerList = metadataBrokerList;
        this.acks = acks;
        this.topic = topic;
        this.excludePlugin = excludePlugin;
        this.changeBuildStatus = changeBuildStatus;
        
        this.retries = retries;
        this.clientId = clientId;
        this.connectionsMaxIdle = connectionsMaxIdle;
        this.requestTimeout = requestTimeout;
        this.timeout = timeout;
        this.metadataFetchTimeout = metadataFetchTimeout;
        this.metadataMaxAge = metadataMaxAge;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
    	boolean result = true;
        
    	if(!isExcludePlugin()) {
    		JSONObject buildJob = buildJobToJson(build, listener);
        	listener.getLogger().println(buildJob.toString());
    		
        	// Publish to Kafka
        	result = publishToKafkaTopic(buildJob, listener);
        	if(result == false) {
        		if(isChangeBuildStatus() == false) {
        			result = true;
        		}
        	}
    	}
    	
    	return result;
    }

    /**
     * Creates a JSON object with the current execution data of a build job.
     * 
     * @param build
     * @param listener
     * @return JSONObject
     */
    private JSONObject buildJobToJson(AbstractBuild build, BuildListener listener) {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("buildNumber", build.getNumber());
		jsonObject.put("name", build.getFullDisplayName().substring(0, build.getFullDisplayName().lastIndexOf(" #")));
		jsonObject.put("startDate", new Date(build.getStartTimeInMillis()));
		Date endDate = Calendar.getInstance().getTime();
		jsonObject.put("endDate", endDate);
		jsonObject.put("duration", endDate.getTime() - build.getStartTimeInMillis());
		jsonObject.put("result", build.getResult().toString());
		
		addParameters(build, jsonObject);
		
		try {
			EnvVars envVars = build.getEnvironment(listener);
			
			jsonObject.put("userId", envVars.get("BUILD_USER_ID"));
			jsonObject.put("username", envVars.get("BUILD_USER"));
			
			jsonObject.put("nodeName", envVars.get("NODE_NAME"));
			jsonObject.put("computerName", envVars.get("COMPUTERNAME"));
			
			addGitInfo(build, jsonObject);
			
			addNodeDetails(build, jsonObject, envVars);
		} catch (IOException e) {
			listener.getLogger().println("Exception trying to get the env variables: " + e.getMessage());
		} catch (InterruptedException e) {
			listener.getLogger().println("Exception trying to get the env variables: " + e.getMessage());
		}
		
		return jsonObject;
    }
    
    /**
     * Adds the build job parameters key:value pairs to the JSON output.
     * 
     * @param build
     * @param jsonObject
     */
    private void addParameters(AbstractBuild build, JSONObject jsonObject) {
    	Map<String, String> buildVariables = build.getBuildVariables();
		if(buildVariables.size() > 0) {
			JSONObject parametersJsonObject = new JSONObject();
			for(Map.Entry<String, String> entry : buildVariables.entrySet()) {
				parametersJsonObject.put(entry.getKey(), 
										buildVariables.get(entry.getValue()));
			}
			jsonObject.put("parameters", parametersJsonObject);
		}
    }
    
    /**
     * Adds the Git details (if any for the build job) to the JSON output.
     * 
     * @param build
     * @param jsonObject
     */
    private void addGitInfo(AbstractBuild build, JSONObject jsonObject) {
    	JSONObject gitDetailsJsonObject = new JSONObject();
    	
    	List<Action> actions = (List<Action>) build.getAllActions();
    	Iterator<Action> actionsIterator = actions.iterator();
    	while(actionsIterator.hasNext()) {
    		Action action = actionsIterator.next();
    		if(action instanceof hudson.plugins.git.util.BuildData) {
    			// Add Git information to the JSON content
    			gitDetailsJsonObject.put("remoteURLs", ((hudson.plugins.git.util.BuildData)action).getRemoteUrls());
    			gitDetailsJsonObject.put("lastBuiltRevision", ((hudson.plugins.git.util.BuildData)action).getLastBuiltRevision());
    			break;
    		}
    	}
    	
    	jsonObject.put("gitDetails", gitDetailsJsonObject);
    }
    
    /**
     * Adds the details about the node where the job has been executed to the JSON output.
     * 
     * @param build
     * @param jsonObject
     * @param envVars
     */
    private void addNodeDetails(AbstractBuild build, JSONObject jsonObject, EnvVars envVars) {
    	JSONObject nodeDetailsJsonObject = new JSONObject();
    	nodeDetailsJsonObject.put("nodeLabels", envVars.get("NODE_LABELS"));
    	nodeDetailsJsonObject.put("OS", envVars.get("OS"));
    	nodeDetailsJsonObject.put("numberOfProcessors", envVars.get("NUMBER_OF_PROCESSORS"));
    	nodeDetailsJsonObject.put("processorArchitecture", envVars.get("PROCESSOR_ARCHITECTURE"));
    	nodeDetailsJsonObject.put("processorArchitectureW6432", envVars.get("PROCESSOR_ARCHITEW6432"));
    	nodeDetailsJsonObject.put("processorIdentifier", envVars.get("PROCESSOR_IDENTIFIER"));
    	String computerName = "";
    	if(envVars.get("COMPUTERNAME") != null && !envVars.get("COMPUTERNAME").equals("")) {
    		computerName = envVars.get("COMPUTERNAME");
		} else {
			computerName = getComputerNameFromJenkinsUrl(envVars.get("JENKINS_URL"));
		}
    	nodeDetailsJsonObject.put("computerName", computerName);
    	
    	jsonObject.put("nodeDetails", nodeDetailsJsonObject);
    }
    
    /**
     * Returns the name of the node where the job has been executed.
     * 
     * @param jenkinsUrl The Jenkins URL on the given node
     * @return The node name
     */
    private String getComputerNameFromJenkinsUrl(String jenkinsUrl) {
		// Get the name + port
		String[] urlTokens = jenkinsUrl.split("/");
		String computerName = urlTokens[2];
		// Remove port (if any)
		int portPosition = computerName.indexOf(":");
		if(portPosition != -1) {
			computerName = computerName.substring(0, computerName.indexOf(":"));
		}
		
		return computerName;
	}
    
    /**
     * Publishes the build job current execution data to a Kafka topic.
     * 
     * @param jsonObject
     * @param listener
     * @return true, if everything has gone as expected
     */
    private boolean publishToKafkaTopic(JSONObject jsonObject, BuildListener listener) {
    	boolean success = false;
    	
    	// Init the producer
    	// The following command is needed due to the following issue 
    	// (https://issues.apache.org/jira/browse/KAFKA-3218)
    	// running Kafka as OSGi module
    	Thread.currentThread().setContextClassLoader(null);
    	SimpleKafkaProducer producer = null;
    	try {
    		producer = new SimpleKafkaProducer(initKafkaProducer());
			// Send the message
			producer.sendMessage(topic, jsonObject.toString());
			success = true;
		} catch (Exception e) {
			listener.getLogger().println("Exception trying to publish: " + e.getMessage());
		} finally {
			// Close the connection
			if(producer != null) {
				producer.closeConnection();
			}
		}
    	
    	return success;
    }
    
    /**
     * Initializes a Kafka Producer.
     * 
     * @return	KafkaProducerConfiguration
     */
    private KafkaProducerConfiguration initKafkaProducer() {
    	KafkaProducerConfiguration producerConfig = new KafkaProducerConfiguration();
    	
    	producerConfig.setBootstrapServers(bootstrapServers);
    	producerConfig.setMetadataBrokerList(metadataBrokerList);
    	producerConfig.setAcks(acks);
    	producerConfig.setRetries(retries);
    	producerConfig.setClientId(clientId);
    	producerConfig.setConnectionsMaxIdle(connectionsMaxIdle);
    	producerConfig.setRequestTimeout(requestTimeout);
    	producerConfig.setTimeout(timeout);
    	producerConfig.setMetadataFetchTimeout(metadataFetchTimeout);
    	producerConfig.setMetadataMaxAge(metadataMaxAge);
    	
    	return producerConfig;
    }
    
    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    /**
     * Descriptor for {@link PublishToKafkaBuilder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See <tt>src/main/resources/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        /**
         * To persist global configuration information,
         * simply store it in a field and call save().
         *
         * <p>
         * If you don't want fields to be persisted, use <tt>transient</tt>.
         */
    	private String bootstrapServers;
        private String metadataBrokerList;
        private String acks;
        private String topic;
        private boolean excludePlugin;
        private boolean changeBuildStatus;
        
        /**
         * In order to load the persisted global configuration, you have to 
         * call load() in the constructor.
         */
        public DescriptorImpl() {
            load();
        }

        /**
         * Performs on-the-fly validation of the form field 'bootstrapServers'.
         *
         * @param value
         *      This parameter receives the value that the user has typed.
         * @return
         *      Indicates the outcome of the validation. This is sent to the browser.
         *      <p>
         *      Note that returning {@link FormValidation#error(String)} does not
         *      prevent the form from being saved. It just means that a message
         *      will be displayed to the user. 
         */
        public FormValidation doCheckBootstrapServers(@QueryParameter String value)
                throws IOException, ServletException {
            if(value.length() == 0) {
                return FormValidation.error("Please add at least one server name/port pair");
            }
            if(!checkHostPortPairs(value)) {
            	return FormValidation.error("One of the server name/port pair isn't valid");
            }
            
            return FormValidation.ok();
        }
        
        /**
         * Performs on-the-fly validation of the form field 'metadataBrokerList'.
         *
         * @param value
         *      This parameter receives the value that the user has typed.
         * @return
         *      Indicates the outcome of the validation. This is sent to the browser.
         *      <p>
         *      Note that returning {@link FormValidation#error(String)} does not
         *      prevent the form from being saved. It just means that a message
         *      will be displayed to the user. 
         */
        public FormValidation doCheckMetadataBrokerList(@QueryParameter String value)
                throws IOException, ServletException {
            if(value.length() == 0) {
                return FormValidation.error("Please add at least one server name/port pair");
            }
            if(!checkHostPortPairs(value)) {
            	return FormValidation.error("One of the server name/port pair isn't valid");
            }
            
            return FormValidation.ok();
        }
        
        /**
         * Performs on-the-fly validation of the form field 'topic'.
         *
         * @param value
         *      This parameter receives the value that the user has typed.
         * @return
         *      Indicates the outcome of the validation. This is sent to the browser.
         *      <p>
         *      Note that returning {@link FormValidation#error(String)} does not
         *      prevent the form from being saved. It just means that a message
         *      will be displayed to the user. 
         */
        public FormValidation doCheckTopic(@QueryParameter String value)
                throws IOException, ServletException {
            if(value.length() == 0)
                return FormValidation.error("Please add a topic name");
            
            return FormValidation.ok();
        }

        /**
         * Performs on-the-fly validation of the form field 'retries'.
         *
         * @param value
         *      This parameter receives the value that the user has typed.
         * @return
         *      Indicates the outcome of the validation. This is sent to the browser.
         *      <p>
         *      Note that returning {@link FormValidation#error(String)} does not
         *      prevent the form from being saved. It just means that a message
         *      will be displayed to the user. 
         */
        public FormValidation doCheckRetries(@QueryParameter String value)
                throws IOException, ServletException {
            if(value != null && !value.equals("")) {
            	if(!isInteger(value)) {
            		return FormValidation.error("Please set an integer value");
            	}
            }
            
            return FormValidation.ok();
        }
        
        /**
         * Performs on-the-fly validation of the form field 'connectionsMaxIdle'.
         *
         * @param value
         *      This parameter receives the value that the user has typed.
         * @return
         *      Indicates the outcome of the validation. This is sent to the browser.
         *      <p>
         *      Note that returning {@link FormValidation#error(String)} does not
         *      prevent the form from being saved. It just means that a message
         *      will be displayed to the user. 
         */
        public FormValidation doCheckConnectionsMaxIdle(@QueryParameter String value)
                throws IOException, ServletException {
            if(value != null && !value.equals("")) {
            	if(!isLong(value)) {
            		return FormValidation.error("Please set a numeric value");
            	}
            }
            
            return FormValidation.ok();
        }
        
        /**
         * Performs on-the-fly validation of the form field 'requestTimeout'.
         *
         * @param value
         *      This parameter receives the value that the user has typed.
         * @return
         *      Indicates the outcome of the validation. This is sent to the browser.
         *      <p>
         *      Note that returning {@link FormValidation#error(String)} does not
         *      prevent the form from being saved. It just means that a message
         *      will be displayed to the user. 
         */
        public FormValidation doCheckRequestTimeout(@QueryParameter String value)
                throws IOException, ServletException {
            if(value != null && !value.equals("")) {
            	if(!isInteger(value)) {
            		return FormValidation.error("Please set an integer value");
            	}
            }
            
            return FormValidation.ok();
        }
        
        /**
         * Performs on-the-fly validation of the form field 'timeout'.
         *
         * @param value
         *      This parameter receives the value that the user has typed.
         * @return
         *      Indicates the outcome of the validation. This is sent to the browser.
         *      <p>
         *      Note that returning {@link FormValidation#error(String)} does not
         *      prevent the form from being saved. It just means that a message
         *      will be displayed to the user. 
         */
        public FormValidation doCheckTimeout(@QueryParameter String value)
                throws IOException, ServletException {
            if(value != null && !value.equals("")) {
            	if(!isInteger(value)) {
            		return FormValidation.error("Please set an integer value");
            	}
            }
            
            return FormValidation.ok();
        }
        
        /**
         * Performs on-the-fly validation of the form field 'metadataFetchTimeout'.
         *
         * @param value
         *      This parameter receives the value that the user has typed.
         * @return
         *      Indicates the outcome of the validation. This is sent to the browser.
         *      <p>
         *      Note that returning {@link FormValidation#error(String)} does not
         *      prevent the form from being saved. It just means that a message
         *      will be displayed to the user. 
         */
        public FormValidation doCheckMetadataFetchTimeout(@QueryParameter String value)
                throws IOException, ServletException {
            if(value != null && !value.equals("")) {
            	if(!isLong(value)) {
            		return FormValidation.error("Please set a numeric value");
            	}
            }
            
            return FormValidation.ok();
        }
        
        /**
         * Performs on-the-fly validation of the form field 'metadataMaxAge'.
         *
         * @param value
         *      This parameter receives the value that the user has typed.
         * @return
         *      Indicates the outcome of the validation. This is sent to the browser.
         *      <p>
         *      Note that returning {@link FormValidation#error(String)} does not
         *      prevent the form from being saved. It just means that a message
         *      will be displayed to the user. 
         */
        public FormValidation doCheckMetadataMaxAge(@QueryParameter String value)
                throws IOException, ServletException {
            if(value != null && !value.equals("")) {
            	if(!isLong(value)) {
            		return FormValidation.error("Please set a numeric value");
            	}
            }
            
            return FormValidation.ok();
        }
        /**
         * Checks that provided host/port pairs are valid.
         * 
         * @param hostPortString The host/port pairs comma separated list as string
         * @return true, if all of the host/port pairs are valid
         */
        private boolean checkHostPortPairs(String hostPortString) {
        	boolean isValid = true;
        	String patternString = "[0-9a-zA-Z.]+:[0-9]+";
        	
        	Pattern pattern = Pattern.compile(patternString);
        	
        	String[] parts = hostPortString.split(",");
        	Matcher matcher = null;
        	int partCount = parts.length;
        	if(partCount == 0) {
    			matcher = pattern.matcher(hostPortString);
    			if(!matcher.find()) {
    				isValid = false;
    			}
        	} else {
    			for(String pair : parts) {
    				matcher = pattern.matcher(pair);
        			if(!matcher.find()) {
        				isValid = false;
        				break;
        			}
        		}
        	}
        	
        	return isValid;
        }
        
        /**
         * Checks if a string value is an integer.
         * 
         * @param inputString The input string to check.
         * @return	boolean. If true, then the input value is an integer value
         */
        public static boolean isInteger(String inputString) {
            boolean isValidInteger = false;
            
            try {
               Integer.parseInt(inputString);
               isValidInteger = true;
            }
            catch (NumberFormatException ex) {
               // inputString is not an integer
            }
       
            return isValidInteger;
         }
        
        public static boolean isLong(String inputString) {
            boolean isValidLong = false;
            
            try {
               Long.parseLong(inputString);
               isValidLong = true;
            }
            catch (NumberFormatException ex) {
               // inputString is not an long
            }
       
            return isValidLong;
         }
        
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Publish to Kafka";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
        	bootstrapServers = formData.getString("bootstrapServers");
        	metadataBrokerList = formData.getString("metadataBrokerList");
        	acks = formData.getString("acks");
        	topic = formData.getString("topic");
        	excludePlugin = formData.getBoolean("excludePlugin");
        	changeBuildStatus = formData.getBoolean("changeBuildStatus");
        	
        	// ^Can also use req.bindJSON(this, formData);
            //  (easier when there are many fields; need set* methods for this, like setUseFrench)
            save();
            return super.configure(req,formData);
        }

        public String getBootstrapServers() {
			return bootstrapServers;
		}

		public String getMetadataBrokerList() {
			return metadataBrokerList;
		}

		public String getAcks() {
			return acks;
		}

		public String getTopic() {
			return topic;
		}

		public boolean isExcludePlugin() {
			return excludePlugin;
		}

		public boolean isChangeBuildStatus() {
			return changeBuildStatus;
		}
    }

	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	public String getBootstrapServers() {
		if(bootstrapServers == null || bootstrapServers.equals("")) {
			return getDescriptor().getBootstrapServers();
		} else {
			return bootstrapServers;
		}
	}

	public String getMetadataBrokerList() {
		if(metadataBrokerList == null || metadataBrokerList.equals("")) {
			return getDescriptor().getMetadataBrokerList();
		} else {
			return metadataBrokerList;
		}
	}

	public String getAcks() {
		if(acks == null || acks.equals("")) {
			return getDescriptor().getAcks();
		} else {
			return acks;
		}
	}

	public String getTopic() {
		if(topic == null || topic.equals("")) {
			return getDescriptor().getTopic();
		} else {
			return topic;
		}
	}

	public boolean isExcludePlugin() {
		if(getDescriptor().isExcludePlugin()) {
			return true;
		} else {
			return excludePlugin;
		}
	}

	public boolean isChangeBuildStatus() {
		if(getDescriptor().isChangeBuildStatus()) {
			return true;
		} else {
			return changeBuildStatus;
		}
	}

	public int getRetries() {
		return retries;
	}
}