package org.softwire.training.sensoremitter.exporter;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.softwire.training.sensoremitter.model.Location;

import java.util.Collection;

public class NoopExporter implements Exporter {
    private static final Logger LOG = LoggerFactory.getLogger(NoopExporter.class);

    @Inject
    public NoopExporter() {
    }

    @Override
    public void writeOut(Collection<Location> locations) {
        LOG.debug("Locations: {}", locations);
    }
}
