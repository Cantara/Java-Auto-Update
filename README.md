# Java-Auto-Update
Java Auto-Update - wrapper to automatically upgrade a Java application. 

![GitHub tag (latest SemVer)](https://img.shields.io/github/v/tag/Cantara/Java-Auto-Update)
![Build Status](https://jenkins.quadim.ai/buildStatus/icon?job=Java-Auto-Update) [![Project Status: Active – The project has reached a stable, usable state and is being actively developed.](http://www.repostatus.org/badges/latest/active.svg)](http://www.repostatus.org/#active) [![Known Vulnerabilities](https://snyk.io/test/github/Cantara/Java-Auto-Update/badge.svg)](https://snyk.io/test/github/Cantara/Java-Auto-Update)

## Install on Windows 

1. Download zip file from https://mvnrepo.cantara.no/content/repositories/releases/no/cantara/jau/java-auto-update/0.11.1/java-auto-update-0.11.1.zip
2. unzip 
3. Update configServiceUrl in config_override/jau.properties
4. Execute download-java.bat. Use the parameter extraSecurity if you want to add the optional cryptography extension kit
5. Open command prompt with 'Run as Administrator'
6. cd java-auto-update-0.10.5-SNAPSHOT\bin
7. java-auto-update remove & java-auto-update install
8. java-auto-update start

Tip! If it fails to start, check the wrapper.log.   


## Run on Linux 

1. wget https://mvnrepo.cantara.no/content/repositories/releases/no/cantara/jau/java-auto-update/0.10.5/java-auto-update-0.10.5.jar
2. java -Dconfigservice.url=http://localhost:7000/jau/serviceconfig/query?clientid=clientid1 -jar java-auto-update-0.10.5.jar


## Configuration


If JAU is running behind a proxy, use the following properties
* "http.useProxy"
* "http.proxyPort"
* "http.proxyHost"
* "https.proxyHost"
* "https.proxyPort"

If JAU is running behind a proxy that performs SSL sniffing, you can use the
following property to ignore certificate validation for given domains. Matches
the Subject DN of the certificate, so if a wildcard certificate is used, specify
like "*.example.com". Separate multiple domains using a comma.

- `disable.tlscheck.domains`, e.g.
  `disable.tlscheck.domains="*.example.com,subdomain.domain.com"`

jau.properties
```
configservice.url=http://localhost:8086/jau/client
configservice.username=read
configservice.password=baretillesing
configservice.artifactid=cantara-demo

updateinterval=60
isrunninginterval=40

clientName=local-jau

monitor.events=testkey

# "startPattern" is a regex defining the start of a log entry. Setting this property causes multi-line
# log entries to be collated before being sent to ConfigService.
# For example, if your log entries starts with a UTC timestamp (e.g., 2016-06-09T14:01:05.348) you can
# use the following regex:

# startPattern=\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}
```


## Example application configuration JAU receives from ConfigService

```
{
  "name": "hello-world_0.1-SNAPSHOT",
  "lastChanged": "2016-03-09T07:50:18.994Z",
  "downloadItems": [
    {
      "url": "repository-url/hello-world-0.1-SNAPSHOT.jar",
      "username": "basic-auth-username",
      "password": "basic-auth-password",
      "metadata": {
        "groupId": "com.example",
        "artifactId": "hello-world-service",
        "version": "0.1-SNAPSHOT",
        "packaging": "jar",
        "lastUpdated": null,
        "buildNumber": null
      }
    }
  ],
  "configurationStores": [
    {
      "fileName": "helloworld_overrides.properties",
      "properties": {
        "hello.world.message": "Hello World"
      }
    }
  ],
  "eventExtractionConfigs" : [ {
     "groupName" : "hw-agent",
     "tags" : [ {
       "tagName" : "jau",
       "regex" : ".*",
       "filePath" : "logs/jau.log"
     }, {
       "tagName" : "agent",
       "regex" : ".*",
       "filePath" : "logs/hwagent.log"
     } ]
   } ],

  ],
  "startServiceScript": "java -jar hello-world-0.1-SNAPSHOT.jar"
}
```

## Develop

Java Auto-Update depends on ConfigService, so this must be available in your maven repositories.
You can download ConfigService from github: https://github.com/Cantara/ConfigService

