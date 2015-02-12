/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import static org.junit.Assert.*;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.TemporaryResourceStore;
import org.geotools.data.DataUtilities;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class DefaultGeoServerLoaderTest {
    DefaultGeoServerLoader loader;
    
    Catalog catalog;
    XStreamPersister xp;
    
    @Rule
    public TemporaryResourceStore storeRule = TemporaryResourceStore.use(DataUtilities.urlToFile(
                DefaultGeoServerLoaderTest.class.getResource("/data_dir/nested_layer_groups")));
    
    @Before
    public void setUp() {
        GeoServerResourceLoader resourceLoader = new GeoServerResourceLoader(storeRule.getStore());
        GeoServerExtensionsHelper.singleton( "resourceLoader", resourceLoader);
        
        loader = new DefaultGeoServerLoader(resourceLoader);
        catalog = new CatalogImpl();
        catalog.setResourceLoader( resourceLoader );
        
        XStreamPersisterFactory xpf = new XStreamPersisterFactory();
        xp = xpf.createXMLPersister();
    }
    
    @After
    public void tearDown() {
        GeoServerExtensionsHelper.clear(); // clear singleton
    }
    
    @Test
    public void testGeneratedStyles() throws Exception {
        XStreamPersisterFactory xpf = new XStreamPersisterFactory();
        XStreamPersister xp = xpf.createXMLPersister();
        xp.setCatalog( catalog );
        loader.initializeStyles(catalog, xp);
        
        StyleInfo polygon = catalog.getStyleByName( StyleInfo.DEFAULT_POLYGON );
        assertEquals( "default_polygon.sld", polygon.getFilename() );
    }
    
    @Test
    public void testLoadNestedLayerGroups() throws Exception {
        GeoServerResourceLoader resources = GeoServerExtensions.bean(GeoServerResourceLoader.class );
        assertSame( catalog.getResourceLoader(), resources );
        loader.readCatalog(catalog, xp);
        assertNotNull(catalog.getLayerGroupByName("topp", "simplegroup"));
        LayerGroupInfo nestedLayerGroup = catalog.getLayerGroupByName("topp", "nestedgroup");
        assertNotNull(nestedLayerGroup);
        assertNotNull(nestedLayerGroup.getLayers());
        assertEquals(2, nestedLayerGroup.getLayers().size());
        assertTrue(nestedLayerGroup.getLayers().get(0) instanceof LayerGroupInfo);
        assertNotNull(((LayerGroupInfo)nestedLayerGroup.getLayers().get(0)).getLayers());
        assertTrue(nestedLayerGroup.getLayers().get(1) instanceof LayerInfo);
    }
}
