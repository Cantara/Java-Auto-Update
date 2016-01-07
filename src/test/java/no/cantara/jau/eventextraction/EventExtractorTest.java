package no.cantara.jau.eventextraction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class EventExtractorTest {
    private static final Logger log = LoggerFactory.getLogger(EventExtractorTest.class);
    private final static String LOG_FILE_PATH = "event-extractor-test-log-file.log";

    private String regularLogLine;
    private String errorLogLine;
    private String mdcLogLine;

    @BeforeClass
    public void createFileWithEvents() throws IOException {
        File logFile = Files.createFile(Paths.get(LOG_FILE_PATH)).toFile();

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(logFile), "utf-8"))) {
            regularLogLine = "17:16:26.828 [main] TRACE n.n.pharmacy.config.ApplicationMode - isDemoMode: false";
            errorLogLine = "12:34:45.035 [pool-1-thread-1] ERROR n.c.jau.coms.CheckForUpdateHelper - checkForUpdate failed, do nothing. Retrying in 60 seconds.";
            mdcLogLine = "13:28:13.343 this-is-an-mdc-tag [pool-1-thread-1] DEBUG no.cantara.jau.JavaAutoUpdater - Checking if process is running...";

            writer.write(regularLogLine);
            writer.newLine();
            writer.write(errorLogLine);
            writer.newLine();
            writer.write(mdcLogLine);
        }
    }

    @AfterClass
    public void deleteTestFile() throws IOException {
        Files.delete(Paths.get(LOG_FILE_PATH));
    }

    @Test
    public void shouldExtractEventsBasedOnTags() {
        EventRepo repo = new EventRepo();
        EventExtractor extractor = new EventExtractor(repo, "this-is-an-mdc-tag", LOG_FILE_PATH);

        extractor.run();

        List<NumberedLine> eventsExtracted = repo.getEvents();
        Assert.assertEquals(eventsExtracted.size(), 2);

        String event1 = eventsExtracted.get(0).getLine();
        String event2 = eventsExtracted.get(1).getLine();
        boolean shouldHaveBeenExtracted = (event1.equals(errorLogLine) || event1
                .equals(mdcLogLine)) && (event2.equals(errorLogLine) || event2
                .equals(mdcLogLine));

        Assert.assertTrue(shouldHaveBeenExtracted);
    }
}
