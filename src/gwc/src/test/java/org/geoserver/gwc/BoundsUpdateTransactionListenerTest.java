/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import static org.geotools.referencing.crs.DefaultGeographicCRS.WGS84;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import net.opengis.wfs.InsertElementType;
import net.opengis.wfs.TransactionResponseType;
import net.opengis.wfs.TransactionType;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.wfs.TransactionEvent;
import org.geoserver.wfs.TransactionEventType;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.geotools.referencing.CRS;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.type.Name;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import org.easymock.EasyMock;

public class BoundsUpdateTransactionListenerTest {
    
    private BoundsUpdateTransactionListener listener;
    
    private Catalog catalog;
    
    private FeatureTypeInfo featureType1, featureType2;
    
    private QName featureTypeQName1 = new QName("foo", "bar"),
                  featureTypeQName2 = new QName("foo", "quux");
    private Name featureTypeName1 = new NameImpl(featureTypeQName1),
                  featureTypeName2 = new NameImpl(featureTypeQName2);
    
    @Before
    public void setUp() throws Exception {
        catalog = EasyMock.createMock("catalog", Catalog.class);
        featureType1 = EasyMock.createMock("featureType1", FeatureTypeInfo.class);
        featureType2 = EasyMock.createMock("featureType2", FeatureTypeInfo.class);
        
        listener = new BoundsUpdateTransactionListener(catalog);
    }
    
    @Test
    public void testNoInteractionsInUnusedMethods() {
        TransactionType request = EasyMock.createNiceMock(TransactionType.class);
        EasyMock.replay(catalog, featureType1, featureType2, request);
        
        TransactionType returned = listener.beforeTransaction(request);
        Assert.assertThat(returned, Matchers.sameInstance(request));
        listener.beforeCommit(request);
        
        EasyMock.verify(catalog, featureType1, featureType2, request);
    }
    
    @Test
    public void testAfterTransactionUncommitted() {
        TransactionType request = EasyMock.createNiceMock(TransactionType.class);
        EasyMock.replay(catalog, featureType1, featureType2, request);
        TransactionResponseType result = EasyMock.createNiceMock(TransactionResponseType.class);
        boolean committed = false;
        
        listener.afterTransaction(request, result, committed);
        
        EasyMock.verify(catalog, featureType1, featureType2, request);
    }
    
    @Test
    public void testDataStoreChangeDoesNotPropagateExceptions() {
        TransactionEvent event = EasyMock.createNiceMock(TransactionEvent.class);
        EasyMock.expect(event.getSource()).andStubThrow(new RuntimeException("This exception should be eaten to prevent the transaction from failing"));
        EasyMock.replay(catalog, featureType1, featureType2, event);
        
        listener.dataStoreChange(event);
        
        EasyMock.verify(catalog, featureType1, featureType2, event);
    }
    
    @Test
    public void testDataStoreChangeOfNoInterest() {
        TransactionEvent event = EasyMock.createNiceMock(TransactionEvent.class);
        EasyMock.expect(event.getSource()).andReturn(featureType2).once();
        EasyMock.expect(event.getLayerName()).andReturn(featureTypeQName2).once();
        EasyMock.expect(event.getType()).andReturn(TransactionEventType.PRE_INSERT).once();
        
        EasyMock.replay(catalog, featureType1, featureType2, event);
        
        listener.dataStoreChange(event);
        
        EasyMock.verify(catalog, featureType1, featureType2, event);
    }
    
    @Test
    public void testDataStoreChangePostInsert() {
        TransactionEvent event = EasyMock.createNiceMock(TransactionEvent.class);
        InsertElementType insert = EasyMock.createNiceMock(InsertElementType.class);
        EasyMock.expect(event.getSource()).andStubReturn(insert);
        EasyMock.expect(event.getLayerName()).andStubReturn(featureTypeQName1);
        EasyMock.expect(event.getType()).andStubReturn(TransactionEventType.POST_INSERT);
        
        EasyMock.replay(catalog, featureType1, featureType2, event, insert);
        
        listener.dataStoreChange(event);
        // no need to do anything at post insert, bounds computed at pre_insert
        EasyMock.verify(catalog, featureType1, featureType2, event, insert);
    }
    
