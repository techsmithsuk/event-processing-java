package org.softwire.training.analyzer.application;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.softwire.training.analyzer.pipeline.ChronologicalAggregator;
import org.softwire.training.analyzer.pipeline.Deduplicator;
import org.softwire.training.analyzer.pipeline.LocationAggregator;
import org.softwire.training.analyzer.receiver.Receiver;
import org.softwire.training.analyzer.services.LocationService;

public class TypedConfig {
    public final Deduplicator.TypedConfig deduplicator;
    public final EventLoop.TypedConfig application;
    public final Receiver.TypedConfig receiver;
    public final LocationService.TypedConfig locationService;
    public final ChronologicalAggregator.TypedConfig chronologicalAggregator;
    public final LocationAggregator.TypedConfig locationAggregator;

    public TypedConfig() {
        Config config = ConfigFactory.load();
        deduplicator = Deduplicator.TypedConfig.fromUntypedConfig(config.getConfig("deduplicator"));
        application = EventLoop.TypedConfig.fromUntypedConfig(config.getConfig("application"));
        receiver = Receiver.TypedConfig.fromUntypedConfig(config.getConfig("receiver"));
        locationService = LocationService.TypedConfig.fromUntypedConfig(config.getConfig("locations"));
        chronologicalAggregator = ChronologicalAggregator.TypedConfig.fromUntypedConfig(config.getConfig("chronologicalAggregator"));
        locationAggregator = LocationAggregator.TypedConfig.fromUntypedConfig(config.getConfig("locationAggregator"));
    }
}
