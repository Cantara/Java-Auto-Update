package no.cantara.jau.eventextraction.dto;

import no.cantara.jau.eventextraction.EventLine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EventFile {
    private final Map<String, EventTag> tagNameToTagMapping;

    public EventFile() {
        this.tagNameToTagMapping = new HashMap<>();
    }

    public EventTag getEventTag(String tagName) {
        return tagNameToTagMapping.get(tagName);
    }

    public void addEvents(List<EventLine> events) {
        Map<String, List<EventLine>> eventsGroupedByTagName = events.stream()
                .collect(Collectors.groupingBy(
                        EventLine::getTag
                ));

        eventsGroupedByTagName.keySet().stream()
                .forEach(tag -> {
                    List<EventLine> eventsToAdd = eventsGroupedByTagName.get(tag);
                    tagNameToTagMapping.putIfAbsent(tag, new EventTag());
                    tagNameToTagMapping.get(tag).addEvents(eventsToAdd);
                });
    }
}
