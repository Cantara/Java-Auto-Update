package no.cantara.jau.eventextraction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class EventRepo {
    private static final Logger log = LoggerFactory.getLogger(EventRepo.class);
    ConcurrentMap<String, List<String>> events;

    public EventRepo() {
        this.events = new ConcurrentHashMap<>();
        events.put("ERROR", new LinkedList<>());
        events.put("Exception", new LinkedList<>());
    }

    public void addEvents(List<NumberedLine> eventsToAdd) {
        eventsToAdd.forEach(unformattedEvent -> {
            List<String> eventsByType = events.get(unformattedEvent.getType());
            if (eventsByType == null) {
                eventsByType = new LinkedList<>();
                events.put(unformattedEvent.getType(), eventsByType);
            }
            eventsByType.add(unformattedEvent.getLine());
        });
        log.trace("New events added. Number of events in repo now: {}", events.size());
    }

    public Map<String, List<String>> getEvents() {
        return events;
    }

    public void clearEvents() {
        events.clear();
    }
}
