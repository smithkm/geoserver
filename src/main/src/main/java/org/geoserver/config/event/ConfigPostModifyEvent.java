package org.geoserver.config.event;

import org.geoserver.config.ConfigInfo;

/**
 * Event for the completion of modification of a ConfigInfo object.
 * 
 * @author Kevin Smith, Boundless
 *
 * @param <T> Class of the ConfigInfo object modified
 */
public interface ConfigPostModifyEvent<T extends ConfigInfo> extends ConfigEvent<T> {
    
}
