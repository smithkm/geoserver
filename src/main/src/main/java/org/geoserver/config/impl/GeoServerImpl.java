/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.impl;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.LocalWorkspaceCatalog;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.ConfigInfo;
import org.geoserver.config.ConfigurationListener;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.GeoServerFactory;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.GeoServerLoader;
import org.geoserver.config.GeoServerLoaderProxy;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.event.ConfigAddEvent;
import org.geoserver.config.event.ConfigEvent;
import org.geoserver.config.event.ConfigListener;
import org.geoserver.config.event.ConfigModifyEvent;
import org.geoserver.config.event.ConfigPostModifyEvent;
import org.geoserver.config.event.ConfigRemoveEvent;
import org.geoserver.config.event.impl.ConfigAddEventImpl;
import org.geoserver.config.event.impl.ConfigModifyEventImpl;
import org.geoserver.config.event.impl.ConfigPostModifyEventImpl;
import org.geoserver.config.event.impl.ConfigRemoveEventImpl;
import org.geoserver.config.event.impl.ConfigurationListenerWrapper;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import static org.geoserver.ows.util.OwsUtils.resolveCollections;

public class GeoServerImpl implements GeoServer, ApplicationContextAware {
    
    private static final Logger LOGGER = Logging.getLogger(GeoServerImpl.class);

    /**
     * factory for creating objects
     */
    GeoServerFactory factory = new GeoServerFactoryImpl(this);
    
    /**
     * the catalog
     */
    Catalog catalog;
    
    /**
     * data access object
     */
    GeoServerFacade facade;
    
    /**
     * listeners
     */
    List<ConfigListener> listeners = new ArrayList<ConfigListener>();

    public GeoServerImpl() {
        this.facade = new DefaultGeoServerFacade(this);
    }
    
    public GeoServerFacade getFacade() {
        return facade;
    }
    
    public void setFacade(GeoServerFacade facade) {
        this.facade = facade;
        facade.setGeoServer(this);
    }
    
    public GeoServerFactory getFactory() {
        return factory;
    }
    
    public void setFactory(GeoServerFactory factory) {
        this.factory = factory;
    }

    public Catalog getCatalog() {
        return catalog;
    }
    
    public void setCatalog(Catalog catalog) {
        this.catalog = catalog;
        
        // This instance of check is has to be here because this Geoserver cannot be injected
        // into LocalWorkspaceCatalog because it causes a circular reference
        if (catalog instanceof LocalWorkspaceCatalog) {
            LocalWorkspaceCatalog lwCatalog = (LocalWorkspaceCatalog) catalog;
            lwCatalog.setGeoServer(this);
        }
    }
    
    public GeoServerInfo getGlobal() {
        return facade.getGlobal();
    }
    
    public void setGlobal(GeoServerInfo global) {
        facade.setGlobal(global);
        
        //fire the modification event
        fireGlobalPostModified();
    }

    public SettingsInfo getSettings() {
        SettingsInfo settings = null;
        if (LocalWorkspace.get() != null) {
            settings = getSettings(LocalWorkspace.get());
        }
        return settings != null ? settings : getGlobal().getSettings();
    }

    public SettingsInfo getSettings(WorkspaceInfo workspace) {
        return facade.getSettings(workspace);
    }

    public void add(SettingsInfo settings) {
        validate(settings);
        resolve(settings);
        
        WorkspaceInfo workspace = settings.getWorkspace();
        if (facade.getSettings(workspace) != null) {
            throw new IllegalArgumentException("Settings already exist for workspace '" + 
                workspace.getName() + "'");
        }

        facade.add(settings);
        fireSettingsAdded(settings);
    }

    public void save(SettingsInfo settings) {
        validate(settings);
        
        facade.save(settings);
        fireSettingsPostModified(settings);
    }

    public void remove(SettingsInfo settings) {
        facade.remove(settings);

        fireSettingsRemoved(settings);
    }

