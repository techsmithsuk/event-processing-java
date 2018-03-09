package org.softwire.training.analyzer.services;

public class ToStringMarshaller <T> implements Marshaller<T> {
    @Override
    public String marshall(T element) {
        return element.toString();
    }
}
