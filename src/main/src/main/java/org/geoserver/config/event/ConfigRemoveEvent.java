package org.geoserver.config.event;

import org.geoserver.config.ConfigInfo;

/**
 * Event for the removal of a ConfigInfo object.
 * 
 * @author Kevin Smith, Boundless
 *
 * @param <T> Class of the ConfigInfo object removed
 */
public interface ConfigRemoveEvent<T extends ConfigInfo> extends ConfigEvent<T> {

}
