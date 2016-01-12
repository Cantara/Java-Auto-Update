package no.cantara.jau.eventextraction;

import no.cantara.jau.serviceconfig.client.ConfigServiceClient;
import no.cantara.jau.serviceconfig.dto.EventExtractionConfig;
import no.cantara.jau.serviceconfig.dto.EventExtractionTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.stream.Collectors.groupingBy;

public class EventExtractorService {
    private static final Logger log = LoggerFactory.getLogger(EventExtractorService.class);
    private final EventRepo repo;
    private final ConfigServiceClient client;
    private List<EventExtractor> eventExtractors;
    private final ExecutorService executor;

    public EventExtractorService(EventRepo repo, ConfigServiceClient client) {
        this.repo = repo;
        this.client = client;
        this.executor = Executors.newScheduledThreadPool(3);
    }

    private void createEventExtractors(List<EventExtractionConfig> configs) {
        for (EventExtractionConfig config : configs) {
            Map<String, List<EventExtractionTag>> tagsByFile = groupExtractionConfigsByFile(config);

            for (String filePath : tagsByFile.keySet()) {
                List<EventExtractionTag> eventExtractionTags = tagsByFile.get(filePath);
                EventExtractor extractor = new EventExtractor(repo, eventExtractionTags,
                        filePath);
                eventExtractors.add(extractor);
            }
        }
        log.info("Created {} EventExtractors", eventExtractors);
        try {
            executor.invokeAll(eventExtractors);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Can probably be moved to configservice-sdk
     */
    public Map<String, List<EventExtractionTag>> groupExtractionConfigsByFile(
            EventExtractionConfig config) {
        Map<String, List<EventExtractionTag>> collect = config.tags.stream()
                .collect(groupingBy(item -> item.filePath));
        log.info(collect.toString());
        return collect;
    }
}
