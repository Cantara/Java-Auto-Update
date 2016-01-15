package no.cantara.jau.eventextraction;

import no.cantara.jau.eventextraction.dto.EventGroup;
import no.cantara.jau.eventextraction.dto.ExtractedEvents;
import no.cantara.jau.serviceconfig.client.ConfigServiceClient;
import no.cantara.jau.serviceconfig.dto.EventExtractionConfig;
import no.cantara.jau.serviceconfig.dto.EventExtractionTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.util.stream.Collectors.groupingBy;

public class EventExtractorService {
    private static final Logger log = LoggerFactory.getLogger(EventExtractorService.class);
    private final EventRepo repo;
    private List<EventExtractor> eventExtractors;
    private final ExecutorService executor;

    public EventExtractorService(EventRepo repo) {
        this.repo = repo;
        this.executor = Executors.newCachedThreadPool();
    }

    public void updateConfigs(List<EventExtractionConfig> configs) {
        createEventExtractors(configs);
        runEventExtractors();
    }

    public List<Future<String>> runEventExtractors() {
        try {
            List<Future<String>> futures = executor.invokeAll(eventExtractors);
            return futures;
        } catch (InterruptedException e) {
            log.error("Execution of EventExtractor was interrupted!", e);
        }
        return null;
    }

    private void createEventExtractors(List<EventExtractionConfig> configs) {
        eventExtractors = new ArrayList<>();
        for (EventExtractionConfig config : configs) {
            Map<String, List<EventExtractionTag>> tagsByFile = groupExtractionConfigsByFile(config);

            for (String filePath : tagsByFile.keySet()) {
                List<EventExtractionTag> eventExtractionTags = tagsByFile.get(filePath);
                EventExtractor extractor = new EventExtractor(repo, eventExtractionTags,
                        filePath, "jau");
                eventExtractors.add(extractor);
            }
        }
        log.info("Created {} EventExtractors", eventExtractors);
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

    /**
     * Can probably be moved to configservice-sdk
     */
    public static ExtractedEvents mapToExtractedEvents(List<EventLine> events) {
        ExtractedEvents mappedEvents = new ExtractedEvents();
        mappedEvents.addEvents(events);
        return mappedEvents;
    }
}