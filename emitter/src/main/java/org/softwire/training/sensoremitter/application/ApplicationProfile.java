package org.softwire.training.sensoremitter.application;

import com.google.common.base.MoreObjects;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public enum ApplicationProfile {
    PART1("part1.conf"), PART2("part2.conf");

    private final String filename;

    ApplicationProfile(String filename) {
        this.filename = filename;
    }

    public Config getRawConfig() {
        return ConfigFactory
                .parseResources(filename)
                .withFallback(ConfigFactory.parseResources("defaults.conf"));

    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("filename", filename)
                .toString();
    }
}