    void validate(SettingsInfo settings) {
        WorkspaceInfo workspace = settings.getWorkspace();
        if (workspace == null) {
            throw new IllegalArgumentException("Settings must be part of a workspace");
        }
    }

    void resolve(SettingsInfo settings) {
        resolveCollections(settings);
    }

    
    public void fireSettingsAdded(SettingsInfo settings) {
        ConfigAddEventImpl<SettingsInfo> evt = new ConfigAddEventImpl<SettingsInfo>();
        evt.setSource(settings);
        
        event(evt);
    }

   public void fireSettingsModified(SettingsInfo settings, List<String> changed, List oldValues, 
            List newValues) {
        ConfigModifyEventImpl<SettingsInfo> evt = new ConfigModifyEventImpl<SettingsInfo>();
        evt.setSource(settings);
        evt.setPropertyNames(changed);
        evt.setNewValues(newValues);
        evt.setOldValues(oldValues);
        
        event(evt);
    }

   public void fireSettingsPostModified(SettingsInfo settings) {
       ConfigPostModifyEventImpl<SettingsInfo> evt = new ConfigPostModifyEventImpl<SettingsInfo>();
       evt.setSource(settings);
       
       event(evt);
    }

   public void fireSettingsRemoved(SettingsInfo settings) {
       ConfigRemoveEventImpl<SettingsInfo> evt = new ConfigRemoveEventImpl<SettingsInfo>();
       evt.setSource(settings);
       
       event(evt);
    }

    public LoggingInfo getLogging() {
        return facade.getLogging();
    }
    
    public void setLogging(LoggingInfo logging) {
        facade.setLogging(logging);
        fireLoggingPostModified();
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        if (factory instanceof ApplicationContextAware) {
            ((ApplicationContextAware)factory).setApplicationContext(context);
        }
    }

    public void add(ServiceInfo service) {
        if (service.getId() != null && facade.getService(service.getId(), ServiceInfo.class) != null) {
            throw new IllegalArgumentException( "service with id '" + service.getId() + "' already exists" );
        }

        resolve(service);
        WorkspaceInfo workspace = service.getWorkspace(); 
        if (workspace != null) {
            if (facade.getServiceByName(service.getName(), workspace, ServiceInfo.class) != null) {
                throw new IllegalArgumentException( "service with name '" + service.getName() + 
                    "' already exists in workspace '" + workspace.getName() + "'" );
            }
        }
        facade.add(service);
        
        //fire post modification event
        firePostServiceModified(service);
    }

    void resolve(ServiceInfo service) {
        resolveCollections(service);
    }

    public static <T> T unwrap(T obj) {
        return DefaultGeoServerFacade.unwrap(obj);
    }
    
    public <T extends ServiceInfo> T getService(Class<T> clazz) {
        WorkspaceInfo ws = LocalWorkspace.get();
        T service = ws != null ? facade.getService(ws, clazz) : null;
        service = service != null ? service : facade.getService(clazz);
        if(service == null) {
            LOGGER.log(Level.SEVERE, "Could not locate service of type " + clazz + ", local workspace is " + ws);
        }
        
        return service;
    }

    @Override
    public <T extends ServiceInfo> T getService(WorkspaceInfo workspace, Class<T> clazz) {
       return facade.getService(workspace, clazz);
    }

    public <T extends ServiceInfo> T getService(String id, Class<T> clazz) {
        return facade.getService(id, clazz);
    }

    public <T extends ServiceInfo> T getServiceByName(String name, Class<T> clazz) {
        T service = LocalWorkspace.get() != null ? 
            facade.getServiceByName(name, LocalWorkspace.get(), clazz) : null;
        return service != null ? service : facade.getServiceByName(name, clazz);
    }

    public <T extends ServiceInfo> T getServiceByName(WorkspaceInfo workspace, String name, Class<T> clazz) {
        return facade.getServiceByName(name, workspace, clazz);
    }

