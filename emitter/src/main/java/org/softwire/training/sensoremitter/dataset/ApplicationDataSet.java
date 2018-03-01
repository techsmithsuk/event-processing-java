package org.softwire.training.sensoremitter.dataset;

import com.google.common.base.MoreObjects;
import org.softwire.training.sensoremitter.datasource.ChronologicalTrend;
import org.softwire.training.sensoremitter.datasource.PositionalTrend;
import org.softwire.training.sensoremitter.emitter.Emitter;
import org.softwire.training.sensoremitter.model.Location;

import java.util.Set;

/**
 * All the application state which contains randomly generated elements - trends in the data, locations, and the
 * emitter which is built from these.
 */
public class ApplicationDataSet {
    public final Emitter emitter;
    public final Set<Location> validLocations;
    public final ChronologicalTrend chronologicalTrend;
    public final Location peak;

    ApplicationDataSet(ChronologicalTrend chronologicalTrend,
                       Location peak,
                       Emitter emitter,
                       Set<Location> validLocations) {
        this.chronologicalTrend = chronologicalTrend;
        this.peak = peak;
        this.emitter = emitter;
        this.validLocations = validLocations;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("chronologicalTrend", chronologicalTrend)
                .add("peak", peak)
                .add("emitter", emitter)
                .add("validLocations", validLocations)
                .toString();
    }
}
