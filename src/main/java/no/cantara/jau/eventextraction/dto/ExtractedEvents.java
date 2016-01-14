package no.cantara.jau.eventextraction.dto;

import no.cantara.jau.eventextraction.EventLine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ExtractedEvents {
    private final Map<String, EventGroup> eventGroups;

    public ExtractedEvents() {
        eventGroups = new HashMap<>();
    }

    public void addEvents(List<EventLine> eventsToAdd) {
        Map<String, List<EventLine>> eventsGroupedByGroup = eventsToAdd.stream()
                .collect(Collectors.groupingBy(
                        EventLine::getGroupName
                ));

        eventsGroupedByGroup.keySet().stream()
                .forEach(groupName -> {
                    List<EventLine> eventFilesToAdd = eventsGroupedByGroup.get(groupName);
                    eventGroups.putIfAbsent(groupName, new EventGroup());
                    EventGroup eventGroup = eventGroups.get(groupName);
                    eventGroup.addEvents(eventFilesToAdd);
                });
    }

    public EventGroup getEventGroup(String groupName) {
        return eventGroups.get(groupName);
    }

}
