package no.cantara.jau.eventextraction;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class EventRepo {
    Map<String, List<String>> events;

    public EventRepo(List<String> mdcEvents) {
        this.events = new HashMap<>();
        events.put("ERROR", new LinkedList<>());
        events.put("Exception", new LinkedList<>());
        mdcEvents.forEach(e -> events.put(e, new LinkedList<>()));
    }

    public void addEvents(List<NumberedLine> eventsToAdd) {
        eventsToAdd.forEach(unformattedEvent -> events.get(unformattedEvent.getType())
                .add(unformattedEvent.getLine()));
    }

    public Map<String, List<String>> getEvents() {
        return events;
    }

    public void clearEvents() {
        events.clear();
    }
}
