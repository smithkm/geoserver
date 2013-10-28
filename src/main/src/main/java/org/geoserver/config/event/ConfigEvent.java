package org.geoserver.config.event;

import org.geoserver.config.ConfigInfo;
import org.geoserver.event.GeoServerEvent;

public interface ConfigEvent<T extends ConfigInfo> extends GeoServerEvent {
    /**
     * The source of the event.
     */
    T getSource();
}
