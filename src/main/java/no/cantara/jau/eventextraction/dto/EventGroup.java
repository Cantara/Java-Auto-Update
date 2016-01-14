package no.cantara.jau.eventextraction.dto;

import no.cantara.jau.eventextraction.EventLine;

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

    public void addEvents(List<EventLine> events) {
        Map<String, List<EventLine>> eventGroupsGroupedByFileName = events.stream()
                .collect(Collectors.groupingBy(
                        EventLine::getFileName
                ));

        eventGroupsGroupedByFileName.keySet().stream()
                .forEach(fileName -> {
                    List<EventLine> eventFilesToAdd = eventGroupsGroupedByFileName.get(fileName);
                    fileNameToFileMapping.putIfAbsent(fileName, new EventFile());
                    EventFile eventFile = fileNameToFileMapping.get(fileName);
                    eventFile.addEvents(eventFilesToAdd);
                });


    }
}
