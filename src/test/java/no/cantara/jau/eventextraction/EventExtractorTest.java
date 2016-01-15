package no.cantara.jau.eventextraction;

import no.cantara.jau.serviceconfig.dto.event.Event;
import no.cantara.jau.serviceconfig.dto.event.EventExtractionTag;
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
    public void shouldExtractEventsBasedOnTags() throws Exception {
        EventRepo repo = new EventRepo();
        List<EventExtractionTag> tags = new ArrayList<>();
        tags.add(new EventExtractionTag("this-is-an-mdc-tag", "\\bthis-is-an-mdc-tag\\b", LOG_FILE_PATH));
        EventExtractor extractor = new EventExtractor(repo, tags, LOG_FILE_PATH, "jau");

        extractor.call();

        List<Event> eventsExtracted = repo.getEvents();

        Assert.assertNotEquals(eventsExtracted.size(), 0);
    }

    @Test
    public void shouldNotExtractEventsWhenFileIsUnmodified() throws Exception {
        EventRepo repo = new EventRepo();
        List<EventExtractionTag> tags = new ArrayList<>();
        tags.add(new EventExtractionTag("this-is-an-mdc-tag", "\\bthis-is-an-mdc-tag\\b", LOG_FILE_PATH));
        EventExtractor extractor = new EventExtractor(repo, tags, LOG_FILE_PATH, "jau");

        extractor.call();

        List<Event> eventsExtracted = repo.getEvents();
        Assert.assertEquals(eventsExtracted.size(), 3);

        extractor.call();

        eventsExtracted = repo.getEvents();
        Assert.assertEquals(eventsExtracted.size(), 3);
    }
}
