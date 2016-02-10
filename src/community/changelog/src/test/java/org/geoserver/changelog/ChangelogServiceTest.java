package org.geoserver.changelog;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.data.test.CiteTestData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.data.DataStore;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureReader;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.referencing.CRS;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.mockrunner.mock.web.MockHttpServletRequest;
import com.mockrunner.mock.web.MockHttpServletResponse;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

public class ChangelogServiceTest extends GeoServerSystemTestSupport {
    private static final String TEST_GROUP = "testGroup";


    @Rule
    public TemporaryFolder temp = new TemporaryFolder();
    
    
    DataStoreInfo dsi;
    
    @Before
    public void addLogStore() throws Exception {
        dsi = getCatalog().getFactory().createDataStore();
        dsi.setWorkspace(getCatalog().getWorkspaceByName("georss"));
        dsi.setName("georss");
        dsi.setType("h2");
        Map<String, String> parameters = new HashMap<>();
        parameters.put("dbtype", "h2");
        parameters.put("database", temp.newFile().toString());
        dsi.getConnectionParameters().putAll(parameters);
        
        getCatalog().add(dsi);
    }
    
    @After
    public void removeLogStore() throws Exception {
        // Need to delete contents of store first
        for(ResourceInfo ri : getCatalog().getResourcesByStore(dsi, ResourceInfo.class)) {
            for(LayerInfo li : getCatalog().getLayers(ri)) {
                // Shouldn't be any layer groups to worry about
                getCatalog().remove(li);
            }
            getCatalog().remove(ri);
        }
        getCatalog().remove(dsi);
    }
    
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        testData.addWorkspace("georss", "http://localhost/georss", this.getCatalog());
        
