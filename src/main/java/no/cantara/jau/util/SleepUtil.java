package no.cantara.jau.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SleepUtil {

    private static final Logger log = LoggerFactory.getLogger(SleepUtil.class);

    public static void sleepWithLogging(long waitInterval) {
        try {
            log.debug("retrying in {} milliseconds ", waitInterval);
            Thread.sleep(waitInterval);
        } catch (InterruptedException e) {
            log.error("Failed to run Thread.sleep({})", waitInterval);
            log.error(e.getMessage());
        }
    }

}
