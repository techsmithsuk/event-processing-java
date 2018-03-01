package org.softwire.training.sensoremitter.exporter;


import org.softwire.training.sensoremitter.model.Location;

import java.io.IOException;
import java.util.Collection;

public interface Exporter {
    void writeOut(Collection<Location> locations) throws IOException;
}
