/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.util.List;
import java.util.logging.Logger;

import org.geoserver.platform.util.GeoServerPropertyFactoryBean;
import org.geotools.util.logging.Logging;

/**
 * Defines LayerGroup visibility policy, used by AdvertisedCatalog.
 * 
 * @author Davide Savazzi - GeoSolutions
 */
public interface LayerGroupVisibilityPolicy {

    /**
     * 
     * @param group
     * @param filteredLayers
     * @return true if LayerGroup must be hidden, false otherwise
     */
    boolean hideLayerGroup(LayerGroupInfo group, List<PublishedInfo> filteredLayers);

    /**
     * Never hide a LayerGroup
     */
    public static final LayerGroupVisibilityPolicy HIDE_NEVER = new LayerGroupVisibilityPolicy() {
        @Override
        public boolean hideLayerGroup(LayerGroupInfo group, List<PublishedInfo> filteredLayers) {
            return false;
        }       
    };
    
    /**
     * Hide a LayerGroup if it doesn't contain Layers or if its Layers are all hidden
     */
    public static final LayerGroupVisibilityPolicy HIDE_EMPTY = new LayerGroupVisibilityPolicy() {
        @Override
        public boolean hideLayerGroup(LayerGroupInfo group, List<PublishedInfo> filteredLayers) {
            return filteredLayers.size() == 0;
        }       
    };    
    
    /**
     * Hide a LayerGroup if its Layers are all hidden
     */
    public static final LayerGroupVisibilityPolicy HIDE_IF_ALL_HIDDEN = new LayerGroupVisibilityPolicy() {
        @Override
        public boolean hideLayerGroup(LayerGroupInfo group, List<PublishedInfo> filteredLayers) {
            return filteredLayers.size() == 0 && group.getLayers().size() > 0;
        }       
    };
    
    /**
     * Factory Bean providing one of the visibility properties depending on the value of the 
     * property GEOSERVER_LAYERGROUP_VISIBILITY
     * 
     * @author Kevin Smith, Boundless
     *
     */
    public static class Factory extends GeoServerPropertyFactoryBean<LayerGroupVisibilityPolicy> {
        private static final Logger LOGGER = Logging.getLogger(Factory.class);
        public Factory() {
            super("GEOSERVER_LAYERGROUP_VISIBILITY");
        }
        
        @Override
        public Class<?> getObjectType() {
            return LayerGroupVisibilityPolicy.class;
        }
        
        @Override
        protected LayerGroupVisibilityPolicy createInstance(final String policyName) {
            if(policyName.equals("HIDE_NEVER")) {
                return HIDE_NEVER;
            } else if(policyName.equals("HIDE_EMPTY")) {
                return HIDE_EMPTY;
            } else if(policyName.equals("HIDE_IF_ALL_HIDDEN")) {
                return HIDE_IF_ALL_HIDDEN;
            } else {
                return null;
            }
        }
        
    }
}
