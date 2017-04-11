/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;
import static org.geotools.referencing.crs.DefaultGeographicCRS.WGS84;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
        EasyMock.expect(event.getSource()).andReturn(new Object()).once();
        EasyMock.expect(event.getLayerName()).once();
        EasyMock.expect(event.getType()).once();
        
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
    public void testDataStoreChangeDoesNotAffectTileLayer() {
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

    @Test
    public void testDataStoreChangeInsert() {

        Map<Object, Object> extendedProperties = new HashMap<Object, Object>();
        ReferencedEnvelope affectedBounds = new ReferencedEnvelope(-180, 0, 0, 90, WGS84);

        issueInsert(extendedProperties, affectedBounds);

        assertTrue(extendedProperties
                .containsKey(GWCTransactionListener.GWC_TRANSACTION_INFO_PLACEHOLDER));

        @SuppressWarnings("unchecked")
        Map<String, List<ReferencedEnvelope>> placeHolder = (Map<String, List<ReferencedEnvelope>>) extendedProperties
                .get(GWCTransactionListener.GWC_TRANSACTION_INFO_PLACEHOLDER);

        assertNotNull(placeHolder.get("theLayer"));

        assertSame(affectedBounds, placeHolder.get("theLayer").get(0));
        assertSame(affectedBounds, placeHolder.get("theGroup").get(0));
    }

    @Test
    public void testAfterTransactionCompoundCRS() throws Exception {
        Map<Object, Object> extendedProperties = new HashMap<Object, Object>();
        final CoordinateReferenceSystem compoundCrs = CRS.decode("EPSG:7415");
        ReferencedEnvelope3D transactionBounds = new ReferencedEnvelope3D(142892, 470783, 142900, 470790, 16, 20, compoundCrs);

        issueInsert(extendedProperties, transactionBounds);

        TransactionType request = mock(TransactionType.class);
        TransactionResponseType result = mock(TransactionResponseType.class);
        when(request.getExtendedProperties()).thenReturn(extendedProperties);

        /*when(mediator.getDeclaredCrs(anyString())).thenReturn(compoundCrs);*/
        listener.afterTransaction(request, result, true);
        
        ReferencedEnvelope expectedBounds = new ReferencedEnvelope(transactionBounds, CRS.getHorizontalCRS(compoundCrs));

        /*verify(mediator, times(1)).truncate(eq("theLayer"), eq(expectedBounds));
        verify(mediator, times(1)).truncate(eq("theGroup"), eq(expectedBounds));*/
    }
    
    @Test
    public void testAfterTransaction() throws Exception {
        Map<Object, Object> extendedProperties = new HashMap<Object, Object>();
        ReferencedEnvelope affectedBounds1 = new ReferencedEnvelope(-180, 0, 0, 90, WGS84);
        ReferencedEnvelope affectedBounds2 = new ReferencedEnvelope(0, 180, 0, 90, WGS84);

        issueInsert(extendedProperties, affectedBounds1);

        issueInsert(extendedProperties, affectedBounds2);

        TransactionType request = mock(TransactionType.class);
        TransactionResponseType result = mock(TransactionResponseType.class);
        when(request.getExtendedProperties()).thenReturn(extendedProperties);

        /*when(mediator.getDeclaredCrs(anyString())).thenReturn(WGS84);*/
        listener.afterTransaction(request, result, true);

        ReferencedEnvelope expectedEnv = new ReferencedEnvelope(affectedBounds1);
        expectedEnv.expandToInclude(affectedBounds2);

        /*verify(mediator, times(1)).truncate(eq("theLayer"), eq(expectedEnv));
        verify(mediator, times(1)).truncate(eq("theGroup"), eq(expectedEnv));*/

    }

    /**
     * Issues a fake dataStoreChange insert event that affects two tile layers: "theLayer" and
     * "theGroup"
     */
    private void issueInsert(Map<Object, Object> extendedProperties,
            ReferencedEnvelope affectedBounds) {

        TransactionType transaction = mock(TransactionType.class);
        when(transaction.getExtendedProperties()).thenReturn(extendedProperties);

        TransactionEvent event = mock(TransactionEvent.class);

        when(event.getRequest()).thenReturn(transaction);

        QName layerName = new QName("testType");
        when(event.getLayerName()).thenReturn(layerName);

        InsertElementType insert = mock(InsertElementType.class);

        when(event.getSource()).thenReturn(insert);
        when(event.getType()).thenReturn(TransactionEventType.PRE_INSERT);

        /*when(
                mediator.getTileLayersByFeatureType(eq(layerName.getNamespaceURI()),
                        eq(layerName.getLocalPart()))).thenReturn(

        ImmutableSet.of("theLayer", "theGroup"));*/

        SimpleFeatureCollection affectedFeatures = mock(SimpleFeatureCollection.class);
        when(affectedFeatures.getBounds()).thenReturn(affectedBounds);
        when(event.getAffectedFeatures()).thenReturn(affectedFeatures);

        listener.dataStoreChange(event);
    }
}
