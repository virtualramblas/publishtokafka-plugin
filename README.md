# publishtokafka-plugin  
  
A Jenkins plugin to publish build jobs execution data to Kafka topics.  
  
### What's Jenkins?  
[Jenkins](https://jenkins.io/) is probably the leading Open Source automation server, implemented in Java. It provides continuous integration services and hundreds of plugins to support building, deploying and automating any project. You can extend the Jenkins features writing your own custom plugins.    
  
### What's Kafka?  
[Kafka](http://kafka.apache.org/) is an Open Source message broker written in [Scala](http://www.scala-lang.org/) and currently maintained by the [Apache Software Foundation](http://www.apache.org/). It is fast, scalable, and has a modern cluster-centric design aimed to provide strong durability and fault-tolerance guarantees.  
  
### Features  
This plugin publishes the build jobs execution details in JSON format to a Kafka topic. At the moment just the general info about a build job are collected. The next releases will publish also details about the host (master or slave) where a build job has been executed, the full set of parameters values for a given execution and all of the details about each single build step. Then starting from the destination topic they can be consumed by other systems in order to generate stats and/or perform analytics upon them.  
At the moment just a small set of parameters can be configured for the underlying Kafka producer. Future releases will allow a more advanced configuration.
  
### How to build and install it  
The plugin project has been initially created from scratch through [Maven](https://maven.apache.org/) (same as for the other available Jenkins plugins), so you need Maven in order to build it starting from the source code. Please read this [post](http://googlielmo.blogspot.ie/2015/07/implementing-jenkins-plugin-from.html) in my personal blog in order to understand how to set up Maven to work locally with Jenkins plugins.    
After downloading the project you need to move to the root directory of the project (the one containing the *pom.xml* file) and run the following command:  
  
*mvn compile*  
    
or start to look at how it works:  
  
*mvn hpi:run*  
    
In order to produce the *.hpi* installer run the following Maven command:  
    
*mvn install*  
   
Then you can install it on your server simply uploading it through the *Manage Jenkins -> Manage Plugins-> Advanced* tab of the Jenkins dashboard.  
I didn't use intentionally any Java 8 feature while implementing this plugin in order to ensure compatibility with Jenkins servers running on JVM 7 as well. 
  
### Releases 
At present time just one release (0.7) is available.
  
      