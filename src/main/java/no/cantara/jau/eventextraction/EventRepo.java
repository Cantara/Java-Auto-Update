package no.cantara.jau.eventextraction;

import java.util.List;

public class EventRepo {
    List<NumberedLine> events;

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
