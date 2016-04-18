package no.cantara.jau.eventextraction;

import no.cantara.cs.dto.event.Event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * This class is thread-safe.
 */
public class EventRepo {
    private static final Logger log = LoggerFactory.getLogger(EventRepo.class);
    private List<Event> events;

    public EventRepo() {
        this.events = new LinkedList<>();
    }

    public synchronized void addEvents(List<Event> eventsToAdd) {
        events.addAll(eventsToAdd);
        log.trace("New events added. Number of events in repo now: {}", events.size());
    }

    public synchronized List<Event> getEvents() {
        return new ArrayList<>(events);
    }

    public synchronized void clearEvents() {
        events.clear();
    }
}
