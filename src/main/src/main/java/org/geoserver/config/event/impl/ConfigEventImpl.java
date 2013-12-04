package org.geoserver.config.event.impl;

import java.util.HashMap;
import java.util.Map;

import org.geoserver.config.ConfigInfo;
import org.geoserver.config.event.ConfigEvent;

public abstract class ConfigEventImpl<T extends ConfigInfo> implements ConfigEvent<T> {
    T source;
    
    Map<String, Object> hints = new HashMap<String, Object>();

    public T getSource() {
        return source;
    }

    public void setSource(T source) {
        this.source = source;
    }

    public Map<String, Object> getHints() {
        return hints;
    }
}
