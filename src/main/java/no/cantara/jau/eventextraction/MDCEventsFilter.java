package no.cantara.jau.eventextraction;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.AbstractMatcherFilter;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class MDCEventsFilter extends AbstractMatcherFilter<ILoggingEvent> {
    private static final Logger log = LoggerFactory.getLogger(MDCEventsFilter.class);
    private String mdcEventsToMatch = null;
    @Override
    public void start() {
        if (null != this.mdcEventsToMatch)
            super.start();
        else
            addError("No MDC events to match to yet.");
    }
    @Override
    public FilterReply decide(ILoggingEvent event) {
        if (event.getLevel() == Level.ERROR) {
            return onMatch;
        }

        log.info("Events to match: " + mdcEventsToMatch);
        Set<String> mdcKeys = event.getMDCPropertyMap().keySet();

        for (String mdc : mdcKeys) {
            if (mdcEventsToMatch.contains(mdc)) {
                log.info("A match!!");
                return onMatch;
            }
        }
        return onMismatch;
    }
    public void setMdcEvents(String markerStr) {
        if(null != markerStr)
            mdcEventsToMatch = markerStr;
    }

}
