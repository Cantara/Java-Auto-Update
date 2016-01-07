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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ManagedProcessEventExtractor implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(ManagedProcessEventExtractor.class);

    private String[] mdcEvents;
    private String managedProcessLogFilePath;
    private File managedProcessLogFile;
    private long lastModified;
    private int lastLineRead;
    private EventRepo eventRepo;

    public ManagedProcessEventExtractor(EventRepo eventRepo, String mdcEventsUnsplit,
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
        while (true) {
            if (managedProcessLogFile.lastModified() > lastModified) {
                lastModified = managedProcessLogFile.lastModified();
                checkForEvents();
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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
                            if (line.getLine().contains(mdc) ||
                                    line.getLine().contains("ERROR")) {
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
}
