package org.geoserver.gwc.layer;

import org.geowebcache.filter.parameters.ParameterFilter;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Parameter filter which knows which GeoServer layer is being cached.
 * 
 * @author Kevin Smith, Boundless
 */
@ParametersAreNonnullByDefault
public abstract class GeoServerParameterFilter extends ParameterFilter {
    
    transient GeoServerTileLayer tileLayer;
    
    final void setLayer(GeoServerTileLayer tileLayer) {
        if(this.tileLayer==null) {
            this.tileLayer=tileLayer;
            onLayerSet();
        } else if(this.tileLayer!=tileLayer) {
            throw new IllegalStateException("Attempted to set tile layer to "+tileLayer+" when it was already set to "+this.tileLayer);
        }
    }
    
    /**
     * Called after the layer has been set.
     */
    protected void onLayerSet(){
        // Do nothing
    }
    
    /**
     * Get the layer
     * @return
     */
    protected GeoServerTileLayer getLayer() {
        return tileLayer;
    }
    
    
}
