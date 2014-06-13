/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.gwc.layer;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.util.logging.Logging;
import org.geowebcache.filter.parameters.ParameterException;
import org.geowebcache.filter.parameters.ParameterFilter;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * ParameterFilter which allows the workspace of the back end to be specified. Maintains a set 
 * of allowed workspaces which are intersected with those available on the layer.
 * 
 * @author Kevin Smith, Boundless
 *
 */
@XStreamAlias("workspaceParameterFilter")
public class WorkspaceParameterFilter extends GeoServerParameterFilter {

    private static final Logger LOGGER = Logging.getLogger(GeoServerTileLayerInfoImpl.class);
    
    boolean global;
    boolean local;
    
    @Override
    public String apply(String arg0) throws ParameterException {
        // TODO Auto-generated method stub
        return null;
    }
    
    WorkspaceInfo getWorkspace() {
        LayerInfo li = getLayer().getLayerInfo();
        if(li!=null) {
            return li.getResource().getStore().getWorkspace();
        } else {
            LayerGroupInfo lgi = getLayer().getLayerGroupInfo();
            return lgi.getWorkspace();
        }
    }
    
    PublishedInfo getPublished() {
        LayerInfo li = getLayer().getLayerInfo();
        if(li!=null) {
            return li;
        } else {
            LayerGroupInfo lgi = getLayer().getLayerGroupInfo();
            return lgi;
        }
    }
    
    Catalog getCatalog() {
        return null;
    }
    
    @Override
    public List<String> getLegalValues() {
        List<String> values = new ArrayList<>(2);
        if(global) {
            values.add("");
        }
        if(local) {
            values.add(getWorkspace().getName());
        }
        return values;
    }
    
    @Override
    protected void onLayerSet(){
        final LayerInfo li = getLayer().getLayerInfo();
        if(li!=null) {
            getCatalog().addListener(new CatalogListener() {

                @Override
                public void handleAddEvent(CatalogAddEvent event)
                        throws CatalogException {
                    // TODO Auto-generated method stub
                    
                }

                @Override
                public void handleRemoveEvent(CatalogRemoveEvent event)
                        throws CatalogException {
                    // TODO Auto-generated method stub
                    
                }

                @Override
                public void handleModifyEvent(CatalogModifyEvent event)
                        throws CatalogException {
                    
                    
                    if(event.getSource()==li.getResource().getStore() &&
                            event.getPropertyNames().contains("workspace")) {
                        // Workspace changed
                        
                    }
                }

                @Override
                public void handlePostModifyEvent(CatalogPostModifyEvent event)
                        throws CatalogException {
                    // TODO Auto-generated method stub
                    
                }

                @Override
                public void reloaded() {
                    // TODO Auto-generated method stub
                    
                }
                
            });
            return li.getResource().getStore().getWorkspace();
        } else {
            LayerGroupInfo lgi = getLayer().getLayerGroupInfo();
            return lgi.getWorkspace();
        }

        
    }

    @Override
    public ParameterFilter clone() {
        // TODO Auto-generated method stub
        return null;
    }
}
