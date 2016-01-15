package no.cantara.jau.eventextraction.dto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EventGroup {
    private final Map<String, EventFile> files;

    public EventGroup() {
        this.files = new HashMap<>();
    }

    public Map<String, EventFile> getFiles() {
        return files;
    }

    public EventFile getEventFile(String fileName) {
        return files.get(fileName);
    }

    public void addEvents(List<Event> events) {
        Map<String, List<Event>> eventGroupsGroupedByFileName = events.stream()
                .collect(Collectors.groupingBy(
                        Event::getFileName
                ));

        eventGroupsGroupedByFileName.keySet().stream()
                .forEach(fileName -> {
                    List<Event> eventFilesToAdd = eventGroupsGroupedByFileName.get(fileName);
                    files.putIfAbsent(fileName, new EventFile());
                    EventFile eventFile = files.get(fileName);
                    eventFile.addEvents(eventFilesToAdd);
                });


    }
}
