package org.geoserver.config.event.impl;

import org.geoserver.config.ConfigInfo;
import org.geoserver.config.event.ConfigPostModifyEvent;

public class ConfigPostModifyEventImpl<T extends ConfigInfo> 
    extends ConfigEventImpl<T> implements ConfigPostModifyEvent<T> {
    
}
