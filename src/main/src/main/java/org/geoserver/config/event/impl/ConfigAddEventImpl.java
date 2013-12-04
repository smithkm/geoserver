package org.geoserver.config.event.impl;

import org.geoserver.config.ConfigInfo;
import org.geoserver.config.event.ConfigAddEvent;

public class ConfigAddEventImpl<T extends ConfigInfo>  
    extends ConfigEventImpl<T> implements ConfigAddEvent<T>{
    
}
