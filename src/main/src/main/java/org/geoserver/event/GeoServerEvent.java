package org.geoserver.event;

import java.util.Map;

import org.geoserver.catalog.Info;

/**
 * Event related to a catalog or configuration Info object.
 * 
 * @author Kevin Smith, Boundless
 *
 */
public interface GeoServerEvent {
    /**
     * The source of the event.
     */
    Info getSource();
    
    /**
     * Get hints about the event.  This map can be modified to add hints.
     * @return
     */
    Map<String, Object> getHints();
}
