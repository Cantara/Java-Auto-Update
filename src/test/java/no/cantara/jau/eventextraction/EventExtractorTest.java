package no.cantara.jau.eventextraction;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class EventExtractorTest {
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
    public void shouldExtractEventsBasedOnTags() {
        EventRepo repo = new EventRepo();
        EventExtractor extractor = new EventExtractor(repo, "this-is-an-mdc-tag,tags",
                LOG_FILE_PATH);

        extractor.run();

        List<NumberedLine> eventsExtracted = repo.getEvents();
        Assert.assertEquals(eventsExtracted.size(), 3);

        String event1 = eventsExtracted.get(0).getLine();
        String event2 = eventsExtracted.get(1).getLine();
        String event3 = eventsExtracted.get(2).getLine();
        boolean shouldHaveBeenExtracted =
                (event1.equals(errorLogLine) || event1.equals(mdcLogLine) || event1.equals(exceptionLogLine)) &&
                (event2.equals(errorLogLine) || event2.equals(mdcLogLine) || event2.equals(exceptionLogLine)) &&
                        (event3.equals(errorLogLine) || event3.equals(mdcLogLine) || event3.equals(exceptionLogLine));

        Assert.assertTrue(shouldHaveBeenExtracted);
    }

    public void shouldNotExtractEventsWhenMDCTagIsPartOfWord() {
        EventRepo repo = new EventRepo();
        EventExtractor extractor = new EventExtractor(repo, "this-is-an-mdc-tag", LOG_FILE_PATH);

        extractor.run();
    }
}
