# publishtokafka-plugin  
  
A Jenkins post-build action plugin to publish build jobs execution data to Kafka topics.  
  
### What's Jenkins?  
[Jenkins](https://jenkins.io/) is probably the leading Open Source automation server, implemented in Java. It provides continuous integration services and hundreds of plugins to support building, deploying and automating any project. You can extend the Jenkins features writing your own custom plugins.    
  
### What's Kafka?  
[Kafka](http://kafka.apache.org/) is an Open Source message broker written in [Scala](http://www.scala-lang.org/) and currently maintained by the [Apache Software Foundation](http://www.apache.org/). It is fast, scalable, and has a modern cluster-centric design aimed to provide strong durability and fault-tolerance guarantees.  
  
### Features  
This plugin publishes the build jobs execution details in JSON format to a Kafka topic. In the current release the general info about a build job, details about the host (master or slave) where a build job has been executed, the full set of parameters values for a given execution and the Git build data are collected. Future releases will collect also all of the details for each single build step.  Here's an example of the current JSON message content sent to a topic:  
  
```json  
{
	"buildNumber":7,
	"name":"KTest",
	"startDate":{
		"date":8,"day":3,"hours":12,"minutes":14,"month":2,"seconds":57,
		"time":1488975297587,"timezoneOffset":0,"year":117
	},
	"endDate":{
		"date":8,"day":3,"hours":12,"minutes":15,"month":2,"seconds":10,
		"time":1488975310027,"timezoneOffset":0,"year":117
	},
	"duration":12440,
	"result":"SUCCESS",
	"parameters":{
        "MONGODB_HOST":"mongodb1.xxx.com",
        "MONGODB_PORT":"27018"
    },
	"nodeName":"master",
	"computerName":"XXXXXXXXX",
	"gitDetails":{
		"remoteURLs":["https://github.com/virtualramblas/publishtokafka-plugin.git"],
		"lastBuiltRevision":{
			"branches":[{"SHA1":{"firstByte":73,"name":"493ce83487eef93bb119a618ce62811f79fdb26a"},
"SHA1String":"493ce83487eef93bb119a618ce62811f79fdb26a",
"name":"refs/remotes/origin/master"}],
			"sha1":{"firstByte":73,"name":"493ce83487eef93bb119a618ce62811f79fdb26a"},
			"sha1String":"493ce83487eef93bb119a618ce62811f79fdb26a"
		}
	},
	"nodeDetails":{
		"nodeLabels":"master",
		"OS":"Windows_NT",
		"numberOfProcessors":"4",
		"processorArchitecture":"x86",
		"processorArchitectureW6432":"AMD64",
		"processorIdentifier":"Intel64 Family 6 Model 78 Stepping 3, GenuineIntel",
		"computerName":"XXXXXXXXX"
	}
}  
```  
  
After publishing to the destination topic data can then be consumed by other systems in order to generate stats and/or perform analytics.  
  
### How to build and install it  
The plugin project has been initially created from scratch through [Maven](https://maven.apache.org/) (same as for the other available Jenkins plugins), so you need Maven in order to build it starting from the source code. Please read this [post](http://googlielmo.blogspot.ie/2015/07/implementing-jenkins-plugin-from.html) in my personal blog in order to understand how to set up Maven to work locally with Jenkins plugins.    
After downloading the project you need to move to the root directory of the project (the one containing the *pom.xml* file) and run the following command to build it:  
  
*mvn compile*  
    
or start to look at how it works:  
  
*mvn hpi:run*  
    
In order to produce the *.hpi* installer run the following Maven command:  
    
*mvn install*  
   
Then you can install it on your server simply uploading it through the *Manage Jenkins -> Manage Plugins-> Advanced* tab of the Jenkins dashboard.  
I didn't use intentionally any Java 8 feature while implementing this plugin in order to ensure compatibility with Jenkins servers still running on JVM 7 as well. 
  
### Releases 
4 releases available so far:  
* 1.0: Implemented enhancements [#10](https://github.com/virtualramblas/publishtokafka-plugin/issues/10) and [#11](https://github.com/virtualramblas/publishtokafka-plugin/issues/11). Code refactoring.  
* 0.9: Fixed bug [#8](https://github.com/virtualramblas/publishtokafka-plugin/issues/8). Implemented enhancement [#7](https://github.com/virtualramblas/publishtokafka-plugin/issues/7) and change request [#6](https://github.com/virtualramblas/publishtokafka-plugin/issues/6).  
* 0.8: Fixed bug [#4](https://github.com/virtualramblas/publishtokafka-plugin/issues/4) . Implemented enhancements [#1](https://github.com/virtualramblas/publishtokafka-plugin/issues/1) and [#2](https://github.com/virtualramblas/publishtokafka-plugin/issues/2)  
* 0.7: initial release
  
      