    public Collection<? extends ServiceInfo> getServices() {
        Collection<? extends ServiceInfo> services = 
            LocalWorkspace.get() != null ? facade.getServices(LocalWorkspace.get()) : null; 
        return services != null ? services : facade.getServices();
    }

    @Override
    public Collection<? extends ServiceInfo> getServices(WorkspaceInfo workspace) {
        return facade.getServices(workspace);
    }

    public void remove(ServiceInfo service) {
        facade.remove(service);

        fireServiceRemoved(service);
    }

    public void save(GeoServerInfo geoServer) {
        facade.save(geoServer);
        
        //fire post modification event
        fireGlobalPostModified();
    }

    public void save(LoggingInfo logging) {
        facade.save(logging);
        
        //fire post modification event
        fireLoggingPostModified();
    } 
    
    void fireGlobalPostModified() {
        ConfigPostModifyEventImpl<GeoServerInfo> evt = new ConfigPostModifyEventImpl<GeoServerInfo>();
        evt.setSource(facade.getGlobal());
        event(evt);
    }
    
    public void fireGlobalModified(GeoServerInfo global, List<String> changed, List oldValues, 
        List newValues) {
        ConfigModifyEventImpl<GeoServerInfo> evt = new ConfigModifyEventImpl<GeoServerInfo>();
        evt.setSource(facade.getGlobal());
        evt.setPropertyNames(changed);
        evt.setNewValues(newValues);
        evt.setOldValues(oldValues);
        
        event(evt);
    }

    public void fireLoggingModified(LoggingInfo logging, List<String> changed, List oldValues, 
            List newValues) {
        ConfigModifyEventImpl<LoggingInfo> evt = new ConfigModifyEventImpl<LoggingInfo>();
        evt.setSource(logging);
        evt.setPropertyNames(changed);
        evt.setNewValues(newValues);
        evt.setOldValues(oldValues);
        event(evt);
    }
    
    void fireLoggingPostModified() {
        ConfigModifyEventImpl<LoggingInfo> evt = new ConfigModifyEventImpl<LoggingInfo>();
        evt.setSource(facade.getLogging());
        
        event(evt);
    }
    
    public void save(ServiceInfo service) {
        validate(service);

        facade.save(service);
        
        //fire post modification event
        firePostServiceModified(service);
    }

    void validate(ServiceInfo service) {
        CatalogImpl.validateKeywords(service.getKeywords());
    }

    public void fireServiceModified(ServiceInfo service, List<String> changed, List oldValues, 
            List newValues) {
        ConfigModifyEventImpl<ServiceInfo> evt = new ConfigModifyEventImpl<ServiceInfo>();
        evt.setSource(service);
        evt.setPropertyNames(changed);
        evt.setNewValues(newValues);
        evt.setOldValues(oldValues);
        
        event(evt);
    }
    
    void firePostServiceModified(ServiceInfo service) {
        ConfigPostModifyEventImpl<ServiceInfo> evt = new ConfigPostModifyEventImpl<ServiceInfo>();
        evt.setSource(service);
        event(evt);
    }

    void fireServiceRemoved(ServiceInfo service) {
        ConfigRemoveEventImpl<ServiceInfo> evt = new ConfigRemoveEventImpl<ServiceInfo>();
        evt.setSource(service);
        event(evt);
    }
    public void addListener(ConfigurationListener listener) {
        listeners.add(new ConfigurationListenerWrapper(listener));
    }
    
    public void removeListener(ConfigurationListener listener) {
        listeners.remove( new ConfigurationListenerWrapper(listener) );
    }
    
    public Collection<ConfigurationListener> getListeners() {
        return null;
        // FIXME Need to make this backward compatible
    }
    
