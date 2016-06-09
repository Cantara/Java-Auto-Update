package no.cantara.jau.eventextraction;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import no.cantara.cs.dto.event.Event;
import no.cantara.cs.dto.event.EventExtractionTag;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

public class EventExtractorTest {

    private final static String LOG_FILE_PATH = "event-extractor-test-log-file.log";
    private final static String LOG_FILE_PROPERTIES_PATH = "event-extractor-test-log-file.log.properties";

    private String regularLogLine;
    private String errorLogLine;
    private String mdcLogLine;
    private String mdcLogLineNotOwnWord;
    private String exceptionLogLine;
    private String continuedLogLine;
    private EventRepo repo;
    private List<EventExtractionTag> tags;

    @BeforeMethod
    public void createFileWithEvents() throws Exception {
        Files.deleteIfExists(Paths.get(LOG_FILE_PROPERTIES_PATH));

        regularLogLine = "17:16:26.828 [main] TRACE n.n.pharmacy.config.ApplicationMode - isDemoMode: false";
        errorLogLine = "12:34:45.035 [pool-1-thread-1] ERROR n.c.jau.coms.CheckForUpdateHelper - checkForUpdate failed, do nothing. Retrying in 60 seconds.";
        mdcLogLine = "13:28:13.343 this-is-an-mdc-tag [pool-1-thread-1] DEBUG no.cantara.jau.JavaAutoUpdater - Checking if process is running...";
        mdcLogLineNotOwnWord = "13:28:11.801 [main] DEBUG no.cantara.jau.Main - Resolved clientName=local-jau and hello_tags";
        exceptionLogLine = "17:18:11.666 [main] DEBUG n.n.p.config.PharmacyIdService - Exception cause getMessage=Could not get JDBC Connection; nested exception is java.sql.SQLException: Network error IOException: Connection refused";
        continuedLogLine = "Caused by NullPointerException";

        addLine(regularLogLine);
        addLine(errorLogLine);
        addLine(mdcLogLine);
        addLine(mdcLogLineNotOwnWord);
        addLine(exceptionLogLine);
        addLine(continuedLogLine);

        repo = new EventRepo();
        tags = Collections.singletonList(new EventExtractionTag("this-is-an-mdc-tag", "\\bthis-is-an-mdc-tag\\b", LOG_FILE_PATH));
    }

    private void addLine(String line) throws Exception {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(LOG_FILE_PATH, true), "utf-8"))) {
            writer.write(line);
            writer.newLine();
        }
    }

    @AfterMethod
    public void deleteTestFile() throws IOException {
        Files.deleteIfExists(Paths.get(LOG_FILE_PATH));
        Files.deleteIfExists(Paths.get(LOG_FILE_PROPERTIES_PATH));
    }

    @Test
    public void shouldExtractEventsBasedOnTags() throws Exception {
        EventExtractor extractor = new EventExtractor(repo, tags, LOG_FILE_PATH, "jau", null);
        extractor.call();

        List<Event> eventsExtracted = repo.getEvents();

        assertEquals(eventsExtracted.size(), 4);

        assertEquals(eventsExtracted.get(0).getLine(), errorLogLine);
        assertEquals(eventsExtracted.get(0).getTag(), "ERROR");

        assertEquals(eventsExtracted.get(1).getLine(), mdcLogLine);
        assertEquals(eventsExtracted.get(1).getTag(), "this-is-an-mdc-tag");

        assertEquals(eventsExtracted.get(2).getLine(), exceptionLogLine);
        assertEquals(eventsExtracted.get(2).getTag(), "Exception");

        assertEquals(eventsExtracted.get(3).getLine(), continuedLogLine);
        assertEquals(eventsExtracted.get(3).getTag(), "Exception");
    }

    @Test
    public void shouldCollateLines() throws Exception {
        EventExtractor extractor = new EventExtractor(repo, tags, LOG_FILE_PATH, "jau", "\\d{2}:\\d{2}:\\d{2}\\.\\d{3}");
        extractor.call();

        List<Event> eventsExtracted = repo.getEvents();

        assertEquals(eventsExtracted.size(), 3);

        assertEquals(eventsExtracted.get(0).getLine(), errorLogLine);
        assertEquals(eventsExtracted.get(0).getTag(), "ERROR");

        assertEquals(eventsExtracted.get(1).getLine(), mdcLogLine);
        assertEquals(eventsExtracted.get(1).getTag(), "this-is-an-mdc-tag");

        assertEquals(eventsExtracted.get(2).getLine(), exceptionLogLine + "\n" + continuedLogLine);
        assertEquals(eventsExtracted.get(2).getTag(), "Exception");
    }

    @Test
    public void shouldNotExtractEventsWhenFileIsUnmodified() throws Exception {
        EventRepo repo = new EventRepo();
        List<EventExtractionTag> tags = new ArrayList<>();
        tags.add(new EventExtractionTag("this-is-an-mdc-tag", "\\bthis-is-an-mdc-tag\\b", LOG_FILE_PATH));
        EventExtractor extractor = new EventExtractor(repo, tags, LOG_FILE_PATH, "jau", null);

        extractor.call();

        List<Event> eventsExtracted = repo.getEvents();
        assertEquals(eventsExtracted.size(), 4);

        extractor.call();

        eventsExtracted = repo.getEvents();
        assertEquals(eventsExtracted.size(), 4);
    }

    @Test
    public void shouldRememberState() throws Exception {
        EventExtractor extractor = new EventExtractor(repo, tags, LOG_FILE_PATH, "jau", null);
        extractor.call();

        List<Event> eventsExtracted = repo.getEvents();
        assertEquals(eventsExtracted.size(), 4);

        // Add a line, then create new extractor.
        Thread.sleep(1000);  // To make sure last-modified date of file changes.
        repo.clearEvents();
        addLine(errorLogLine);
        extractor = new EventExtractor(repo, tags, LOG_FILE_PATH, "jau", null);
        extractor.call();

        eventsExtracted = repo.getEvents();
        assertEquals(eventsExtracted.size(), 1);
        assertEquals(eventsExtracted.get(0).getLine(), errorLogLine);
    }
}
