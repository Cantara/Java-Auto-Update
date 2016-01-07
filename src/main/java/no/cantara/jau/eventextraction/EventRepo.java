package no.cantara.jau.eventextraction;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class EventRepo {
    List<NumberedLine> events;

    public EventRepo() {
        this.events = new LinkedList<>();
    }

    public void addEvents(List<NumberedLine> eventsToAdd) {
        events.addAll(eventsToAdd);
    }

    public List<NumberedLine> getEvents() {
        return events;
    }

    public void clearEvents() {
        events.clear();
    }

}
