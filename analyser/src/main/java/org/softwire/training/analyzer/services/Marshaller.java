package org.softwire.training.analyzer.services;

public interface Marshaller<T> {
    String marshall(T element);
}