        try {
        LayerGroupInfo lgi = getCatalog().getFactory().createLayerGroup();
        lgi.setName(TEST_GROUP);
        lgi.setWorkspace(getCatalog().getWorkspaceByName(CiteTestData.CITE_PREFIX));
        Arrays.asList(CiteTestData.BUILDINGS, CiteTestData.BRIDGES).stream()
            .map(this::qname)
            .map(getCatalog()::getLayerByName)
            .peek(lgi.getLayers()::add)
            .map(LayerInfo::getDefaultStyle)
            .forEach(lgi.getStyles()::add);
        getCatalog().add(lgi);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private String qname(QName q) {
        return q.getPrefix()+":"+q.getLocalPart();
    }
    
    @Test
    public void testInit() throws Exception {
        
        ChangelogService service = new ChangelogService();
        
        service.initialize(getGeoServer());
    }
    
    @Test
    public void testFilterForName() throws Exception {
        final String testLayerFullName = qname(CiteTestData.BUILDINGS);
        final String testLayerShortName = CiteTestData.BUILDINGS.getLocalPart();
        
        ChangelogService service = new ChangelogService();
        service.initialize(getGeoServer());
        
        this.getTestData();
        Filter f = service.filterForName(testLayerFullName);
        try(CloseableIterator<LayerInfo> it = getCatalog().list(LayerInfo.class, f);){
            assertThat(it.hasNext(), is(true));
            assertThat(it.next(), Matchers.hasProperty("name", equalTo(testLayerShortName)));
            assertThat(it.hasNext(), is(false));
        }
    }
    @Test
    public void testFilterForNameGroup() throws Exception {
        final String testLayerFullName = CiteTestData.CITE_PREFIX+":"+TEST_GROUP;
        final String testLayerShortName = TEST_GROUP;
        
        ChangelogService service = new ChangelogService();
        service.initialize(getGeoServer());
        
        Filter f = service.filterForName(testLayerFullName);
        try(CloseableIterator<LayerGroupInfo> it = getCatalog().list(LayerGroupInfo.class, f);){
            assertThat(it.hasNext(), is(true));
            assertThat(it.next(), Matchers.hasProperty("name", equalTo(testLayerShortName)));
            assertThat(it.hasNext(), is(false));
        }
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    Matcher<LayerInfo> layer(String workspace, String name) {
        return (Matcher) hasItem(
                both(hasProperty("name", is(name)))
                .and(hasProperty("resource", hasProperty("store", hasProperty("workspace", hasProperty("name", is(workspace)))))));
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testCreateLog() throws Exception {
        final String testLayerFullName = qname(CiteTestData.BUILDINGS);
        
        ChangelogService service = new ChangelogService();
        service.initialize(getGeoServer());
        
        MockHttpServletResponse response = 
                mockRequest("ows?service=changelog&request=createLog&layer="+testLayerFullName, 
                        service::createLog);
        
        DataStore ds = (DataStore) dsi.getDataStore(null);
        
        // It shows up in the datastore
        String expectedName = getCatalog().getLayerByName(testLayerFullName).getId().replace(":", "-");
        assertThat(ds.getNames(), hasItem(hasProperty("localPart", is(expectedName))));
        
        // Retrieve it and check that the schema looks right 
        FeatureType ft = ds.getSchema(expectedName);
        Matcher<CoordinateReferenceSystem> crsMatcher = equalTo(CRS.decode("EPSG:4326"));
        Matcher<GeometryDescriptor> geomPropMatcher = 
            allOf(
                hasProperty("name", hasProperty("localPart", equalTo("geom_the_geom"))),
                hasProperty("type", hasProperty("binding", equalTo(MultiPolygon.class))),
                hasProperty("coordinateReferenceSystem", equalTo(CRS.decode("EPSG:4326")))
                );
        assertThat(ft.getCoordinateReferenceSystem(), crsMatcher);
        assertThat(ft.getGeometryDescriptor(), geomPropMatcher);
        assertThat(ft.getDescriptors(), containsInAnyOrder(
                allOf(
                        hasProperty("name", hasProperty("localPart", equalTo("guid"))),
                        hasProperty("type", hasProperty("binding", equalTo(String.class)))
                        ),
                allOf(
                        hasProperty("name", hasProperty("localPart", equalTo("time"))),
                        hasProperty("type", hasProperty("binding", equalTo(Timestamp.class)))
                        ),
                (Matcher)geomPropMatcher
                ));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testCreateLogGroup() throws Exception {
        
        final String testLayerFullName = CiteTestData.CITE_PREFIX+":"+TEST_GROUP;
        @SuppressWarnings("rawtypes")
        final List expectedNames = (List)Arrays.asList(CiteTestData.BUILDINGS, CiteTestData.BRIDGES).stream()
            .map(this::qname)
            .map(getCatalog()::getLayerByName)
            .map(LayerInfo::getId)
            .map(id->hasItem(hasProperty("localPart", is(id.replace(":", "-")))))
            .collect(Collectors.toList());
        
        ChangelogService service = new ChangelogService();
        service.initialize(getGeoServer());
        
        MockHttpServletResponse response = 
                mockRequest("ows?service=changelog&request=createLog&layer="+testLayerFullName, 
                        service::createLog);
        
        DataStore ds = (DataStore) dsi.getDataStore(null);
        
        // Check that logs were created for the layers in the group
        assertThat(ds.getNames(), Matchers.allOf(expectedNames));

    }
    
    @FunctionalInterface
    interface RequestHandler {
        void handle(HttpServletRequest request, HttpServletResponse response)
                throws ServletException, IOException;
    }
    
    MockHttpServletResponse mockRequest(String path, RequestHandler test)
            throws ServletException, IOException {
        MockHttpServletRequest request = createRequest(path);
        MockHttpServletResponse response = new MockHttpServletResponse();
        test.handle(request, response);
        return response;
    }
    
    @Test
    public void testGetChanges() throws Exception {
        final String testLayerFullName = qname(CiteTestData.BUILDINGS);
        
        ChangelogService service = new ChangelogService();
        service.initialize(getGeoServer());
        
        // First create the log
        mockRequest("ows?service=changelog&request=createLog&layer="+testLayerFullName, 
                service::createLog);
        
        GeometryFactory fact = new GeometryFactory();
        
        DataStore ds = (DataStore) dsi.getDataStore(null);
        try (Transaction t = new DefaultTransaction();
             FeatureWriter<SimpleFeatureType, SimpleFeature> fw = ds.getFeatureWriterAppend(getCatalog().getLayerByName(testLayerFullName).getId().replace(':', '-'), t);){
            SimpleFeature f = fw.next();
            f.setAttribute("guid", UUID.randomUUID().toString());
            f.setAttribute("time", Timestamp.from(Instant.now().minusSeconds(5)));
            f.setDefaultGeometry(fact.createMultiPolygon(new Polygon[]{fact.createPolygon(new Coordinate[]{new Coordinate(1, 1),new Coordinate(1, 2),new Coordinate(2, 1), new Coordinate(1, 1)})}));
            fw.write();
            f.setAttribute("guid", UUID.randomUUID().toString());
            f.setAttribute("time", Timestamp.from(Instant.now()));
            f.setDefaultGeometry(fact.createMultiPolygon(new Polygon[]{fact.createPolygon(new Coordinate[]{new Coordinate(11, 11),new Coordinate(11, 12),new Coordinate(12, 11), new Coordinate(11, 11)})}));
            fw.write();
            t.commit();
        }
        
        MockHttpServletResponse response = 
                mockRequest("ows?service=changelog&request=GetChanges&layers="+testLayerFullName, 
                        service::getChanges);
        
        assertThat(response.getErrorCode(), is(200));
        
        System.out.println(response.getOutputStreamContent());
        
        response.getOutputStreamContent();
    }

}
