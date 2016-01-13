package no.cantara.jau.eventextraction;

import no.cantara.jau.serviceconfig.dto.EventExtractionConfig;
import no.cantara.jau.serviceconfig.dto.EventExtractionTag;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EventExtractorServiceTest {

    @Test
    public void shouldGroupTagsByFileName() {
        EventExtractorService service = new EventExtractorService(null, null);

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
        EventExtractorService service = new EventExtractorService(repo, null);
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

        List<String> events = repo.getEvents().get("MDC tag test");

        Assert.assertEquals(events.size(), 3);
    }
}
