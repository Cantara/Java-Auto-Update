package no.cantara.jau.eventextraction;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EventExtractorTest {
    private static final Logger log = LoggerFactory.getLogger(EventExtractorTest.class);
    private final static String LOG_FILE_PATH = "event-extractor-test-log-file.log";

    private String regularLogLine;
    private String errorLogLine;
    private String mdcLogLine;
    private String mdcLogLineNotOwnWord;
    private String exceptionLogLine;

    @BeforeClass
    public void createFileWithEvents() throws IOException {
        File logFile = Files.createFile(Paths.get(LOG_FILE_PATH)).toFile();

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(logFile), "utf-8"))) {
            regularLogLine = "17:16:26.828 [main] TRACE n.n.pharmacy.config.ApplicationMode - isDemoMode: false";
            errorLogLine = "12:34:45.035 [pool-1-thread-1] ERROR n.c.jau.coms.CheckForUpdateHelper - checkForUpdate failed, do nothing. Retrying in 60 seconds.";
            mdcLogLine = "13:28:13.343 this-is-an-mdc-tag [pool-1-thread-1] DEBUG no.cantara.jau.JavaAutoUpdater - Checking if process is running...";
            mdcLogLineNotOwnWord = "13:28:11.801 [main] DEBUG no.cantara.jau.Main - Resolved clientName=local-jau and " +
                    "hello_tags";
            exceptionLogLine = "17:18:11.666 [main] DEBUG n.n.p.config.PharmacyIdService - Exception cause getMessage=Could not get JDBC Connection; nested exception is java.sql.SQLException: Network error IOException: Connection refused";

            writer.write(regularLogLine);
            writer.newLine();
            writer.write(errorLogLine);
            writer.newLine();
            writer.write(mdcLogLine);
            writer.newLine();
            writer.write(mdcLogLineNotOwnWord);
            writer.newLine();
            writer.write(exceptionLogLine);
        }
    }

    @AfterClass
    public void deleteTestFile() throws IOException {
        Files.delete(Paths.get(LOG_FILE_PATH));
    }

    @Test
    public void shouldExtractEventsBasedOnTags() throws JsonProcessingException {
        List<String> mdcs = new ArrayList<>();
        mdcs.add("this-is-an-mdc-tag");
        EventRepo repo = new EventRepo(mdcs);
        EventExtractor extractor = new EventExtractor(repo, "this-is-an-mdc-tag,tags",
                LOG_FILE_PATH);

        extractor.run();

        Map<String, List<String>> eventsExtracted = repo.getEvents();
        Assert.assertEquals(eventsExtracted.size(), 3);

        Assert.assertEquals(eventsExtracted.get("this-is-an-mdc-tag").get(0), mdcLogLine);
        Assert.assertEquals(eventsExtracted.get("ERROR").get(0), errorLogLine);
        Assert.assertEquals(eventsExtracted.get("Exception").get(0), exceptionLogLine);
    }

    @Test
    public void shouldNotExtractEventsWhenFileIsUnmodified() throws InterruptedException {
        List<String> mdcs = new ArrayList<>();
        mdcs.add("this-is-an-mdc-tag");
        EventRepo repo = new EventRepo(mdcs);
        EventExtractor extractor = new EventExtractor(repo, "this-is-an-mdc-tag",
                LOG_FILE_PATH);

        extractor.run();

        Map<String, List<String>> eventsExtracted = repo.getEvents();
        Assert.assertEquals(eventsExtracted.size(), 3);

        extractor.run();

        eventsExtracted = repo.getEvents();
        Assert.assertEquals(eventsExtracted.size(), 3);
    }

}
