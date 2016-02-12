/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import net.opengis.wfs.TransactionType;

import org.geoserver.wfs.TransactionEvent;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;
import org.geowebcache.GeoWebCacheException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

/**
 * Listens to transactions (so far only issued by WFS) and truncates the cache for the affected area
 * of the layers involved in the transaction.
 * <p>
 * A Spring bean singleton of this class needs to be declared in order for GeoServer transactions to
 * pick it up automatically and forward transaction events to it.
 * </p>
 * <p>
 * TODO: upon deletion, only truncate if feature count > 0
 * </p>
 * 
 * @author Arne Kepp
 * @author Gabriel Roldan
 * @version $Id$
 * 
 */
public class GWCTransactionListener extends DirtyRegionListener<ReferencedEnvelope> {

    private static Logger log = Logging.getLogger(GWCTransactionListener.class);
    
    final private GWC gwc;

    static final String GWC_TRANSACTION_INFO_PLACEHOLDER = "GWC_TRANSACTION_INFO_PLACEHOLDER";

    /**
     * @param gwc
     */
    public GWCTransactionListener(final GWC gwc) {
        this.gwc = gwc;
    }

    @Override
    protected void handleChange(String tileLayerName,
            ReferencedEnvelope dirtyRegion) {
        try {
            gwc.truncate(tileLayerName, dirtyRegion);
        } catch (GeoWebCacheException e) {
            log.warning("Error truncating tile layer " + tileLayerName
                    + " for transaction affected bounds " + dirtyRegion);
        }
    }
    
    @Override
    protected CoordinateReferenceSystem getCrs(final String tileLayerName) {
        return gwc.getDeclaredCrs(tileLayerName);
    }
    
    @Override
    protected Set<String> getAffected(final QName featureTypeName) {
        return gwc.getTileLayersByFeatureType(
                featureTypeName.getNamespaceURI(), featureTypeName.getLocalPart());
    }
    
    @Override
    protected String getPlaceholderTag() {
        return GWC_TRANSACTION_INFO_PLACEHOLDER;
    }

    @Override
    protected void changes(final TransactionEvent event, final Set<String> affectedTileLayers, final SimpleFeatureCollection affectedFeatures) {
        final ReferencedEnvelope affectedBounds = affectedFeatures.getBounds();
    
        final TransactionType transaction = event.getRequest();
    
        for (String tileLayerName : affectedTileLayers) {
            addLayerDirtyRegion(transaction, tileLayerName, affectedBounds);
        }
    }

    protected void addLayerDirtyRegion(final TransactionType transaction, final String tileLayerName, final ReferencedEnvelope affectedBounds) {
    
        Map<String, List<ReferencedEnvelope>> byLayerDirtyRegions = getByLayerDirtyRegions(transaction);
    
        List<ReferencedEnvelope> layerDirtyRegion = byLayerDirtyRegions.get(tileLayerName);
        if (layerDirtyRegion == null) {
            layerDirtyRegion = new ArrayList<ReferencedEnvelope>(2);
            byLayerDirtyRegions.put(tileLayerName, layerDirtyRegion);
        }
        layerDirtyRegion.add(affectedBounds);
    }

    @Override
    protected ReferencedEnvelope merge(final CoordinateReferenceSystem declaredCrs, final Collection<ReferencedEnvelope> dirtyList)
            throws TransformException, FactoryException {
                if (dirtyList.size() == 0) {
                    return null;
                }
            
                ReferencedEnvelope merged = new ReferencedEnvelope(declaredCrs);
                for (ReferencedEnvelope env : dirtyList) {
                    ReferencedEnvelope transformedDirtyRegion = env.transform(declaredCrs, true, 1000);
                    merged.expandToInclude(transformedDirtyRegion);
                }
                return merged;
            }
}
