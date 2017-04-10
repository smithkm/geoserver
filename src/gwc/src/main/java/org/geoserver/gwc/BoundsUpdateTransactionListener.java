package org.geoserver.gwc;

import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.jxpath.ri.QName;
import org.eclipse.emf.ecore.EObject;
import org.geoserver.catalog.Catalog;
import org.geoserver.wfs.TransactionEvent;
import org.geoserver.wfs.TransactionEventType;
import org.geoserver.wfs.TransactionPlugin;
import org.geoserver.wfs.WFSException;
import org.geotools.util.logging.Logging;

import net.opengis.wfs.DeleteElementType;
import net.opengis.wfs.InsertElementType;
import net.opengis.wfs.TransactionResponseType;
import net.opengis.wfs.TransactionType;
import net.opengis.wfs.UpdateElementType;

public class BoundsUpdateTransactionListener implements TransactionPlugin {
    private static Logger log = Logging.getLogger(BoundsUpdateTransactionListener.class);
    
    Catalog catalog;
    
    @Override
    public void dataStoreChange(TransactionEvent event) throws WFSException {
        log.info("DataStoreChange: " + event.getLayerName() + " " + event.getType());
        try {
            dataStoreChangeInternal(event);
        } catch (RuntimeException e) {
            // Do never make the transaction fail due to a GWC error. Yell on the logs though
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
            // Do never make the transaction fail due to a GWC error. Yell on the logs though
            log.log(Level.WARNING, "Error trying to update bounds to include affected area", e);
        }
    
    }
    
    @Override
    public int getPriority() {
        return 0;
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
        
        catalog.getFeatureType(null).setNativeBoundingBox(box);
        catalog.getLayerGroup("").setBounds(bounds);
        final javax.xml.namespace.QName featureTypeName = event.getLayerName();
        final Set<String> affectedTileLayers = gwc.getTileLayersByFeatureType(
                featureTypeName.getNamespaceURI(), featureTypeName.getLocalPart());
        if (affectedTileLayers.isEmpty()) {
            // event didn't touch a cached layer
            return;
        }

        final SimpleFeatureCollection affectedFeatures = event.getAffectedFeatures();
        final ReferencedEnvelope affectedBounds = affectedFeatures.getBounds();

        final TransactionType transaction = event.getRequest();

        for (String tileLayerName : affectedTileLayers) {
            addLayerDirtyRegion(transaction, tileLayerName, affectedBounds);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, List<ReferencedEnvelope>> getByLayerDirtyRegions(
            final TransactionType transaction) {

        final Map<Object, Object> extendedProperties = transaction.getExtendedProperties();
        Map<String, List<ReferencedEnvelope>> byLayerDirtyRegions;
        byLayerDirtyRegions = (Map<String, List<ReferencedEnvelope>>) extendedProperties
                .get(GWC_TRANSACTION_INFO_PLACEHOLDER);
        if (byLayerDirtyRegions == null) {
            byLayerDirtyRegions = new HashMap<String, List<ReferencedEnvelope>>();
            extendedProperties.put(GWC_TRANSACTION_INFO_PLACEHOLDER, byLayerDirtyRegions);
        }
        return byLayerDirtyRegions;
    }

    private void addLayerDirtyRegion(final TransactionType transaction, final String tileLayerName,
            final ReferencedEnvelope affectedBounds) {

        Map<String, List<ReferencedEnvelope>> byLayerDirtyRegions = getByLayerDirtyRegions(transaction);

        List<ReferencedEnvelope> layerDirtyRegion = byLayerDirtyRegions.get(tileLayerName);
        if (layerDirtyRegion == null) {
            layerDirtyRegion = new ArrayList<ReferencedEnvelope>(2);
            byLayerDirtyRegions.put(tileLayerName, layerDirtyRegion);
        }
        layerDirtyRegion.add(affectedBounds);
    }
}
