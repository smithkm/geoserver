package org.geoserver.changelog;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInitializer;
import org.geotools.data.DataStore;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.Transaction;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.NameImpl;
import org.geotools.feature.TypeBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.feature.type.FeatureTypeFactoryImpl;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.util.SimpleInternationalString;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.FeatureTypeFactory;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.InternationalString;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import com.sun.jmx.snmp.Timestamp;
import com.vividsolutions.jts.geom.Geometry;

public class ChangelogService implements GeoServerInitializer {
    
    GeoServer gs;
    
    String workspace = "georss";
    String datastore = "georss";
    
    FilterFactory2 ff;
    FeatureTypeFactory ftf;
    
    DataStoreInfo getDataStore () {
        final DataStoreInfo ds = gs.getCatalog().getDataStoreByName(workspace, datastore);
        if(ds==null) {
            throw new IllegalStateException("Unknown datastore "+workspace+":"+datastore);
        }
        return ds;
    }
    
    PublishedInfo findPublished(String name, DataStoreInfo dsi){
        throw new NotImplementedException();
    }
    
    GeometryDescriptor loggedGeomProperty(GeometryDescriptor desc) {
        Name name = new NameImpl("geom_"+desc.getName().getLocalPart());
        return ftf.createGeometryDescriptor(desc.getType(), name, desc.getMinOccurs(), desc.getMaxOccurs(), desc.isNillable(), null);
    }
    
    private void createLog(LayerInfo info, DataStore ds) throws IOException{
        String id=info.getId();
        ResourceInfo ri = info.getResource();
        CoordinateReferenceSystem crs = ri.getCRS();
        
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder(ftf);
        builder.setName(id);
        builder.add("guid", UUID.class);
        builder.add("time", Date.class);
        
        GeometryDescriptor logGeom;
        if(ri instanceof FeatureTypeInfo) {
            FeatureType ft = ((FeatureTypeInfo)ri).getFeatureType();
            GeometryDescriptor gd = ft.getGeometryDescriptor();
            logGeom = loggedGeomProperty(gd);
            builder.add(logGeom);
        } else  {
            throw new IllegalArgumentException("Can only log vector layers"); // TODO Need to be more flexible here.  A layer group may contain other kinds of layer.
        }
        
        SimpleFeatureType sft = builder.buildFeatureType();
        //SimpleFeatureType sft = ftf.createSimpleFeatureType(new NameImpl(id), schema, logGeom, false, Collections.emptyList(), (AttributeType)null, new SimpleInternationalString("Change log for layer "+info.getName()));
        ds.createSchema(sft);
    }
    
    Filter filterForName(String name) {
        int i = name.indexOf(':');
        if(i<0) {
            return ff.equals(ff.property("name"), ff.literal(name));
        } else {
            return ff.and(
                    ff.or(
                            ff.equals(ff.property("workspace.name"), ff.literal(name.substring(0, i))), // for a layer group
                            ff.equals(ff.property("resource.store.workspace.name"), ff.literal(name.substring(0, i)))), // for a layer
                    ff.equals(ff.property("name"), ff.literal(name.substring(i+1))));
        }
        
    }
    
    public void createLog(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final String layerName;
        {
            String[] names = request.getParameterValues("layer");
            if(names.length!=1) {
                throw new IllegalArgumentException();
            }
            layerName=names[0];
        }
        
        DataStoreInfo dsi = getDataStore();
        DataStore ds = (DataStore) dsi.getDataStore(null); // TODO handle this more gracefully when it's not a DataStore
        
        // Using Catalog.list lets us grab a layer or layer group as appropriate with a single query.
        try (CloseableIterator<PublishedInfo> it = gs.getCatalog().list(PublishedInfo.class, filterForName(layerName));){
            while(it.hasNext()) {
                PublishedInfo info = it.next();
                if(info instanceof LayerInfo) {
                    createLog((LayerInfo) info, ds);
                } else if(info instanceof LayerGroupInfo) {
                    // we need to use the layers method that flattens the layer group to actual layers rather than getLayers
                    for(LayerInfo layerInfo:((LayerGroupInfo)info).layers()){
                        createLog(layerInfo, ds); 
                    }
                } else {
                    throw new IllegalStateException("Unknown class of PublishableInfo: "+info.getClass().toString());
                }
            }
        } finally {
            //ds.dispose(); // TODO use the try with resources once DataStore implements AutoClosable
        }
        
    }
    
    public void getChanges(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        List<String> layers = Arrays.asList(request.getParameterValues("layers"));
        
        DataStoreInfo dsi = gs.getCatalog().getDataStoreByName(workspace, datastore);
        Name logName = new NameImpl("changelog");
        final JDBCDataStore dataStore = (JDBCDataStore) dsi.getDataStore(null);
        FeatureStore<?,SimpleFeature> fs = (FeatureStore<?, SimpleFeature>) dataStore.getFeatureSource(logName);
        Filter filter=null;// FIXME
        try(
                FeatureIterator<SimpleFeature> fi = fs.getFeatures(filter).features();
                OutputStream out = response.getOutputStream();
            ) {
            while (fi.hasNext()) {
                SimpleFeature f = fi.next();
                Geometry o = (Geometry) f.getDefaultGeometry();
                CoordinateReferenceSystem crs;

                out.write(o.toString().getBytes());
                out.write("\n".getBytes());

                out.write("\n".getBytes());
                out.write("\n".getBytes());
            }
        }
    }
    
    @Override
    public void initialize(GeoServer geoServer) throws Exception {
        gs = geoServer;
        ff = CommonFactoryFinder.getFilterFactory2( GeoTools.getDefaultHints());
        ftf = new FeatureTypeFactoryImpl(); // Because CommonFactoryFinder.getFeatureTypeFactory doesn't work.
    }
    
}
