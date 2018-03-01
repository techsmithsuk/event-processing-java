package org.softwire.training.analyzer.application;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.softwire.training.analyzer.pipeline.Aggregator;
import org.softwire.training.analyzer.pipeline.Deduplicator;
import org.softwire.training.analyzer.receiver.Receiver;
import org.softwire.training.analyzer.services.FileWriter;
import org.softwire.training.analyzer.services.LocationService;

public class TypedConfig {
    public final Deduplicator.TypedConfig deduplicator;
    public final EventLoop.TypedConfig application;
    public final Receiver.TypedConfig receiver;
    public final LocationService.TypedConfig locationService;
    public final Aggregator.TypedConfig aggregator;
    public final FileWriter.TypedConfig fileWriter;

    public TypedConfig() {
        Config config = ConfigFactory.load();
        deduplicator = Deduplicator.TypedConfig.fromUntypedConfig(config.getConfig("deduplicator"));
        application = EventLoop.TypedConfig.fromUntypedConfig(config.getConfig("application"));
        receiver = Receiver.TypedConfig.fromUntypedConfig(config.getConfig("receiver"));
        locationService = LocationService.TypedConfig.fromUntypedConfig(config.getConfig("locations"));
        aggregator = Aggregator.TypedConfig.fromUntypedConfig(config.getConfig("aggregator"));
        fileWriter = FileWriter.TypedConfig.fromUntypedConfig(config.getConfig("fileWriter"));
    }
}
