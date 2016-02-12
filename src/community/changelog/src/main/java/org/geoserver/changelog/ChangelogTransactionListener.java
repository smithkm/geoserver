/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.changelog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import net.opengis.wfs.TransactionType;

import org.geoserver.catalog.Catalog;
import org.geoserver.gwc.DirtyRegionListener;
import org.geoserver.wfs.TransactionEvent;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.Name;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Geometry;

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
public class ChangelogTransactionListener extends DirtyRegionListener<Geometry> {
    
    Catalog getCatalog() {
        return null; // TODO
    }
    
    @SuppressWarnings("unused")
    private static Logger log = Logging.getLogger(ChangelogTransactionListener.class);
    
    static final String CHANGELOG_TRANSACTION_INFO_PLACEHOLDER = "CHANGELOG_TRANSACTION_INFO_PLACEHOLDER";
    
    @Override
    protected String getPlaceholderTag() {
        return CHANGELOG_TRANSACTION_INFO_PLACEHOLDER;
    }
    
    Name qnameToName(final QName qname) {
        return new NameImpl(qname.getPrefix(), qname.getLocalPart());
    }
    
    @Override
    protected Set<String> getAffected(QName featureTypeName) {
        return Collections.singleton(getCatalog().getFeatureTypeByName(qnameToName(featureTypeName)).getId().replace(":", "-"));
    }

    @Override
    protected CoordinateReferenceSystem getCrs(String affectedName) {
        
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void handleChange(String affectedName, Geometry dirtyRegion) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected Geometry merge(CoordinateReferenceSystem crs,
            Collection<Geometry> dirtyList) throws TransformException,
            FactoryException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void changes(TransactionEvent event,
            Set<String> affectedTileLayers,
            SimpleFeatureCollection affectedFeatures) {
        // TODO Auto-generated method stub
        
    }
    

}
