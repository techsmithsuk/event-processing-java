package org.softwire.training.analyzer.services;

import com.google.common.base.Joiner;
import org.softwire.training.analyzer.model.LocationAverage;

import java.util.List;
import java.util.stream.Collectors;

public class LocationAverageMarshaller implements Marshaller<List<LocationAverage>> {

    @Override
    public String marshall(List<LocationAverage> locationAverages) {
        // This could be done with something fancier like the Apache Commons CSV writer, but we wouldn't gain much
        // over the simple solution
        return Joiner.on("\n").join(
                locationAverages
                        .stream()
                        .map(average -> Joiner.on(",").join(
                                Float.toString(average.location.x),
                                Float.toString(average.location.y),
                                Double.toString(average.value)))
                        .collect(Collectors.toList())
        ) + "\n";
    }
}
