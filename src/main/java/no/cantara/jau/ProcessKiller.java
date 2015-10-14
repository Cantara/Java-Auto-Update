package no.cantara.jau;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Makes sure any running managed service is killed if JAU should restart
 */
public class ProcessKiller {
    private static final Logger log = LoggerFactory.getLogger(ProcessKiller.class);
    private static final String RUNNING_PROCESS_FILENAME = "runningProcess.txt";

    public void killExistingProcessIfRunning() {
        Path file = Paths.get(RUNNING_PROCESS_FILENAME);
        if (Files.exists(file)) {
            String pid = null;
            try {
                pid = new String(Files.readAllBytes(Paths.get(RUNNING_PROCESS_FILENAME)));
            } catch (IOException e) {
                log.warn("Could not read file={}. Possible multiple processes!", RUNNING_PROCESS_FILENAME);
            }
            if (isValidPid(pid)) {
                killRunningProcess(pid);
            }
        } else {
            log.info("{} not found. Assuming no existing process is running.", RUNNING_PROCESS_FILENAME);
        }
    }

    public boolean isValidPid(String pid) {
        // TODO: How to check if valid pid?
        try {
            Integer.parseInt(pid);
            return true;
        } catch (NumberFormatException e) {
            log.warn("PID is not valid integer. Got: '{}'", pid, e);
            return false;
        }
    }

    public void killRunningProcess(String pid) {
        log.info("Killing existing managed process with pid={}", pid);
        Runtime rt = Runtime.getRuntime();
        ProcessBuilder processBuilder;

        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            processBuilder = new ProcessBuilder("taskkill", pid);
            killProcess(pid, processBuilder);
        } else { //unix
            processBuilder = new ProcessBuilder("kill", "-9", pid);
            killProcess(pid, processBuilder);
        }
    }

    public void killProcess(String pid, ProcessBuilder processBuilder) {
        try {
            Process p = processBuilder.start();
            try {
                p.waitFor();
                if (p.exitValue() == 0) {
                    log.info("Successfully killed existing managed process pid={}", pid);
                } else {
                    printErrorCommandFromProcess(p);
                    log.warn("Could not kill existing managed process pid={}, process exitValue={}", pid, p.exitValue());
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            log.warn("Could not kill existing managed process pid={}. Possible duplicate processes!", pid, e);
        }
    }

    public void printErrorCommandFromProcess(Process p) throws IOException {
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(p.getErrorStream()));
        StringBuilder builder = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        String result = builder.toString();
        log.error("Error output from kill command: '{}'", result);
    }
}