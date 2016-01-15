package no.cantara.jau.eventextraction.dto;

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

    public void addEvents(List<Event> events) {
        Map<String, List<Event>> eventsGroupedByTagName = events.stream()
                .collect(Collectors.groupingBy(
                        Event::getTag
                ));

        eventsGroupedByTagName.keySet().stream()
                .forEach(tag -> {
                    List<Event> eventsToAdd = eventsGroupedByTagName.get(tag);
                    tagNameToTagMapping.putIfAbsent(tag, new EventTag());
                    tagNameToTagMapping.get(tag).addEvents(eventsToAdd);
                });
    }
}
