package org.geoserver.gwc;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import net.opengis.wfs.DeleteElementType;
import net.opengis.wfs.InsertElementType;
import net.opengis.wfs.TransactionResponseType;
import net.opengis.wfs.TransactionType;
import net.opengis.wfs.UpdateElementType;

import org.eclipse.emf.ecore.EObject;
import org.geoserver.wfs.TransactionEvent;
import org.geoserver.wfs.TransactionEventType;
import org.geoserver.wfs.TransactionPlugin;
import org.geoserver.wfs.WFSException;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.util.logging.Logging;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

/**
 * Listens to transactions (so far only issued by WFS)
 * <p>
 * A Spring bean singleton of this class needs to be declared in order for GeoServer transactions to
 * pick it up automatically and forward transaction events to it.
 * @param <T>
 */
// Originally extracted from GWCTransactionListener
public abstract class DirtyRegionListener<T> implements TransactionPlugin {
    
    private static Logger log = Logging.getLogger(DirtyRegionListener.class);
    
    protected abstract String getPlaceholderTag();
    
    /**
     * Get the names of entities affected by a change to the named feature type
     * @param featureTypeName
     * @return
     */
    protected abstract Set<String> getAffected(final QName featureTypeName);
    
    /**
     * Get the CRS
     * @param affectedName
     * @return
     */
    protected abstract CoordinateReferenceSystem getCrs(final String affectedName);
    
    protected abstract void handleChange(String affectedName, T dirtyRegion);
    
    public DirtyRegionListener() {
        super();
    }
    
    /**
     * Not used, we're interested in the {@link #dataStoreChange} and {@link #afterTransaction}
     * hooks
     * 
     * @see org.geoserver.wfs.TransactionPlugin#beforeTransaction(net.opengis.wfs.TransactionType)
     */
    public TransactionType beforeTransaction(TransactionType request) throws WFSException {
        // nothing to do
        return request;
    }
    
    /**
     * Not used, we're interested in the {@link #dataStoreChange} and {@link #afterTransaction}
     * hooks
     * 
     * @see org.geoserver.wfs.TransactionPlugin#beforeCommit(net.opengis.wfs.TransactionType)
     */
    public void beforeCommit(TransactionType request) throws WFSException {
        // nothing to do
    }
    
    /**
     * If transaction's succeeded then truncate the affected layers at the transaction affected
     * bounds
     * 
     * @see org.geoserver.wfs.TransactionPlugin#afterTransaction
     */
    public void afterTransaction(final TransactionType request, TransactionResponseType result, boolean committed) {
        if (!committed) {
            return;
        }
        try {
            afterTransactionInternal(request, committed);
        } catch (RuntimeException e) {
            // Do never make the transaction fail due to a GWC error. Yell on the logs though
            log.log(Level.WARNING, "Error trying to truncate the transaction affected area", e);
        }
    }
    
    private void afterTransactionInternal(final TransactionType transaction, boolean committed) {
    
        final Map<String, List<T>> byLayerDirtyRegions = getByLayerDirtyRegions(transaction);
        if (byLayerDirtyRegions.isEmpty()) {
            return;
        }
        for (String tileLayerName : byLayerDirtyRegions.keySet()) {
            List<T> dirtyList = byLayerDirtyRegions.get(tileLayerName);
            T dirtyRegion;
            try {
                CoordinateReferenceSystem crs = getCrs(tileLayerName);
                dirtyRegion = merge(crs, dirtyList);
            } catch (Exception e) {
                log.log(Level.WARNING, e.getMessage(), e);
                continue;
            }
            if (dirtyRegion == null) {
                continue;
            }
            handleChange(tileLayerName, dirtyRegion);
        }
    }
    
    protected abstract T merge(final CoordinateReferenceSystem crs, final Collection<T> dirtyList) throws TransformException,
            FactoryException;
    
    /**
     * @return {@code 0}, we don't need any special treatment
     * @see org.geoserver.wfs.TransactionPlugin#getPriority()
     */
    public int getPriority() {
        return 0;
    }
    
    /**
     * Collects the per TileLayer affected bounds
     * 
     * @see org.geoserver.wfs.TransactionListener#dataStoreChange(org.geoserver.wfs.TransactionEvent)
     */
    public void dataStoreChange(final TransactionEvent event) throws WFSException {
        log.info("DataStoreChange: " + event.getLayerName() + " " + event.getType());
        try {
            dataStoreChangeInternal(event);
        } catch (RuntimeException e) {
            // Do never make the transaction fail due to a GWC error. Yell on the logs though
            log.log(Level.WARNING, "Error pre computing the transaction's affected area", e);
        }
    }
    
    private void dataStoreChangeInternal(final TransactionEvent event) {
        final Object source = event.getSource();
        if (!(source instanceof InsertElementType || source instanceof UpdateElementType || source instanceof DeleteElementType)) {
            return;
        }
    
        final EObject originatingTransactionRequest = (EObject) source;
        Objects.requireNonNull(originatingTransactionRequest, "No original transaction request exists");
        final TransactionEventType type = event.getType();
        if (TransactionEventType.POST_INSERT.equals(type)) {
            // no need to compute the bounds, they're the same than for PRE_INSERT
            return;
        }
        final QName featureTypeName = event.getLayerName();
        final Set<String> affectedTileLayers = getAffected(featureTypeName);
        if (affectedTileLayers.isEmpty()) {
            // event didn't touch a cached layer
            return;
        }
    
        final SimpleFeatureCollection affectedFeatures = event.getAffectedFeatures();
        changes(event, affectedTileLayers, affectedFeatures);
    }

    protected abstract void changes(final TransactionEvent event,
            final Set<String> affectedTileLayers,
            final SimpleFeatureCollection affectedFeatures);
    
    @SuppressWarnings("unchecked")
    protected Map<String, List<T>> getByLayerDirtyRegions(final TransactionType transaction) {
    
        final Map<Object, Object> extendedProperties = transaction.getExtendedProperties();
        Map<String, List<T>> byLayerDirtyRegions;
        byLayerDirtyRegions = (Map<String, List<T>>) extendedProperties
                .get(getPlaceholderTag());
        if (byLayerDirtyRegions == null) {
            byLayerDirtyRegions = new HashMap<String, List<T>>();
            extendedProperties.put(getPlaceholderTag(), byLayerDirtyRegions);
        }
        return byLayerDirtyRegions;
    }
    
}