/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.event;

import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.event.ConfigAddEvent;
import org.geoserver.config.event.ConfigModifyEvent;
import org.geoserver.config.event.ConfigPostModifyEvent;
import org.geoserver.config.event.ConfigRemoveEvent;

/**
 * Replaces {@link org.geoserver.config.ConfigurationListener}
 *
 * @author Kevin Smith, Boundless
 */
public interface ConfigListener {

    /**
     * Handles a change to the global configuration.
     * 
     * @param event Event object.
     */
    void handleGlobalModify( ConfigModifyEvent<GeoServerInfo> event );
    
    /**
     * Handles the event fired post change to global configuration. 
     * 
     * @param event Event object.
     */
    void handleGlobalPostModify( ConfigPostModifyEvent<GeoServerInfo> event);
    
    /**
     * Handles the event fired when a settings configuration is added. 
     * 
     * @param event Event object.
     */
    void handleSettingsAdd(ConfigAddEvent<SettingsInfo> event);

    /**
     * Handles the event fired when a settings configuration is changed. 
     * 
     * @param event Event object.
     */
    void handleSettingsModify(ConfigModifyEvent<SettingsInfo> event);

    /**
     * Handles the event fired post change to a settings configuration. 
     * 
     * @param event Event object.
     */
    void handleSettingsPostModify(ConfigPostModifyEvent<SettingsInfo> event);

    /**
     * Handles the event fired when a settings configuration is removed. 
     * 
     * @param event Event object.
     */
    void handleSettingsRemove(ConfigRemoveEvent<SettingsInfo> event);

    /**
     * Handles a change to the logging configuration.
     * 
     * @param event Event object.
     */
    void handleLoggingModify( ConfigModifyEvent<LoggingInfo> event );

    /**
     * Handles the event fired post change to logging configuration. 
     * 
     * @param event Event object.
     */
    void handleLoggingPostModify( ConfigPostModifyEvent<LoggingInfo> event );
    
    /**
     * Handles a change to a service configuration.
     * 
     * @param event Event object.
     */
    void handleServiceModify( ConfigModifyEvent<ServiceInfo> event);
    
    /**
     * Handles the event fired post change to service configuration.
     * 
     * @param event Event object.
     */
    void handleServicePostModify( ConfigPostModifyEvent<ServiceInfo> event );

    /**
     * Handles the event fired when a service configuration is removed.
     * 
     * @param event Event object.
     */
    void handleServiceAdd( ConfigAddEvent<ServiceInfo> event );

    /**
     * Handles the event fired when a service configuration is removed.
     * 
     * @param event Event object.
     */
    void handleServiceRemove( ConfigRemoveEvent<ServiceInfo> event );
}
