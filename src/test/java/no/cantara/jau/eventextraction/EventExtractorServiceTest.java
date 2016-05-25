package no.cantara.jau.eventextraction;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.cantara.cs.client.EventExtractionUtil;
import no.cantara.cs.dto.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

public class EventExtractorServiceTest {
    private static final Logger log = LoggerFactory.getLogger(EventExtractorServiceTest.class);

    @Test
    public void shouldGroupTagsByFileName() {
        EventExtractionConfig config = new EventExtractionConfig("jau");
        config.addEventExtractionTag(new EventExtractionTag("This a is a tag", "\\btest\\b", "path/to/log.log"));
        config.addEventExtractionTag(new EventExtractionTag("Seconds tag", "\\blalala\\b", "path/to/log.log"));

        Map<String, List<EventExtractionTag>> mapped = EventExtractionUtil.groupExtractionConfigsByFile(config);

        assertEquals(mapped.size(), 1);
        Assert.assertNotNull(mapped.get("path/to/log.log"));
    }

    @Test
    public void shouldExtractEventsFromFiles() throws Exception {
        EventRepo repo = new EventRepo();
        EventExtractorService service = new EventExtractorService(repo);
        EventExtractionConfig config = new EventExtractionConfig("jau");
        String filePath1 = getLogFile("jau-test-log.logg");
        String filePath2 = getLogFile("ma-test-log.logg");
        config.addEventExtractionTag(new EventExtractionTag("This a is a tag", "\\btest\\b",
                                                            filePath1));
        config.addEventExtractionTag(new EventExtractionTag("MDC tag test", "\\bmdc-tag-test\\b",
                                                            filePath2));
        List<EventExtractionConfig> configs = new ArrayList<>();
        configs.add(config);
        service.updateConfigs(configs);
        List<Event> events = service.extractEvents();

        assertNotEquals(events.size(), 0);

        ExtractedEventsStore mappedEvents = EventExtractionUtil.mapToExtractedEvents(events);
        ObjectMapper mapper = new ObjectMapper();
        log.info(mapper.writeValueAsString(mappedEvents));

        EventTag tag = mappedEvents.getEventGroup("jau").getEventFile(filePath1)
                                   .getEventTag("This a is a tag");
        List<Event> manuallyCollected = events.stream()
                                              .filter(e -> e.getTag().equals("This a is a tag"))
                                              .collect(Collectors.toList());

        assertEquals(tag.getEvents().size(), manuallyCollected.size());
    }

    @Test
    public void shouldExtractEventsFromMultipleLargeFiles() throws Exception {
        EventRepo repo = new EventRepo();
        EventExtractorService service = new EventExtractorService(repo);
        EventExtractionConfig config = new EventExtractionConfig("jau");
        String filePath1 = getLogFile("mymanagedapplication-2016-01-07.logg");
        String filePath2 = getLogFile("jau-2016-01-10.logg");
        config.addEventExtractionTag(new EventExtractionTag("This a is a tag", "\\b200\\b",
                                                            filePath1));
        config.addEventExtractionTag(new EventExtractionTag("MDC tag test", "\\bmdc-tag-test\\b",
                                                            filePath2));
        List<EventExtractionConfig> configs = new ArrayList<>();
        configs.add(config);
        service.updateConfigs(configs);

        List<Event> events = service.extractEvents();

        assertNotEquals(events.size(), 0);
    }

    private String getLogFile(String name) throws Exception {
        Files.deleteIfExists(Paths.get(name + ".properties"));
        return ClassLoader.getSystemResource(name).toURI().getPath();
    }

}
