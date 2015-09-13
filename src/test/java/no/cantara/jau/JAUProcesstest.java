package no.cantara.jau;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.cantara.jau.serviceconfig.client.ConfigServiceClient;
import no.cantara.jau.serviceconfig.client.ConfigurationStoreUtil;
import no.cantara.jau.serviceconfig.client.DownloadUtil;
import no.cantara.jau.serviceconfig.dto.ServiceConfig;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;

import java.io.File;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import static java.util.concurrent.TimeUnit.MILLISECONDS;


public class JAUProcesstest {

    private static final ObjectMapper mapper = new ObjectMapper();

    @BeforeClass
    public void startServer() throws InterruptedException {

    }


    @AfterClass
    public void stop() {
    }


    @Test
    public void testProcessDownloadStartupAndRunning() throws Exception {


        String jsonResponse = "{  \n" +
                "   \"name\":\"Service1-1.23\",\n" +
                "   \"changedTimestamp\":\"2015-08-11T10:13:12.141Z\",\n" +
                "   \"downloadItems\":[  \n" +
                "      {  \n" +
                "         \"url\":\"http://mvnrepo.capraconsulting.no/service/local/artifact/maven/redirect?r=nmdsnapshots&g=no.nmd.pharmacy&a=pharmacy-agent&v=0.7-SNAPSHOT&p=jar\",\n" +
                "         \"username\":\"nmdread\",\n" +
                "         \"password\":\"gr0ssistmelding\",\n" +
                "         \"metadata\":{  \n" +
                "            \"groupId\":\"no.nmd.pharmacy\",\n" +
                "            \"artifactId\":\"pharmacy-agent\",\n" +
                "            \"version\":\"0.7-SNAPSHOT\",\n" +
                "            \"packaging\":\"jar\",\n" +
                "            \"lastUpdated\":null,\n" +
                "            \"buildNumber\":null\n" +
                "         }\n" +
                "      }\n" +
                "   ],\n" +
                "   \"configurationStores\":[  \n" +
                "      {  \n" +
                "         \"fileName\":\"config_override.properties\",\n" +
                "         \"properties\":{  \n" +
                "            \"stocklevel.aws.accessKey\":\"AKIAJ52X4LP557TOXSWA\",\n" +
                "            \"stocklevel.aws.secretKey\":\"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\",\n" +
                "            \"stocklevel.aws.destination.name\":\"nmd_devtest_stocklevel\",\n" +
                "            \"stocklevel.aws.region\":\"us-east-1\",\n" +
                "            \"jms.rest.host\":\"localhost\"\n" +
                "         }\n" +
                "      }\n" +
                "   ],\n" +
                "   \"startServiceScript\":\"java -DDEMO_MODE=true -jar pharmacy-agent-0.7-SNAPSHOT.jar\"\n" +
                "}";

        // let us type a configuration the quick way..
        ServiceConfig serviceConfig = mapper.readValue(jsonResponse, ServiceConfig.class);

        // Process stuff
        ApplicationProcess processHolder= new ApplicationProcess();
        processHolder.setWorkingDirectory(new File("./"));
        String workingDirectory = processHolder.getWorkingDirectory().getAbsolutePath();


        // Download stuff
        DownloadUtil.downloadAllFiles(serviceConfig.getDownloadItems(), workingDirectory);
        ConfigurationStoreUtil.toFiles(serviceConfig.getConfigurationStores(), workingDirectory);

        Properties initialApplicationState = new Properties();
        String initialCommand = getStringProperty(initialApplicationState, ConfigServiceClient.COMMAND, null);
        processHolder.setCommand(initialCommand.split("\\s+"));




    }

    private static String getStringProperty(final Properties properties, String propertyKey, String defaultValue) {
        String property = properties.getProperty(propertyKey, defaultValue);
        if (property == null) {
            //-Dconfigservice.url=
            property = System.getProperty(propertyKey);
        }
        return property;
    }


}