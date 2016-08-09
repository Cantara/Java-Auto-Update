package no.cantara.jau.eventextraction;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;

import no.cantara.cs.dto.event.Event;
import no.cantara.cs.dto.event.EventExtractionTag;

public class CommandExtractEventsFromFile extends HystrixCommand<Long> {
    private static final Logger log = LoggerFactory.getLogger(CommandExtractEventsFromFile.class);
    private static final String GROUP_KEY = "EXTRACT_EVENTS";
    private static final int COMMAND_TIMEOUT = 10_000;
    private static final int MAX_LINE_LENGTH = 1_000_000;
    private static final String ERROR_WORD = "ERROR";
    private static final String EXCEPTION_WORD = "Exception";
    private static final String STACK_TRACE_PREFIX = "\tat";
    private final EventRepo repo;
    private long lastLineRead;
    private final String filePath;
    private final String groupName;
    private final Pattern startPattern;
    private final List<EventExtractionTag> extractionTags;
    private boolean isException;

    protected CommandExtractEventsFromFile(EventRepo repo, long lastLineRead, String filePath,
                                           String groupName, String startPattern, List<EventExtractionTag> extractionTags) {
        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(GROUP_KEY))
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                        .withExecutionTimeoutInMilliseconds(COMMAND_TIMEOUT)));
        this.repo = repo;
        this.lastLineRead = lastLineRead;
        this.filePath = filePath;
        this.groupName = groupName;
        this.extractionTags = extractionTags;
        this.startPattern = startPattern == null ? null : Pattern.compile('^' + startPattern);
    }

    @Override
    protected Long run() throws Exception {
        log.trace("Start reading from line {} from file {}", lastLineRead, filePath);

        List<Event> events = new ArrayList<>();
        int lineNumber = -1;

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath), StandardCharsets.UTF_8)) {

            String line = reader.readLine();
            Event event = null;

            while (line != null) {
                if (++lineNumber > lastLineRead) {

                    if (hasStartPattern(line) || event == null) {
                        event = new Event(lineNumber, line);
                        if (matchLineToTags(event)) {
                            events.add(event);
                        }
                    } else if (event.getLine().length() < MAX_LINE_LENGTH) {
                        // Append to the current log event if this line is a continuation.
                        event.setLine(event.getLine() + "\n" + line);
                    }
                }
                line = reader.readLine();
            }

        } catch (IOException e) {
            log.error("Error reading log file {}", filePath, e);
        }

        lastLineRead = lineNumber;
        repo.addEvents(events);

        log.trace("Line {} was the last line read from file {}. Number of events in repo {}", lastLineRead, filePath, events.size());
        return lastLineRead;
    }

    private boolean hasStartPattern(String line) {
        return startPattern == null || startPattern.matcher(line).find();
    }

    private boolean matchLineToTags(Event line) {
        if (line == null) {
            return false;
        }

        for (EventExtractionTag tag : extractionTags) {
            if (matchLineToTag(line, tag)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchLineToTag(Event line, EventExtractionTag tag) {
        line.setGroupName(groupName);
        line.setFileName(filePath);
        String logLine = line.getLine();

        if (isException) {
            if (logLine.startsWith(STACK_TRACE_PREFIX)) {
                line.setTag(EXCEPTION_WORD);
                return true;
            }
            isException = false;
        }
        boolean isMatch = matchAgainstRegex(tag.regex, logLine);
        if (isMatch) {
            line.setTag(tag.tagName);
            return true;
        }
        else if (logLine.contains(ERROR_WORD)) {
            line.setTag(ERROR_WORD);
            return true;
        }
        else if (logLine.contains(EXCEPTION_WORD)) {
            line.setTag(EXCEPTION_WORD);
            isException = true;
            return true;
        }
        return false;
    }

    private boolean matchAgainstRegex(String regex, String logLine) {
        Matcher matcher = Pattern.compile(regex).matcher(logLine);
        return matcher.find();
    }
}
