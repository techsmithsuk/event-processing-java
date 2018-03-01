package org.softwire.training.sensoremitter.module;

import org.softwire.training.sensoremitter.application.ApplicationConfig;
import org.softwire.training.sensoremitter.consumer.ConsoleTopic;
import org.softwire.training.sensoremitter.consumer.Consumer;
import org.softwire.training.sensoremitter.exporter.Exporter;
import org.softwire.training.sensoremitter.exporter.NoopExporter;

public class NoAwsModule extends BaseModule {
    public NoAwsModule(ApplicationConfig config) {
        super(config);
    }

    @Override
    protected void configure() {
        super.configure();
        bind(Consumer.class).to(ConsoleTopic.class);
        bind(Exporter.class).to(NoopExporter.class);
    }
}
