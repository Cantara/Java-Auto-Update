package no.cantara.jau.eventextraction.dto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EventGroup {
    private final Map<String, EventFile> fileNameToFileMapping;

    public EventGroup() {
        this.fileNameToFileMapping = new HashMap<>();
    }

    public EventFile getEventFile(String fileName) {
        return fileNameToFileMapping.get(fileName);
    }

    public void addEvents(List<Event> events) {
        Map<String, List<Event>> eventGroupsGroupedByFileName = events.stream()
                .collect(Collectors.groupingBy(
                        Event::getFileName
                ));

        eventGroupsGroupedByFileName.keySet().stream()
                .forEach(fileName -> {
                    List<Event> eventFilesToAdd = eventGroupsGroupedByFileName.get(fileName);
                    fileNameToFileMapping.putIfAbsent(fileName, new EventFile());
                    EventFile eventFile = fileNameToFileMapping.get(fileName);
                    eventFile.addEvents(eventFilesToAdd);
                });


    }
}
