/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import java.util.List;

import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.config.event.ConfigAddEvent;
import org.geoserver.config.event.ConfigListener;
import org.geoserver.config.event.ConfigModifyEvent;
import org.geoserver.config.event.ConfigPostModifyEvent;
import org.geoserver.config.event.ConfigRemoveEvent;

/**
 * Updates the updateSequence on Catalog events.
 */
class UpdateSequenceListener implements CatalogListener, ConfigListener {
    
    GeoServer geoServer;
    boolean updating = false;
    
    public UpdateSequenceListener(GeoServer geoServer) {
        this.geoServer = geoServer;
        
        geoServer.getCatalog().addListener(this);
        geoServer.addListener(this);
    }
    
    synchronized void incrementSequence() {
        // prevent infinite loop on configuration update
        if(updating)
            return;
        
        try { 
            updating = true;
            GeoServerInfo gsInfo = geoServer.getGlobal();
            gsInfo.setUpdateSequence(gsInfo.getUpdateSequence() + 1);
            geoServer.save(gsInfo);
        } finally {
            updating = false;
        }
    }

    @Override
    public void handleAddEvent(CatalogAddEvent event) throws CatalogException {
        incrementSequence();
    }

    @Override
    public void handleRemoveEvent(CatalogRemoveEvent event) throws CatalogException {
        incrementSequence();
    }

    @Override
    public void handleModifyEvent(CatalogModifyEvent event) throws CatalogException {
        // never mind: we need the Post event
    }

    @Override
    public void handlePostModifyEvent(CatalogPostModifyEvent event) throws CatalogException {
        incrementSequence();
    }

    @Override
    public void reloaded() {
        // never mind
    }

    @Override
    public void handleGlobalModify(ConfigModifyEvent<GeoServerInfo> event) {
        // we use the post event
        
    }

    @Override
    public void handleSettingsAdd(ConfigAddEvent<SettingsInfo> event) {
        incrementSequence();
    }

    @Override
    public void handleSettingsModify(ConfigModifyEvent<SettingsInfo> event) {
        // we use post event
    }

    @Override
    public void handleSettingsPostModify(ConfigPostModifyEvent<SettingsInfo> event) {
        incrementSequence();
    }

    @Override
    public void handleSettingsRemove(ConfigRemoveEvent<SettingsInfo> event) {
        incrementSequence();
    }

    @Override
    public void handleLoggingModify(ConfigModifyEvent<LoggingInfo> event) {
        // we don't update the sequence for a logging change, the client cannot notice it   
    }

    @Override
    public void handleGlobalPostModify(ConfigPostModifyEvent<GeoServerInfo> event) {
        incrementSequence();
    }

    @Override
    public void handleLoggingPostModify(ConfigPostModifyEvent<LoggingInfo> event) {
        // we don't update the sequence for a logging change, the client cannot notice it
    }

    @Override
    public void handleServicePostModify(ConfigPostModifyEvent<ServiceInfo> event) {
        incrementSequence();
    }

    @Override
    public void handleServiceModify(ConfigModifyEvent<ServiceInfo> event) {
        // we use the post version        
    }

    @Override
    public void handleServiceRemove(ConfigRemoveEvent<ServiceInfo> event) {
        incrementSequence();
    }

    @Override
    public void handleServiceAdd(ConfigAddEvent<ServiceInfo> event) {
        incrementSequence();
    }

}
