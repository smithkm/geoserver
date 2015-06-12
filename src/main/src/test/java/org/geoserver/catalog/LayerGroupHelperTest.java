package org.geoserver.catalog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import javax.xml.namespace.QName;

import org.geoserver.catalog.LayerGroupInfo.Mode;
import org.geoserver.catalog.impl.LayerGroupInfoImpl;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.MockTestData;
import org.geoserver.test.GeoServerMockTestSupport;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class LayerGroupHelperTest extends GeoServerMockTestSupport {

    private LayerGroupInfo lakesNeatline;

    private LayerGroupInfo ponds;

    private LayerGroupInfo nested;

    private LayerGroupInfo loop1;
    
    private LayerGroupInfo loop2;
    private LayerGroupInfo loop2Child;
    
    private LayerGroupInfo container;
    private LayerGroupInfo containerParent;
    
    private LayerInfo lakesLayer;

    private LayerInfo neatlineLayer;

    private LayerInfo pondsLayer;

    private LayerInfo forestLayer;

    private LayerInfo buildingsLayer;

    private LayerInfo roadSegmentsLayer;

    private StyleInfo lineStyle;

    private StyleInfo polygonStyle;

    private StyleInfo pointStyle;

    private StyleInfo pondsStyle;

    @Override
    protected MockTestData createTestData() throws Exception {
        MockTestData testData = new MockTestData();
        testData.setIncludeRaster(true);
        return testData;
    }

    private LayerGroupInfo buildGroup(String name, PublishedInfo... publisheds) {
        LayerGroupInfoImpl group = (LayerGroupInfoImpl) getCatalog().getFactory().createLayerGroup();
        group.setId(name);
        group.setName(name);
        group.getLayers().addAll(Arrays.asList(publisheds));

        return group;
    }

    private LayerInfo buildLayer(QName resourceName) throws Exception {
        Catalog cat = getCatalog();
        CatalogBuilder cb = new CatalogBuilder(cat);
        StoreInfo store = cat.getDataStoreByName(resourceName.getPrefix());
        cb.setStore(store);
        ResourceInfo ri = cb.buildFeatureType(toName(resourceName));
        ri.setSRS("EPSG:4326");
        cb.setupBounds(ri);

        LayerInfo layer = cat.getFactory().createLayer();
        layer.setResource(ri);
        layer.setName(ri.getName());
        layer.setEnabled(true);
        layer.setType(PublishedType.VECTOR);

        return layer;
    }

    @Before
    public void buildTestNestedGroup() throws Exception {
        lineStyle = getCatalog().getStyleByName("Streams");
        polygonStyle = getCatalog().getStyleByName("Buildings");
        pointStyle = getCatalog().getStyleByName("Bridges");
        pondsStyle = getCatalog().getStyleByName("Ponds");

        lakesLayer = buildLayer(MockData.LAKES);
        neatlineLayer = buildLayer(MockData.MAP_NEATLINE);
        lakesNeatline = buildGroup("lakesNeatline", lakesLayer, neatlineLayer);
        lakesNeatline.setMode(Mode.EO);
        roadSegmentsLayer = buildLayer(MockData.ROAD_SEGMENTS);
        lakesNeatline.setRootLayer(roadSegmentsLayer);
        lakesNeatline.setRootLayerStyle(lineStyle);
        lakesNeatline.getStyles().add(polygonStyle);
        lakesNeatline.getStyles().add(pointStyle);

        pondsLayer = buildLayer(MockData.PONDS);
        ponds = buildGroup("ponds", pondsLayer);
        ponds.getStyles().add(null);
        ponds.getScaleRanges().add(ScaleRange.INCLUDE);

        forestLayer = buildLayer(MockData.FORESTS);
        buildingsLayer = buildLayer(MockData.BUILDINGS);
        nested = buildGroup("nested", forestLayer, lakesNeatline, buildingsLayer, ponds);
        nested.getStyles().add(polygonStyle);
        nested.getStyles().add(null);
        nested.getStyles().add(polygonStyle);
        nested.getStyles().add(null);
        
        loop1 = buildGroup("loop1", forestLayer);
        loop1.getLayers().add(loop1);
        
        loop2 = buildGroup("loop2", forestLayer);
        loop2Child = buildGroup("ponds", pondsLayer, loop2);
        loop2.getLayers().add(loop2Child);
        
        container = buildGroup("container", forestLayer);
        container.getStyles().add(polygonStyle);
        container.setMode(LayerGroupInfo.Mode.CONTAINER);
        containerParent = buildGroup("containerParent", container);
        containerParent.getStyles().add(null);
    }

    @Test
    public void testSimpleLoop() {
        Assert.assertNull(new LayerGroupHelper(nested).checkLoops());
        
        LayerGroupHelper helper = new LayerGroupHelper(loop1);
        Stack<LayerGroupInfo> path = helper.checkLoops();
        Assert.assertNotNull(path);
        Assert.assertEquals("/loop1/loop1", helper.getLoopAsString(path));
        
        helper = new LayerGroupHelper(loop2);
        path = helper.checkLoops();
        Assert.assertNotNull(path);
        Assert.assertEquals("/loop2/ponds/loop2", helper.getLoopAsString(path));        
    }
    
    @Test
    public void testSimpleLoopWithNotEqualGroups() {
        LayerGroupInfo myLoop = buildGroup("myLoop", forestLayer);
         LayerGroupInfo loopClone = buildGroup("myLoop", forestLayer);
         assertTrue(myLoop.equals(loopClone));
            
         // change the LayerGroup
         myLoop.setTitle("new title");
         // now the two groups aren't equal anymore
         assertFalse(myLoop.equals(loopClone));
            
         // create loop
         myLoop.getLayers().add(loopClone);

         // validate
         LayerGroupHelper helper = new LayerGroupHelper(myLoop);
         Stack<LayerGroupInfo> path = helper.checkLoops();
         Assert.assertNotNull(path);
         Assert.assertEquals("/myLoop/myLoop", helper.getLoopAsString(path));
    }
    
    @Test
    public void testAllLayers() {
        // a plain group
        assertExpectedLayers(Arrays.asList(pondsLayer), ponds);
        // a EO group
        assertExpectedLayers(Arrays.asList(roadSegmentsLayer, lakesLayer, neatlineLayer),
                lakesNeatline);
        // a nested one
        assertExpectedLayers(Arrays.asList(forestLayer, roadSegmentsLayer, lakesLayer,
                neatlineLayer, buildingsLayer, pondsLayer), nested);
        // a container group
        assertExpectedLayers(Arrays.asList(forestLayer), container);
        // a nested container
        assertExpectedLayers(Arrays.asList(forestLayer), containerParent);        
    }

    private void assertExpectedLayers(List<LayerInfo> expected, LayerGroupInfo group) {
        List<LayerInfo> layers = new LayerGroupHelper(group).allLayers();
        assertEquals(expected, layers);
    }

    @Test
    public void testAllLayersForRendering() {
        // a plain group
        assertExpectedRenderingLayers(Arrays.asList(pondsLayer), ponds);
        // a EO group
        assertExpectedRenderingLayers(Arrays.asList(roadSegmentsLayer), lakesNeatline);
        // a nested one
        assertExpectedRenderingLayers(
                Arrays.asList(forestLayer, roadSegmentsLayer, buildingsLayer, pondsLayer), nested);
        
        try {
            // a container group
            assertExpectedRenderingLayers(Arrays.asList(forestLayer), container);
            fail("Unsupported Operation...");
        } catch (UnsupportedOperationException e) {
        }
        
        // a nested container
        assertExpectedRenderingLayers(Arrays.asList(forestLayer), containerParent);                
    }

    private void assertExpectedRenderingLayers(List<LayerInfo> expected, LayerGroupInfo group) {
        List<LayerInfo> layers = new LayerGroupHelper(group).allLayersForRendering();
        assertEquals(expected, layers);
    }

    @Test
    public void testAllStyles() {
        // plain group
        assertExpectedStyles(Arrays.asList((StyleInfo) null), ponds);
        // EO group
        assertExpectedStyles(Arrays.asList(lineStyle, polygonStyle, pointStyle), lakesNeatline);
        // nested group
        assertExpectedStyles(Arrays.asList(polygonStyle, lineStyle, polygonStyle, pointStyle, polygonStyle, null), nested);
        // a container group
        assertExpectedStyles(Arrays.asList(polygonStyle), container);
        // a nested container
        assertExpectedStyles(Arrays.asList(polygonStyle), containerParent);            
    }

    private void assertExpectedStyles(List<StyleInfo> expected, LayerGroupInfo group) {
        List<StyleInfo> styles = new LayerGroupHelper(group).allStyles();
        assertEquals(expected, styles);
    }
    
    @Test
    public void testAllStylesForRendering() {
        // plain group
        assertExpectedRenderingStyles(Arrays.asList((StyleInfo) null), ponds);
        // EO group
        assertExpectedRenderingStyles(Arrays.asList(lineStyle), lakesNeatline);
        // nested group
        assertExpectedRenderingStyles(Arrays.asList(polygonStyle, lineStyle, polygonStyle, null), nested);
        
        try {
            // a container group
            assertExpectedRenderingStyles(Arrays.asList(polygonStyle), container);
            fail("Unsupported Operation...");
        } catch (UnsupportedOperationException e) {
        }        
        
        // a nested container
        assertExpectedRenderingStyles(Arrays.asList(polygonStyle), containerParent);                    
    }

    private void assertExpectedRenderingStyles(List<StyleInfo> expected, LayerGroupInfo group) {
        List<StyleInfo> styles = new LayerGroupHelper(group).allStylesForRendering();
        assertEquals(expected, styles);
    }
    
    @Test
    public void testAllScaleRangesForRendering() {
        // plain group
        assertExpectedRenderingScaleRanges(Arrays.asList(ScaleRange.INCLUDE), ponds);
        // EO group
        assertExpectedRenderingScaleRanges(Arrays.asList(ScaleRange.INCLUDE), lakesNeatline);
        // nested group
        assertExpectedRenderingScaleRanges(
                Arrays.asList(
                        ScaleRange.INCLUDE,ScaleRange.INCLUDE,ScaleRange.INCLUDE,ScaleRange.INCLUDE
                    ), nested);
        
        try {
            // a container group
            assertExpectedRenderingScaleRanges(Arrays.asList(ScaleRange.INCLUDE), containerParent);
            fail("Unsupported Operation...");
        } catch (UnsupportedOperationException e) {
        }        
        
        // a nested container
        assertExpectedRenderingScaleRanges(Arrays.asList(ScaleRange.INCLUDE), containerParent);
    }
    private void assertExpectedRenderingScaleRanges(List<ScaleRange> expected, LayerGroupInfo group) {
        List<ScaleRange> styles = new LayerGroupHelper(group).allScaleRangesForRendering();
        assertEquals(expected, styles);
    }
    @Test
    public void testBounds() throws Exception {
        // plain group
        new LayerGroupHelper(ponds).calculateBounds();
        assertEquals(ponds.getBounds(), ponds.getBounds());
        // EO group
        ReferencedEnvelope eoExpected = aggregateEnvelopes(roadSegmentsLayer, lakesLayer,
                neatlineLayer);
        new LayerGroupHelper(lakesNeatline).calculateBounds();
        ReferencedEnvelope eoActual = lakesNeatline.getBounds();
        assertEquals(eoExpected, eoActual);
        // nested group
        ReferencedEnvelope nestedExpected = aggregateEnvelopes(forestLayer, roadSegmentsLayer,
                lakesLayer, neatlineLayer, buildingsLayer, pondsLayer);
        new LayerGroupHelper(nested).calculateBounds();
        assertEquals(nestedExpected, nested.getBounds());
    }

    @Test
    public void testBoundsCRS() throws Exception {
        CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:32628", true);
        ReferencedEnvelope nestedExpected = aggregateEnvelopes(forestLayer, roadSegmentsLayer,
                lakesLayer, neatlineLayer, buildingsLayer, pondsLayer);
        nestedExpected = nestedExpected.transform(targetCRS, true);
        new LayerGroupHelper(nested).calculateBounds(targetCRS);
        assertEquals(nestedExpected, nested.getBounds());
    }

    private ReferencedEnvelope aggregateEnvelopes(LayerInfo... layers) {
        ReferencedEnvelope eoExpected = new ReferencedEnvelope(layers[0].getResource()
                .getNativeBoundingBox(), layers[0].getResource().getCRS());
        for (int i = 1; i < layers.length; i++) {
            eoExpected.expandToInclude(layers[i].getResource().getNativeBoundingBox());
        }

        return eoExpected;
    }

}
