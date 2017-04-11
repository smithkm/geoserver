package org.geoserver.gwc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.emf.ecore.EObject;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.wfs.TransactionEvent;
import org.geoserver.wfs.TransactionEventType;
import org.geoserver.wfs.TransactionPlugin;
import org.geoserver.wfs.WFSException;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.Name;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import net.opengis.wfs.InsertElementType;
import net.opengis.wfs.TransactionResponseType;
import net.opengis.wfs.TransactionType;
import net.opengis.wfs.UpdateElementType;

public class BoundsUpdateTransactionListener implements TransactionPlugin {
    private static Logger log = Logging.getLogger(BoundsUpdateTransactionListener.class);
    
    static final String TRANSACTION_INFO_PLACEHOLDER = "BOUNDS_UPDATE_TRANSACTION_INFO_PLACEHOLDER";

    Catalog catalog;
    
    public BoundsUpdateTransactionListener(Catalog catalog) {
        super();
        this.catalog = catalog;
    }

    @Override
    public void dataStoreChange(TransactionEvent event) throws WFSException {
        log.info("DataStoreChange: " + event.getLayerName() + " " + event.getType());
        try {
            dataStoreChangeInternal(event);
        } catch (RuntimeException e) {
            log.log(Level.WARNING, "Error pre computing the transaction's affected area", e);
        }
    }
    
    @Override
    public TransactionType beforeTransaction(TransactionType request)
            throws WFSException {
        return request;
    }
    
    @Override
    public void beforeCommit(TransactionType request) throws WFSException {
        // Do Nothing
    
    }
    
    @Override
    public void afterTransaction(TransactionType request,
            TransactionResponseType result, boolean committed) {
        if (!committed) {
            return;
        }
        try {
            afterTransactionInternal(request, committed);
        } catch (RuntimeException e) {
            log.log(Level.WARNING, "Error trying to update bounds to include affected area", e);
        }
    
    }
    
    @Override
    public int getPriority() {
        return 0;
    }
    
    private void dataStoreChangeInternal(final TransactionEvent event) {
        final Object source = event.getSource();
        if (!(source instanceof InsertElementType || source instanceof UpdateElementType)) {
            // We only care about operations that potentially the bounds.
            return;
        }
        
        final EObject originatingTransactionRequest = (EObject) source;
        Objects.requireNonNull(originatingTransactionRequest, "No original transaction request exists");
        final TransactionEventType type = event.getType();
        if (TransactionEventType.POST_INSERT.equals(type)) {
            // no need to compute the bounds, they're the same as for PRE_INSERT
            return;
        }
        
        final Name featureTypeName = new NameImpl(event.getLayerName());
        final FeatureTypeInfo fti = catalog.getFeatureTypeByName(featureTypeName);
        if(Objects.isNull(fti)) {
            return;
        }
        
        final SimpleFeatureCollection affectedFeatures = event.getAffectedFeatures();
        final ReferencedEnvelope affectedBounds = affectedFeatures.getBounds();
        
        final TransactionType transaction = event.getRequest();
        
        addDirtyRegion(transaction, featureTypeName, affectedBounds);
    }
    
    @SuppressWarnings("unchecked")
    private Map<Name, Collection<ReferencedEnvelope>> getByLayerDirtyRegions(
            final TransactionType transaction) {
        
        final Map<Object, Object> extendedProperties = transaction.getExtendedProperties();
        Map<Name, Collection<ReferencedEnvelope>> byLayerDirtyRegions;
        byLayerDirtyRegions = (Map<Name, Collection<ReferencedEnvelope>>) extendedProperties
                .get(TRANSACTION_INFO_PLACEHOLDER);
        if (byLayerDirtyRegions == null) {
            byLayerDirtyRegions = new HashMap<Name, Collection<ReferencedEnvelope>>();
            extendedProperties.put(TRANSACTION_INFO_PLACEHOLDER, byLayerDirtyRegions);
        }
        return byLayerDirtyRegions;
    }
    
    private void addDirtyRegion(final TransactionType transaction, final Name featureTypeName,
            final ReferencedEnvelope affectedBounds) {
        
        Map<Name, Collection<ReferencedEnvelope>> byLayerDirtyRegions = getByLayerDirtyRegions(transaction);
        
        Collection<ReferencedEnvelope> layerDirtyRegion = byLayerDirtyRegions.get(featureTypeName);
        if (layerDirtyRegion == null) {
            layerDirtyRegion = new ArrayList<ReferencedEnvelope>(2);
            byLayerDirtyRegions.put(featureTypeName, layerDirtyRegion);
        }
        layerDirtyRegion.add(affectedBounds);
    }
    
    private void afterTransactionInternal(final TransactionType transaction, boolean committed) {
        
        final Map<Name, Collection<ReferencedEnvelope>> byLayerDirtyRegions = getByLayerDirtyRegions(transaction);
        if (byLayerDirtyRegions.isEmpty()) {
            return;
        }
        byLayerDirtyRegions.entrySet().stream().forEach(e->{
            FeatureTypeInfo fti = catalog.getFeatureTypeByName(e.getKey());
            try {
                merge(fti, e.getValue()).ifPresent(dirtyRegion->{
                    ReferencedEnvelope bounds = fti.getNativeBoundingBox();
                    bounds.expandToInclude(dirtyRegion);
                    fti.setNativeBoundingBox(bounds);
                    catalog.save(fti);
                });
            } catch (Exception ex) {
                log.log(Level.WARNING, ex.getMessage(), ex);
                return;
            }
        });
    }
    
    private Optional<ReferencedEnvelope> merge(final FeatureTypeInfo fti,
            final Collection<ReferencedEnvelope> dirtyList) {
        final CoordinateReferenceSystem declaredCrs = fti.getNativeBoundingBox().getCoordinateReferenceSystem();
        return dirtyList.stream()
            .map(env->{
                if(env instanceof ReferencedEnvelope3D) {
                    return new ReferencedEnvelope(env, CRS.getHorizontalCRS(env.getCoordinateReferenceSystem()));
                } else  {
                    return env;
                }
            })
            .map(env->{
                try {
                    return env.transform(declaredCrs, true, 1000);
                } catch (TransformException | FactoryException e) {
                    throw new RuntimeException("Error while merging bounding boxes",e);
                }
            })
            .reduce((env1, env2)->{ReferencedEnvelope x = new ReferencedEnvelope(env1); x.expandToInclude(env2); return x;});
    }
    
}
