package no.cantara.jau.eventextraction;

import no.cantara.cs.dto.event.EventExtractionTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

public class EventExtractor implements Callable<String> {
    private static final Logger log = LoggerFactory.getLogger(EventExtractor.class);

    private final List<EventExtractionTag> extractionTags;
    private final String managedProcessLogFilePath;
    private final File managedProcessLogFile;
    private final EventRepo eventRepo;
    private final String groupName;
    private int lastLineRead;
    private long lastModified;
    private long lastFileSize;

    public EventExtractor(EventRepo eventRepo, List<EventExtractionTag> extractionTags,
                          String managedProcessLogFilePath, String groupName) {
        this.eventRepo = eventRepo;
        this.groupName = groupName;
        this.extractionTags = extractionTags;
        this.managedProcessLogFilePath = managedProcessLogFilePath;
        managedProcessLogFile = new File(managedProcessLogFilePath);
        lastModified = 0;
        lastLineRead = 0;
        lastFileSize = 0;
    }

    private void checkForEvents() {
        if (fileHasBeenModified()) {
            try {
                lastLineRead = new CommandExtractEventsFromFile(eventRepo, lastLineRead,
                        managedProcessLogFilePath, groupName, extractionTags).run();
            } catch (Exception e) {
                log.error("Failed extraction events from file!", e);
            }
        }
    }

    private boolean fileHasBeenModified() {
        if (managedProcessLogFile.lastModified() > lastModified) {
            log.trace("{} is modified since last extraction. Extracting...", managedProcessLogFilePath);
            lastModified = managedProcessLogFile.lastModified();

            // If file has shrunk (due to rolling log files), start reading from the first line.
            if (managedProcessLogFile.length() < lastFileSize) {
                log.info("{} has shrunk since last extraction. Starting from first line.", managedProcessLogFilePath);
                lastFileSize = managedProcessLogFile.length();
                lastLineRead = 0;
            }

            return true;
        }
        log.trace("{} has not been modified since last extraction. Will not extract.", managedProcessLogFilePath);
        return false;
    }

    @Override
    public String call() throws Exception {
        checkForEvents();
        return null;
    }
}
