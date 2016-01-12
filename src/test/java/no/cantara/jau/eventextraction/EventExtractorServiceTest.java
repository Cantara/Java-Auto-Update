package no.cantara.jau.eventextraction;

import no.cantara.jau.serviceconfig.dto.EventExtractionConfig;
import no.cantara.jau.serviceconfig.dto.EventExtractionTag;
import org.testng.Assert;
import org.testng.annotations.Test;

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
}
