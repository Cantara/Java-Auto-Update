package no.cantara.jau.eventextraction;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import no.cantara.cs.dto.event.Event;

/**
 * This class is thread-safe.
 */
public class EventRepo {
    private List<Event> events;

    public EventRepo() {
        this.events = new LinkedList<>();
    }

    public synchronized void addEvents(List<Event> eventsToAdd) {
        events.addAll(eventsToAdd);
    }

    public synchronized List<Event> getEvents() {
        return new ArrayList<>(events);
    }

    public synchronized void clearEvents() {
        events.clear();
    }
}
