package no.cantara.jau.eventextraction;

import no.cantara.jau.serviceconfig.dto.EventExtractionTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class EventExtractor implements Callable<String> {
    private static final Logger log = LoggerFactory.getLogger(EventExtractor.class);

    private final List<EventExtractionTag> extractionTags;
    private final String managedProcessLogFilePath;
    private final File managedProcessLogFile;
    private final EventRepo eventRepo;
    private final String groupName;
    private int lastLineRead;
    private static final String ERROR_MESSAGE = "ERROR";
    private static final String EXCEPTION_MESSAGE = "Exception";
    private long lastModified;

    public EventExtractor(EventRepo eventRepo, List<EventExtractionTag> extractionTags,
                          String managedProcessLogFilePath, String groupName) {
        this.eventRepo = eventRepo;
        this.groupName = groupName;
        this.extractionTags = extractionTags;
        this.managedProcessLogFilePath = managedProcessLogFilePath;
        managedProcessLogFile = new File(managedProcessLogFilePath);
        lastModified = 0;
        lastLineRead = 0;
    }

    public void run() {
            if (managedProcessLogFile.lastModified() > lastModified) {
                log.trace("File={} is modified since last extraction. Extracting...",
                        managedProcessLogFilePath);
                lastModified = managedProcessLogFile.lastModified();
                checkForEvents();
            }
            else {
                log.trace("File={} has not been modified since last extraction. Will not extract",
                        managedProcessLogFilePath);
            }
    }

    private void checkForEvents() {
        try {
            lastLineRead = new CommandExtractEventsFromFile(eventRepo, lastLineRead,
                    managedProcessLogFilePath, groupName, extractionTags).run();
        } catch (Exception e) {
            log.error("Failed extraction events from file!", e);
        }
    }

    @Override
    public String call() throws Exception {
        run();
        return null;
    }
}
