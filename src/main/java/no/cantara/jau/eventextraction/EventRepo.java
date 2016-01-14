package no.cantara.jau.eventextraction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

public class EventRepo {
    private static final Logger log = LoggerFactory.getLogger(EventRepo.class);
    private List<EventLine> events;

    public EventRepo() {
        this.events = new LinkedList<>();
    }

    public void addEvents(List<EventLine> eventsToAdd) {
        events.addAll(eventsToAdd);
        log.trace("New events added. Number of events in repo now: {}", events.size());
    }

    public List<EventLine> getEvents() {
        return events;
    }

    public void clearEvents() {
        events.clear();
    }
}
