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
    private int lastLineRead;
    private static final String ERROR_MESSAGE = "ERROR";
    private static final String EXCEPTION_MESSAGE = "Exception";
    private long lastModified;

    public EventExtractor(EventRepo eventRepo, List<EventExtractionTag> extractionTags,
                          String managedProcessLogFilePath) {
        this.eventRepo = eventRepo;
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
        log.trace("Start reading from line {}", lastLineRead);
        List<NumberedLine> events = new ArrayList<>();
        try(Stream<NumberedLine> lines = lines(Paths.get(managedProcessLogFilePath))) {
            events = lines.skip(lastLineRead)
                    .filter(line -> {
                            lastLineRead = line.getNumber();
                            for (EventExtractionTag tag : extractionTags) {
                                String logLine = line.getLine();
                            Matcher matcher = Pattern.compile(tag.regex)
                                    .matcher(logLine);
                            boolean isMatch = matcher.find();
                            if (isMatch) {
                                line.setTagName(tag.tagName);
                                return true;
                            }
                            else if (logLine.contains(ERROR_MESSAGE)) {
                                line.setTagName(ERROR_MESSAGE);
                                return true;
                            }
                            else if (logLine.contains(EXCEPTION_MESSAGE)) {
                                line.setTagName(EXCEPTION_MESSAGE);
                                return true;
                            }
                        }
                        return false;
                    })
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Error in reading managed service's log file.", e);
        }
        log.trace("Line {} was the last line read", lastLineRead);
        log.trace("Matching lines: {} {}", events.size(), events);
        eventRepo.addEvents(events);
    }

    public static Stream<NumberedLine> lines(Path p) throws IOException {
        BufferedReader bufferedReader = Files.newBufferedReader(p);
        Spliterator<NumberedLine> spliterator = new Spliterators.AbstractSpliterator<NumberedLine>(
                Long.MAX_VALUE, Spliterator.ORDERED|Spliterator.NONNULL) {
            int lineNumber;
            public boolean tryAdvance(Consumer<? super NumberedLine> action) {
                String line;
                try {
                    line = bufferedReader.readLine();
                }
                catch(IOException e) {
                    throw new UncheckedIOException(e);
                }
                if (line == null) {
                    return false;
                }
                action.accept(new NumberedLine(++lineNumber, line));
                return true;
            }
        };
        return StreamSupport.stream(spliterator, false).onClose(() -> {
            try {
                bufferedReader.close();
            }
            catch(IOException e) {
                throw new UncheckedIOException(e);

            }
        });
    }

    @Override
    public String call() throws Exception {
        run();
        return null;
    }
}
