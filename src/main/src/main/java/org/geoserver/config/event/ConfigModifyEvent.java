package org.geoserver.config.event;

import java.util.List;

import org.geoserver.config.ConfigInfo;

/**
 * Event for modification of one or more properties of a ConfigInfo object
 * 
 * @author Kevin Smith, Boundless
 *
 * @param <T> Class of the ConfigInfo object modified
 */
public interface ConfigModifyEvent<T extends ConfigInfo> extends ConfigEvent<T> {
    /**
     * The names of the properties that were modified.
     */
    List<String> getPropertyNames();
    
    /**
     * The old values of the properties that were modified.
     */
    List<Object> getOldValues();
    
    /**
     * The new values of the properties that were modified.
     */
    List<Object> getNewValues();
}