    public void dispose() {
        // look for pluggable handlers
        for(GeoServerLifecycleHandler handler : GeoServerExtensions.extensions(GeoServerLifecycleHandler.class)) {
            try {
                handler.onDispose();
            } catch(Throwable t) {
                LOGGER.log(Level.SEVERE, "A GeoServer lifecycle handler threw an exception during dispose", t);
            }
        }

        // internal cleanup
        
        if ( catalog != null ) catalog.dispose();
        if ( facade != null ) facade.dispose();
    }

    public void reload() throws Exception {
        // flush caches
        reset();
        
        // reload configuration
        GeoServerLoaderProxy loader = GeoServerExtensions.bean(GeoServerLoaderProxy.class);
        synchronized (org.geoserver.config.GeoServer.CONFIGURATION_LOCK) {
            getCatalog().getResourcePool().dispose();
            loader.reload();
        }
        
        // look for pluggable handlers
        for(GeoServerLifecycleHandler handler : GeoServerExtensions.extensions(GeoServerLifecycleHandler.class)) {
            try {
                handler.onReload();
            } catch(Throwable t) {
                LOGGER.log(Level.SEVERE, "A GeoServer lifecycle handler threw an exception during reload", t);
            }
        }
    }

    public void reset() {
        // drop all the catalog store/feature types/raster caches
        catalog.getResourcePool().dispose();
        
        // reset the referencing subsystem
        CRS.reset("all");
        
        // look for pluggable handlers
        for(GeoServerLifecycleHandler handler : GeoServerExtensions.extensions(GeoServerLifecycleHandler.class)) {
            try {
                handler.onReset();
            } catch(Throwable t) {
                LOGGER.log(Level.SEVERE, "A GeoServer lifecycle handler threw an exception during reset", t);
            }
        }
    }
    
    protected void event(ConfigEvent<?> event) {
        Exception toThrow = null;
        
        if(event.getSource()==null) {
            throw new NullPointerException("A ConfigEvent must have a source to be fired.");
        }
        ConfigInfo source = event.getSource();
        
        final String handle="handle";
        String noun=null;
        String verb=null;
        Class<?> clazz=null;
        
        // Constucting the method name this way is a lot neater and less repetitive.
        
        if (source instanceof ServiceInfo){
            noun="Service";
        } else if (source instanceof SettingsInfo) {
            noun="Settings";
        } else if (source instanceof LoggingInfo) {
            noun="Logging";
        } else if (source instanceof GeoServerInfo) {
            noun="Global";
        }
        
        if (event instanceof ConfigAddEvent) {
            verb="Add";
            clazz=ConfigAddEvent.class;
        } else if(event instanceof ConfigRemoveEvent) {
            verb="Remove";
            clazz=ConfigRemoveEvent.class;
        } else if(event instanceof ConfigModifyEvent) {
            verb="Modify";
            clazz=ConfigModifyEvent.class;
        } else if(event instanceof ConfigPostModifyEvent) {
            verb="PostModify";
            clazz=ConfigPostModifyEvent.class;
        }
        if(noun==null||verb==null||clazz==null) {
            throw new UnsupportedOperationException("Unknown ConfigInfo ("+source.getClass().getName()+") or ConfigEvent ("+event.getClass().getName()+")");
        }
        
        Method listenerMethod;
        try {
            listenerMethod = ConfigListener.class.getMethod(handle+noun+verb, clazz);
        } catch (SecurityException e) {
            throw new IllegalStateException(e);
        } catch (NoSuchMethodException e) {
            throw new UnsupportedOperationException("ConfigListener inteface does not support the event: "+noun+verb);
        }
        
        for (ConfigListener listener: listeners) {
            try {
                listenerMethod.invoke(listener, event);
            } catch(Throwable t) {
                LOGGER.log(Level.WARNING, "Catalog listener threw exception handling event.", t);
            }
        }
    }

    @Override
    public void addListener(ConfigListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(ConfigListener listener) {
        listeners.remove(listener);
    }

    @Override
    public Collection<ConfigListener> getConfigListeners() {
        return listeners;
    }

}
