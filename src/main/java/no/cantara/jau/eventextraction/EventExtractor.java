package no.cantara.jau.eventextraction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class EventExtractor implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(EventExtractor.class);

    private final String[] mdcEvents;
    private final String managedProcessLogFilePath;
    private final File managedProcessLogFile;
    private final EventRepo eventRepo;
    private int lastLineRead;
    private static final String ERROR_MESSAGE = "ERROR";
    private static final String EXCEPTION_MESSAGE = "Exception";
    private long lastModified;

    public EventExtractor(EventRepo eventRepo, String mdcEventsUnsplit,
                          String managedProcessLogFilePath) {
        this.eventRepo = eventRepo;
        mdcEvents = mdcEventsUnsplit.split(",");
        this.managedProcessLogFilePath = managedProcessLogFilePath;
        managedProcessLogFile = new File(managedProcessLogFilePath);
        lastModified = 0;
        lastLineRead = 0;
    }

    @Override
    public void run() {
            if (managedProcessLogFile.lastModified() > lastModified) {
                lastModified = managedProcessLogFile.lastModified();
                checkForEvents();
            }
    }

    private void checkForEvents() {
        log.trace("Reading from line {}", lastLineRead);
        List<NumberedLine> events = new ArrayList<>();
        try(Stream<NumberedLine> lines = lines(Paths.get(managedProcessLogFilePath))) {
            events = lines.skip(lastLineRead)
                    .filter(line -> {
                        lastLineRead = line.getNumber();
                        for (String mdc : mdcEvents) {
                            String logLine = line.getLine();
                            Matcher matcher = Pattern.compile("\\b" + mdc + "\\b")
                                    .matcher(logLine);
                            boolean isMatch = matcher.find();

                            return isMatch || logLine.contains(ERROR_MESSAGE)
                                    || logLine.contains(EXCEPTION_MESSAGE);
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
}
