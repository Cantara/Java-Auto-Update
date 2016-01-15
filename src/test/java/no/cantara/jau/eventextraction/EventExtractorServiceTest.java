package no.cantara.jau.eventextraction;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.cantara.jau.eventextraction.dto.EventGroup;
import no.cantara.jau.eventextraction.dto.EventTag;
import no.cantara.jau.eventextraction.dto.ExtractedEvents;
import no.cantara.jau.serviceconfig.dto.EventExtractionConfig;
import no.cantara.jau.serviceconfig.dto.EventExtractionTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

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
    public void shouldReadTagsFromFiles() throws InterruptedException {
        EventRepo repo = new EventRepo();
        EventExtractorService service = new EventExtractorService(repo);
        EventExtractionConfig config = new EventExtractionConfig("jau");
        config.addEventExtractionTag(new EventExtractionTag("This a is a tag", "\\btest\\b",
                "external_testdata/jau-test-log.logg"));
        config.addEventExtractionTag(new EventExtractionTag("MDC tag test", "\\bmdc-tag-test\\b",
                "external_testdata/pa-test-log.logg"));
        List<EventExtractionConfig> configs = new ArrayList<>();
        configs.add(config);
        service.updateConfigs(configs);

        service.runEventExtractors();

        Thread.sleep(500);

        List<EventLine> events = repo.getEvents();
        Assert.assertNotEquals(events.size(), 0);

        ExtractedEvents mappedEvents = EventExtractorService.mapToExtractedEvents(events);
        EventTag tag = mappedEvents.getEventGroup("jau").getEventFile("external_testdata/jau-test-log.logg")
                .getEventTag("This a is a tag");

        List<EventLine> manuallyCollected = events.stream()
                .filter(e -> e.getTag().equals("This a is a tag"))
                .collect(Collectors.toList());

        Assert.assertEquals(tag.getEvents().size(), manuallyCollected.size());
    }
}
