package no.cantara.jau;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 2015-07-13.
 */
public class ApplicationProcess implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(ApplicationProcess.class);
    private File workingDirectory;
    private String[] command;

    public ApplicationProcess(String workingDirectory, String[] command) {
        this.workingDirectory = new File(workingDirectory);
        this.command = command;
    }

    /*
    public ApplicationProcess(String workingDirectory, String javaOptions, String jarFile) {
        this.workingDirectory = new File(workingDirectory);
        this.javaOptions = javaOptions;
        this.jarFile = jarFile;
    }
    */

    public void run() {
        //ProcessBuilder pb = new ProcessBuilder("java", javaOptions, "-jar", jarFile).inheritIO().directory(workingDirectory);
        ProcessBuilder pb = new ProcessBuilder(command).inheritIO().directory(workingDirectory);
        try {
            Process process = pb.start();
            /*
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String s = "";
            while((s = in.readLine()) != null){
                log.info(s);
            }
            */
            try {
                int exitCode = process.waitFor();
                log.debug("Process exited with exitCode={}", exitCode);
            } catch (InterruptedException e) {
                throw new RuntimeException("", e);
            }
        } catch (IOException e) {
            throw new RuntimeException("", e);
        }
    }
}
