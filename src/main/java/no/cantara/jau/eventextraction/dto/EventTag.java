package no.cantara.jau.eventextraction.dto;

import no.cantara.jau.eventextraction.EventLine;

import java.util.LinkedList;
import java.util.List;

public class EventTag {
    private List<String> logEvent;

    public EventTag() {
        this.logEvent = new LinkedList<>();
    }

    public List<String> getEvents() {
        return logEvent;
    }

    public void addEvents(List<EventLine> events) {
        events.stream().forEach(event -> logEvent.add(event.getLine()));
    }

}
