package no.cantara.jau.eventextraction;

import no.cantara.jau.eventextraction.dto.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

public class EventRepo {
    private static final Logger log = LoggerFactory.getLogger(EventRepo.class);
    private List<Event> events;

    public EventRepo() {
        this.events = new LinkedList<>();
    }

    public void addEvents(List<Event> eventsToAdd) {
        events.addAll(eventsToAdd);
        log.trace("New events added. Number of events in repo now: {}", events.size());
    }

    public List<Event> getEvents() {
        return events;
    }

    public void clearEvents() {
        events.clear();
    }
}
