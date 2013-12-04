package org.geoserver.config.event.impl;

import org.geoserver.config.ConfigurationListener;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.event.ConfigAddEvent;
import org.geoserver.config.event.ConfigListener;
import org.geoserver.config.event.ConfigModifyEvent;
import org.geoserver.config.event.ConfigPostModifyEvent;
import org.geoserver.config.event.ConfigRemoveEvent;

/**
 * Wrap a ConfigurationListener with a ConfigListener for backward compatibility
 * 
 * @author Kevin Smith, OpenGeo
 *
 */
@SuppressWarnings("deprecation")
public class ConfigurationListenerWrapper implements ConfigListener {

    final ConfigurationListener delegate;
    
    public ConfigurationListenerWrapper(ConfigurationListener delegate) {
        super();
        this.delegate = delegate;
    }

    @Override
    public void handleGlobalModify(ConfigModifyEvent<GeoServerInfo> event) {
        delegate.handleGlobalChange(event.getSource(), event.getPropertyNames(), event.getOldValues(), event.getNewValues());
    }
    
    @Override
    public void handleGlobalPostModify(ConfigPostModifyEvent<GeoServerInfo> event) {
        delegate.handlePostGlobalChange(event.getSource());
    }
    
    @Override
    public void handleSettingsAdd(ConfigAddEvent<SettingsInfo> event) {
        delegate.handleSettingsAdded(event.getSource());
    }
    
    @Override
    public void handleSettingsModify(ConfigModifyEvent<SettingsInfo> event) {
        delegate.handleSettingsModified(event.getSource(), event.getPropertyNames(), event.getOldValues(), event.getNewValues());
    }
    
    @Override
    public void handleSettingsPostModify(ConfigPostModifyEvent<SettingsInfo> event) {
        delegate.handleSettingsPostModified(event.getSource());
    }
    
    @Override
    public void handleSettingsRemove(ConfigRemoveEvent<SettingsInfo> event) {
        delegate.handleSettingsRemoved(event.getSource());
    }
    
    @Override
    public void handleLoggingModify(ConfigModifyEvent<LoggingInfo> event) {
        delegate.handleLoggingChange(event.getSource(), event.getPropertyNames(), event.getOldValues(), event.getNewValues());
    }
    
    @Override
    public void handleLoggingPostModify(ConfigPostModifyEvent<LoggingInfo> event) {
        delegate.handlePostLoggingChange(event.getSource());
    }
    
    @Override
    public void handleServiceModify(ConfigModifyEvent<ServiceInfo> event) {
        delegate.handleServiceChange(event.getSource(), event.getPropertyNames(), event.getOldValues(), event.getNewValues());
    }
    
    @Override
    public void handleServicePostModify(ConfigPostModifyEvent<ServiceInfo> event) {
        delegate.handlePostServiceChange(event.getSource());
    }
    
    @Override
    public void handleServiceRemove(ConfigRemoveEvent<ServiceInfo> event) {
        delegate.handleServiceRemove(event.getSource());
    }

    @Override
    public void handleServiceAdd(ConfigAddEvent<ServiceInfo> event) {
        // Do nothing, this event was not handled by the old listener.
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((delegate == null) ? 0 : delegate.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ConfigurationListenerWrapper other = (ConfigurationListenerWrapper) obj;
        if (delegate == null) {
            if (other.delegate != null)
                return false;
        } else if (!delegate.equals(other.delegate))
            return false;
        return true;
    }

}
