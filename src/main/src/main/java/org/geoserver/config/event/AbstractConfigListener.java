package org.geoserver.config.event;

import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;

/**
 * ConfigListener with all methods stubbed.  Subclasses can implement handlers for only those events
 * of interest.
 * 
 * @author Kevin Smith, OpenGeo
 *
 */
public abstract class AbstractConfigListener implements ConfigListener {
    
    @Override
    public void handleGlobalModify(ConfigModifyEvent<GeoServerInfo> event) {
        
    }
    
    @Override
    public void handleGlobalPostModify(ConfigPostModifyEvent<GeoServerInfo> event) {
        
    }
    
    @Override
    public void handleSettingsAdd(ConfigAddEvent<SettingsInfo> event) {
        
    }
    
    @Override
    public void handleSettingsModify(ConfigModifyEvent<SettingsInfo> event) {
        
    }
    
    @Override
    public void handleSettingsPostModify(ConfigPostModifyEvent<SettingsInfo> event) {
        
    }
    
    @Override
    public void handleSettingsRemove(ConfigRemoveEvent<SettingsInfo> event) {
        
    }
    
    @Override
    public void handleLoggingModify(ConfigModifyEvent<LoggingInfo> event) {
        
    }
    
    @Override
    public void handleLoggingPostModify(ConfigPostModifyEvent<LoggingInfo> event) {
        
    }
    
    @Override
    public void handleServiceModify(ConfigModifyEvent<ServiceInfo> event) {
        
    }
    
    @Override
    public void handleServicePostModify(ConfigPostModifyEvent<ServiceInfo> event) {
        
    }
    
    @Override
    public void handleServiceRemove(ConfigRemoveEvent<ServiceInfo> event) {
        
    }
    
    @Override
    public void handleServiceAdd(ConfigAddEvent<ServiceInfo> event) {
        
    }
    
}
