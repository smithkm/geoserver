package org.geoserver.changelog;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

import org.easymock.EasyMock;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.CiteTestData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.data.DataAccess;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;

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
    
    @Test
    public void testCreateLog() throws Exception {
        final String testLayerFullName = qname(CiteTestData.BUILDINGS);
        
        ChangelogService service = new ChangelogService();
        service.initialize(getGeoServer());
        
        HttpServletRequest request = createMock("request", HttpServletRequest.class);
        HttpServletResponse response = createMock("response", HttpServletResponse.class);
        
        expect(request.getParameterValues("layer")).andStubReturn(new String[]{testLayerFullName});
        
        replay(request, response);
        
        service.createLog(request, response);
        
        verify(request, response);
        
        DataStore ds = (DataStore) dsi.getDataStore(null);
        
        String expectedName = getCatalog().getLayerByName(testLayerFullName).getId();
        assertThat(ds.getNames(), hasItem(hasProperty("localPart", is(expectedName))));
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
            .map(id->hasItem(hasProperty("localPart", is(id))))
            .collect(Collectors.toList());
        
        ChangelogService service = new ChangelogService();
        service.initialize(getGeoServer());
        
        HttpServletRequest request = createMock("request", HttpServletRequest.class);
        HttpServletResponse response = createMock("response", HttpServletResponse.class);
        
        expect(request.getParameterValues("layer")).andStubReturn(new String[]{testLayerFullName});
        
        replay(request, response);
        
        service.createLog(request, response);
        
        verify(request, response);
        
        DataStore ds = (DataStore) dsi.getDataStore(null);
        
        ds.getNames().forEach(System.out::println);
        
        assertThat(ds.getNames(), Matchers.allOf(expectedNames));

    }
    
}
