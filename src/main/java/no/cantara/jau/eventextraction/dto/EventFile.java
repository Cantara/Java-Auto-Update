package no.cantara.jau.eventextraction.dto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EventFile {
    private final Map<String, EventTag> tags;

    public EventFile() {
        this.tags = new HashMap<>();
    }

    public Map<String, EventTag> getTags() {
        return tags;
    }

    public EventTag getEventTag(String tagName) {
        return tags.get(tagName);
    }

    public void addEvents(List<Event> events) {
        Map<String, List<Event>> eventsGroupedByTagName = events.stream()
                .collect(Collectors.groupingBy(
                        Event::getTag
                ));

        eventsGroupedByTagName.keySet().stream()
                .forEach(tag -> {
                    List<Event> eventsToAdd = eventsGroupedByTagName.get(tag);
                    tags.putIfAbsent(tag, new EventTag());
                    tags.get(tag).addEvents(eventsToAdd);
                });
    }
}
