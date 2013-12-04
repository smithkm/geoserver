package org.geoserver.config.event.impl;

import org.geoserver.config.ConfigInfo;
import org.geoserver.config.event.ConfigRemoveEvent;

public class ConfigRemoveEventImpl<T extends ConfigInfo> 
    extends ConfigEventImpl<T> implements ConfigRemoveEvent<T> {
    
}
