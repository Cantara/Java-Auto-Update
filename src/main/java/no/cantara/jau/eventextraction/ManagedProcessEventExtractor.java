package no.cantara.jau.eventextraction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
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

    public ManagedProcessEventExtractor(String mdcEventsUnsplit, String managedProcessLogFilePath) {
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
    }

    public static Stream<NumberedLine> lines(Path p) throws IOException {
        BufferedReader b=Files.newBufferedReader(p);
        Spliterator<NumberedLine> sp=new Spliterators.AbstractSpliterator<NumberedLine>(
                Long.MAX_VALUE, Spliterator.ORDERED|Spliterator.NONNULL) {
            int line;
            public boolean tryAdvance(Consumer<? super NumberedLine> action) {
                String s;
                try {
                    s = b.readLine();
                }
                catch(IOException e) {
                    throw new UncheckedIOException(e);
                }
                if (s == null) {
                    return false;
                }
                action.accept(new NumberedLine(++line, s));
                return true;
            }
        };
        return StreamSupport.stream(sp, false).onClose(() -> {
            try {
                b.close();
            }
            catch(IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }
}
