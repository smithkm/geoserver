package org.geoserver.config.event;

import org.geoserver.config.ConfigInfo;

/**
 * Event for addition of a new ConfigInfo object
 * 
 * @author Kevin Smith, Boundless
 *
 * @param <T> Class of the ConfigInfo object added
 */
public interface ConfigAddEvent<T extends ConfigInfo> extends ConfigEvent<T> {

}
