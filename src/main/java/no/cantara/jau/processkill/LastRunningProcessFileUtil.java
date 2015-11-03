package no.cantara.jau.processkill;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LastRunningProcessFileUtil {
    private static final Logger log = LoggerFactory.getLogger(LastRunningProcessFileUtil.class);
    private String fileName;

    public LastRunningProcessFileUtil(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    public void writePidToFile(String pid) {
        Path filePath = Paths.get(fileName);
        try {
            if (!Files.exists(filePath)) {
                Files.createFile(Paths.get(fileName));
            }
        } catch (IOException e) {
            log.error("Could not create file to managed process pid={}", pid, e);
            return;
        }

        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(fileName), "utf-8"))) {
            writer.write(pid);
            log.debug("Wrote pid={} to file={}", pid, fileName);
        } catch (FileNotFoundException e) {
            log.error("File '{}' to write managed process pid={} not found", fileName, pid, e);
        } catch (UnsupportedEncodingException e) {
            log.error("Encoding error while writing to {}", fileName, e);
        } catch (IOException e) {
            log.error("Could not write to file '{}'", fileName, e);
        }
    }

    public String getRunningProcessPidFromFile() throws IOException {
        Path file = Paths.get(fileName);
        String pid = null;
        if (Files.exists(file)) {
            pid = new String(Files.readAllBytes(Paths.get(fileName)));
        }
        return pid;
    }
}
