package org.softwire.training.sensoremitter;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.softwire.training.sensoremitter.application.Application;
import org.softwire.training.sensoremitter.application.ApplicationProfile;
import org.softwire.training.sensoremitter.application.ApplicationConfig;
import org.softwire.training.sensoremitter.module.NoAwsModule;
import org.softwire.training.sensoremitter.module.Module;

import java.util.Optional;

class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        Optional<ApplicationProfile> profileO = parseArgs(args);
        if (!profileO.isPresent()) {
            String usage = "Usage: \n\tjava -jar emitter.jar <profile name>";
            LOG.error(usage);
            System.err.println(usage);
            System.exit(2);
        }

        ApplicationProfile profile = profileO.get();
        LOG.info("Running with profile: {}", profile);

        try {
            ApplicationConfig config = new ApplicationConfig(profile.getRawConfig());
            AbstractModule module;

            if (config.skipAws) {
                LOG.warn("Running without AWS integration");
                module = new NoAwsModule(config);
            } else {
                module = new Module(config);
            }

            Guice.createInjector(module).getInstance(Application.class).run();
        } catch (Throwable t) {
            LOG.error("Fatal error, terminating", t);
            System.exit(1);
        }
    }

    private static Optional<ApplicationProfile> parseArgs(String[] args) {
        if (args.length != 1) {
            return Optional.empty();
        }

        try {
            return Optional.of(ApplicationProfile.valueOf(args[0].toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
