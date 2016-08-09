package no.cantara.jau.eventextraction;

import no.cantara.cs.dto.event.EventExtractionTag;
import no.cantara.jau.util.PropertiesHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;

public class EventExtractor implements Callable<String> {
    private static final Logger LOG = LoggerFactory.getLogger(EventExtractor.class);
    private static final String KEY_LAST_MODIFIED = "lastModified";
    private static final String KEY_LAST_LINE_READ = "lastLineRead";
    private static final String KEY_LAST_FILE_SIZE = "lastFileSize";

    private final List<EventExtractionTag> extractionTags;
    private final String managedProcessLogFilePath;
    private final File managedProcessLogFile;
    private final EventRepo eventRepo;
    private final String groupName;
    private final String startPattern;
    private long lastLineRead;
    private long lastModified;
    private long lastFileSize;
    private Properties properties;

    public EventExtractor(EventRepo eventRepo, List<EventExtractionTag> extractionTags,
                          String managedProcessLogFilePath, String groupName, String startPattern) {
        this.eventRepo = eventRepo;
        this.groupName = groupName;
        this.extractionTags = extractionTags;
        this.startPattern = startPattern;
        this.managedProcessLogFilePath = managedProcessLogFilePath;
        managedProcessLogFile = new File(managedProcessLogFilePath);

        loadState();
    }

    private void loadState() {
        properties = PropertiesHelper.loadProperties(getPropertyFile());
        lastModified = PropertiesHelper.getLongProperty(properties, KEY_LAST_MODIFIED, 0L);
        lastLineRead = PropertiesHelper.getLongProperty(properties, KEY_LAST_LINE_READ, 0L);
        lastFileSize = PropertiesHelper.getLongProperty(properties, KEY_LAST_FILE_SIZE, 0L);
    }

    private void saveState() {
        PropertiesHelper.setLongProperty(properties, KEY_LAST_MODIFIED, lastModified);
        PropertiesHelper.setLongProperty(properties, KEY_LAST_LINE_READ, lastLineRead);
        PropertiesHelper.setLongProperty(properties, KEY_LAST_FILE_SIZE, lastFileSize);
        PropertiesHelper.saveProperties(properties, getPropertyFile());
    }

    private File getPropertyFile() {
        return new File(managedProcessLogFile.getName() + ".properties");
    }

    private void checkForEvents() {
        if (fileHasBeenModified()) {
            try {
                lastLineRead = new CommandExtractEventsFromFile(eventRepo, lastLineRead,
                        managedProcessLogFilePath, groupName, startPattern, extractionTags).run();
                saveState();
            } catch (Exception e) {
                LOG.error("Failed to extract events from file {}", managedProcessLogFilePath, e);
            }
        }
    }

    private boolean fileHasBeenModified() {
        if (managedProcessLogFile.lastModified() > lastModified || managedProcessLogFile.length() != lastFileSize) {
            LOG.trace("{} is modified since last extraction. Extracting...", managedProcessLogFilePath);
            lastModified = managedProcessLogFile.lastModified();

            // If file has shrunk (due to rolling log files), start reading from the first line.
            if (managedProcessLogFile.length() < lastFileSize) {
                LOG.info("{} has shrunk since last extraction. Starting from first line.", managedProcessLogFilePath);
                lastLineRead = 0;
            }
            lastFileSize = managedProcessLogFile.length();
            return true;
        }
        LOG.trace("{} has NOT been modified since last extraction. Will not extract. Last modified when last extracted was {}. Last modified now is {}",
                managedProcessLogFilePath, new Date(lastModified), new Date(managedProcessLogFile.lastModified()));
        return false;
    }

    @Override
    public String call() throws Exception {
        checkForEvents();
        return null;
    }
}
