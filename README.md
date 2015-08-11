# Java-Auto-Update
Java Auto-Update - wrapper to automatically upgrade a Java application. 


## Install on Windows 

1. Download zip file from http://mvnrepo.cantara.no/content/repositories/snapshots/no/cantara/jau/java-auto-update/1.0-SNAPSHOT/java-auto-update-1.0-20150731.054516-1.zip 
2. unzip 
3. Update configServiceUrl in config_override/config.properties 
4. Execute download-java.bat
5. Open command prompt with 'Run as Administrator'
6. cd java-auto-update-1.0-SNAPSHOT\bin
7. java-auto-update remove & java-auto-update install
8. java-auto-update start

Tip! If it fails to start, check the wrapper.log.   


## Run on Linux 

1. wget http://mvnrepo.cantara.no/content/repositories/snapshots/no/cantara/jau/java-auto-update/1.0-SNAPSHOT/java-auto-update-1.0-20150731.054516-1.jar
2. java -DconfigServiceUrl=http://localhost:7000/jau/serviceconfig/query?clientid=clientid1 -jar java-auto-update-1.0-20150731.054516-1.jar