    @Test
    public void testDataStoreChangeDoesNotAffectType() {
        TransactionEvent event = EasyMock.createNiceMock(TransactionEvent.class);
        InsertElementType insert = EasyMock.createNiceMock(InsertElementType.class);
        EasyMock.expect(event.getSource()).andStubReturn(insert);
        EasyMock.expect(event.getLayerName()).andStubReturn(featureTypeQName1);
        EasyMock.expect(event.getType()).andStubReturn(TransactionEventType.PRE_INSERT);
        
        EasyMock.expect(catalog.getFeatureTypeByName(featureTypeName1)).andReturn(null).once();
        
        EasyMock.replay(catalog, featureType1, featureType2, event, insert);
        
        listener.dataStoreChange(event);
        
        EasyMock.verify(catalog, featureType1, featureType2, event, insert);
        
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testDataStoreChangeInsert() {
        
        Map<Object, Object> extendedProperties = new HashMap<Object, Object>();
        ReferencedEnvelope affectedBounds = new ReferencedEnvelope(-180, -90, 45, 90, WGS84);
        ReferencedEnvelope oldBounds = new ReferencedEnvelope(-90, 0, 0, 45, WGS84);
        
        EasyMock.expect(catalog.getFeatureTypeByName(featureTypeName1)).andStubReturn(featureType1);
        EasyMock.expect(featureType1.getNativeBoundingBox()).andStubReturn(oldBounds);
        
        EasyMock.replay(catalog, featureType1, featureType2);
        issueInsert(extendedProperties, affectedBounds);
        
        assertThat(extendedProperties, 
                (Matcher)hasEntry(
                        equalTo(BoundsUpdateTransactionListener.TRANSACTION_INFO_PLACEHOLDER),
                        allOf(
                                hasEntry(equalTo(featureTypeName1),
                                    contains(affectedBounds))
                                )));
        
        EasyMock.verify(catalog, featureType1, featureType2);
        
    }
    
    @Test
    public void testAfterTransactionCompoundCRS() throws Exception {
        Map<Object, Object> extendedProperties = new HashMap<Object, Object>();
        final CoordinateReferenceSystem compoundCrs = CRS.decode("EPSG:7415");
        final CoordinateReferenceSystem nativeCrs = CRS.decode("EPSG:28992");
        ReferencedEnvelope3D transactionBounds = new ReferencedEnvelope3D(142892, 142900, 470783, 470790, 16, 20, compoundCrs);
        ReferencedEnvelope oldBounds = new ReferencedEnvelope(142950, 143000, 470790, 470800, nativeCrs);
        ReferencedEnvelope newBounds = new ReferencedEnvelope(142892, 143000, 470783, 470800, nativeCrs);
        
        EasyMock.expect(catalog.getFeatureTypeByName(featureTypeName1)).andStubReturn(featureType1);
        EasyMock.expect(featureType1.getNativeBoundingBox()).andStubReturn(oldBounds);
        
        // Verify the FT is updated
        featureType1.setNativeBoundingBox(EasyMock.eq(newBounds));EasyMock.expectLastCall().once(); 
        catalog.save(featureType1);EasyMock.expectLastCall().once();
        
        EasyMock.replay(catalog, featureType1, featureType2);
        
        issueInsert(extendedProperties, transactionBounds);
        
        TransactionType request = EasyMock.createNiceMock(TransactionType.class);
        TransactionResponseType result = EasyMock.createNiceMock(TransactionResponseType.class);
        EasyMock.expect(request.getExtendedProperties()).andStubReturn(extendedProperties);
        
        EasyMock.replay(request, result);
        
        listener.afterTransaction(request, result, true);
        
        EasyMock.verify(catalog, featureType1, featureType2, request, result);
    }
    
    @Test
    public void testAfterTransaction() throws Exception {
        Map<Object, Object> extendedProperties = new HashMap<Object, Object>();
        ReferencedEnvelope affectedBounds1 = new ReferencedEnvelope(-180, 0, 0, 90, WGS84);
        ReferencedEnvelope affectedBounds2 = new ReferencedEnvelope(0, 180, 0, 90, WGS84);
        ReferencedEnvelope oldBounds = new ReferencedEnvelope(-90, 0, 0, 45, WGS84);
        ReferencedEnvelope newBounds = new ReferencedEnvelope(oldBounds);
        newBounds.expandToInclude(affectedBounds1);
        newBounds.expandToInclude(affectedBounds2);
        
        EasyMock.expect(catalog.getFeatureTypeByName(featureTypeName1)).andStubReturn(featureType1);
        EasyMock.expect(featureType1.getNativeBoundingBox()).andStubReturn(oldBounds);
        
        // Verify the FT is updated
        featureType1.setNativeBoundingBox(EasyMock.eq(newBounds));EasyMock.expectLastCall().once(); 
        catalog.save(featureType1);EasyMock.expectLastCall().once();
        
        EasyMock.replay(catalog, featureType1, featureType2);
        
        issueInsert(extendedProperties, affectedBounds1);
        
        issueInsert(extendedProperties, affectedBounds2);
        
        TransactionType request = EasyMock.createNiceMock(TransactionType.class);
        TransactionResponseType result = EasyMock.createNiceMock(TransactionResponseType.class);
        EasyMock.expect(request.getExtendedProperties()).andReturn(extendedProperties);
        EasyMock.replay(request, result);
        
        listener.afterTransaction(request, result, true);
        
        ReferencedEnvelope expectedEnv = new ReferencedEnvelope(affectedBounds1);
        expectedEnv.expandToInclude(affectedBounds2);
        
        EasyMock.verify(catalog, featureType1, featureType2, request, result);
        
    }
    
    /**
     * Issues a fake dataStoreChange insert event that affects two tile layers: "theLayer" and
     * "theGroup"
     */
    private void issueInsert(Map<Object, Object> extendedProperties,
            ReferencedEnvelope affectedBounds) {
        
        TransactionType transaction = EasyMock.createNiceMock(TransactionType.class);
        EasyMock.expect(transaction.getExtendedProperties()).andStubReturn(extendedProperties);
        
        TransactionEvent event = EasyMock.createNiceMock(TransactionEvent.class);
        
        EasyMock.expect(event.getRequest()).andStubReturn(transaction);
        
        EasyMock.expect(event.getLayerName()).andStubReturn(featureTypeQName1);
        
        InsertElementType insert = EasyMock.createNiceMock(InsertElementType.class);
        
        EasyMock.expect(event.getSource()).andStubReturn(insert);
        EasyMock.expect(event.getType()).andStubReturn(TransactionEventType.PRE_INSERT);
        
        SimpleFeatureCollection affectedFeatures = EasyMock.createNiceMock(SimpleFeatureCollection.class);
        EasyMock.expect(affectedFeatures.getBounds()).andStubReturn(affectedBounds);
        EasyMock.expect(event.getAffectedFeatures()).andStubReturn(affectedFeatures);
        EasyMock.replay(transaction, event, insert, affectedFeatures);
        listener.dataStoreChange(event);
    }
}
