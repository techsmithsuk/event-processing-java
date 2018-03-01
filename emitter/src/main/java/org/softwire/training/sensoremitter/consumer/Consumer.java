package org.softwire.training.sensoremitter.consumer;

public interface Consumer extends AutoCloseable {
    void send(String message);
}
