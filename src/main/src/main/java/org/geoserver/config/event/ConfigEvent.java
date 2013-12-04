package org.geoserver.config.event;

import org.geoserver.config.ConfigInfo;
import org.geoserver.event.GeoServerEvent;

/**
 * Event for updates related to a ConfigInfo object.
 * 
 * @author Kevin Smith, Boundless
 *
 * @param <T> Class of the ConfigInfo object modified
 */
public interface ConfigEvent<T extends ConfigInfo> extends GeoServerEvent {
    /**
     * The source of the event.
     */
    T getSource();
}
