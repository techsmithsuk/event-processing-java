package org.softwire.training.sensoremitter.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsoleTopic implements Consumer {
    private static final Logger LOG = LoggerFactory.getLogger(ConsoleTopic.class);

    @Override
    public void send(String message) {
        LOG.info(message);
    }

    @Override
    public void close() {
    }
}
