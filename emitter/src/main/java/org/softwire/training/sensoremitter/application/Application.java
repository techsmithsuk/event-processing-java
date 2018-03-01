package org.softwire.training.sensoremitter.application;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.softwire.training.sensoremitter.dataset.ApplicationDataSetGenerator;
import org.softwire.training.sensoremitter.exporter.Exporter;
import org.softwire.training.sensoremitter.dataset.ApplicationDataSet;
import org.softwire.training.sensoremitter.model.Location;
import org.softwire.training.sensoremitter.pipeline.StatsCompiler;

public class Application {
    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    private final Exporter exporter;
    private final EventLoop eventLoop;
    private final ApplicationDataSetGenerator applicationDataSetGenerator;
    private final StatsCompiler statsCompiler;
    private final ApplicationConfig config;

    @Inject
    Application(Exporter exporter,
                EventLoop eventLoop,
                ApplicationDataSetGenerator applicationDataSetGenerator,
                StatsCompiler statsCompiler,
                ApplicationConfig config) {
        this.exporter = exporter;
        this.eventLoop = eventLoop;
        this.applicationDataSetGenerator = applicationDataSetGenerator;
        this.statsCompiler = statsCompiler;
        this.config = config;
    }

    public void run() throws Exception {
        ApplicationDataSet applicationDataSet = applicationDataSetGenerator.create();
        LOG.info("Application DataSet: {}", applicationDataSet);
        logKeyInformation(applicationDataSet, statsCompiler);

        exporter.writeOut(applicationDataSet.validLocations);
        eventLoop.run(applicationDataSet.emitter);
        statsCompiler.logStats();
    }

    private void logKeyInformation(ApplicationDataSet applicationDataSet, StatsCompiler statsCompiler) {
        double hourlyRateOfChange =
                ((double) (applicationDataSet.chronologicalTrend.to - applicationDataSet.chronologicalTrend.from)) /
                        config.duration.toHours();
        Location peak = applicationDataSet.peak;

        LOG.info("------------------------------------------------------------------------");
        LOG.info("--------------------------- Key Information ----------------------------");
        LOG.info("Hourly Rate of Change: {}", hourlyRateOfChange);
        LOG.info("Location with peak value: ({}, {})", peak.x, peak.y);
        LOG.info("Expected events per second: {}", statsCompiler.getExpectedEventsPerSecond());
        LOG.info("------------------------------------------------------------------------");
        LOG.info("------------------------------------------------------------------------");
    }
}
