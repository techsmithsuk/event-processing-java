package org.softwire.training.analyzer.builders;

import org.softwire.training.analyzer.model.Location;

import java.util.UUID;

public class LocationBuilder {
    private static final UUID DEFAULT_UUID = UUID.randomUUID();

    private float x = 5.1f;
    private float y = 5.2f;
    private UUID id = DEFAULT_UUID;

    public LocationBuilder setX(float x) {
        this.x = x;
        return this;
    }

    public LocationBuilder setY(float y) {
        this.y = y;
        return this;
    }

    public LocationBuilder setId(UUID id) {
        this.id = id;
        return this;
    }

    public Location createLocation() {
        return new Location(x, y, id);
    }
}