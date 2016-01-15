package no.cantara.jau.eventextraction;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.cantara.jau.eventextraction.dto.Event;
import no.cantara.jau.eventextraction.dto.EventTag;
import no.cantara.jau.eventextraction.dto.ExtractedEvents;
import no.cantara.jau.serviceconfig.dto.EventExtractionConfig;
import no.cantara.jau.serviceconfig.dto.EventExtractionTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.reporters.Files;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EventExtractorServiceTest {
    private static final Logger log = LoggerFactory.getLogger(EventExtractorServiceTest.class);

    @Test
    public void shouldGroupTagsByFileName() {
        EventExtractorService service = new EventExtractorService(null);

        EventExtractionConfig config = new EventExtractionConfig("jau");
        config.addEventExtractionTag(new EventExtractionTag("This a is a tag", "\\btest\\b", "path/to/log.log"));
        config.addEventExtractionTag(new EventExtractionTag("Seconds tag", "\\blalala\\b", "path/to/log.log"));

        Map<String, List<EventExtractionTag>> mapped = service.groupExtractionConfigsByFile(config);

        Assert.assertEquals(mapped.size(), 1);
        Assert.assertNotNull(mapped.get("path/to/log.log"));
    }

    @Test
    public void shouldExtractEventsFromFiles() throws InterruptedException, JsonProcessingException, URISyntaxException {
        EventRepo repo = new EventRepo();
        EventExtractorService service = new EventExtractorService(repo);
        EventExtractionConfig config = new EventExtractionConfig("jau");
        String filePath1 = ClassLoader.getSystemResource("jau-test-log.logg").toURI().getPath();
        String filePath2 = ClassLoader.getSystemResource("pa-test-log.logg").toURI().getPath();
        config.addEventExtractionTag(new EventExtractionTag("This a is a tag", "\\btest\\b",
                filePath1));
        config.addEventExtractionTag(new EventExtractionTag("MDC tag test", "\\bmdc-tag-test\\b",
                filePath2));
        List<EventExtractionConfig> configs = new ArrayList<>();
        configs.add(config);
        service.updateConfigs(configs);
        List<Event> events = service.extractEvents();

        Assert.assertNotEquals(events.size(), 0);

        ExtractedEvents mappedEvents = EventExtractorService.mapToExtractedEvents(events);
        ObjectMapper mapper = new ObjectMapper();
        log.info(mapper.writeValueAsString(mappedEvents));

        EventTag tag = mappedEvents.getEventGroup("jau").getEventFile(filePath1)
                .getEventTag("This a is a tag");
        List<Event> manuallyCollected = events.stream()
                .filter(e -> e.getTag().equals("This a is a tag"))
                .collect(Collectors.toList());

        Assert.assertEquals(tag.getEvents().size(), manuallyCollected.size());
    }

    @Test
    public void shouldExtractEventsFromMultipleLargeFiles() throws URISyntaxException {
        EventRepo repo = new EventRepo();
        EventExtractorService service = new EventExtractorService(repo);
        EventExtractionConfig config = new EventExtractionConfig("jau");
        String filePath1 = ClassLoader.getSystemResource("pharmacyagent-2016-01-07.logg").toURI().getPath();
        String filePath2 = ClassLoader.getSystemResource("jau-2016-01-10.logg").toURI().getPath();
        config.addEventExtractionTag(new EventExtractionTag("This a is a tag", "\\b200\\b",
                filePath1));
        config.addEventExtractionTag(new EventExtractionTag("MDC tag test", "\\bmdc-tag-test\\b",
                filePath2));
        List<EventExtractionConfig> configs = new ArrayList<>();
        configs.add(config);
        service.updateConfigs(configs);

        List<Event> events = service.extractEvents();

        Assert.assertNotEquals(events.size(), 0);
    }

}
