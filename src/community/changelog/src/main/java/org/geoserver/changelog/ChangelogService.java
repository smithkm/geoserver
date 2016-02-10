package org.geoserver.changelog;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
import org.geotools.data.FeatureReader;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.feature.type.FeatureTypeFactoryImpl;
import org.geotools.jdbc.JDBCDataStore;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.FeatureTypeFactory;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import com.google.common.collect.ForwardingList;
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
        String id=info.getId().replace(":", "-");
        // TODO check if it exists
        // Add to storage
        ResourceInfo ri = info.getResource();
        CoordinateReferenceSystem crs = ri.getCRS();
        
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder(ftf);
        builder.setName(id);
        builder.setCRS(crs);
        builder.add("guid", String.class);
        builder.add("time", Timestamp.class);
        
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
        
        for(LayerInfo li: realLayers(getPublished(layerName))){
            createLog(li, ds);
        }
    }
    
    List<LayerInfo> realLayers(PublishedInfo info) {
        if(info instanceof LayerInfo) {
            return Collections.singletonList((LayerInfo)info);
        } else if(info instanceof LayerGroupInfo) {
            // we need to use the layers method that flattens the layer group to actual layers rather than getLayers
            return ((LayerGroupInfo)info).layers();
        } else {
            throw new IllegalStateException("Unknown class of PublishedInfo: "+info.getClass().toString());
        }
    }
    
    PublishedInfo getPublished(String name) {
        try (CloseableIterator<PublishedInfo> it = gs.getCatalog().list(PublishedInfo.class, filterForName(name));){
            if(it.hasNext()) {
                PublishedInfo info = it.next();
                if(it.hasNext()) {
                    throw new IllegalStateException("Found two or more layers/groups named "+name);
                }
                return info;
            } else {
                throw new IllegalStateException("Found no layers/groups named "+name);
            }
        }
    }
    
    class CloseableList<T extends Closeable> extends ForwardingList<T> implements List<T>, Closeable {
        
        List<T> delegate;
        
        public CloseableList(List<T> delegate) {
            super();
            this.delegate = delegate;
        }
        
        @Override
        public void close() throws IOException {
            List<Exception> exceptions = delegate().stream().map(t->{
                try{
                    t.close();
                    return null;
                } catch (Exception ex) { // Might be IOException or RunTimeException so catch everything
                    return ex;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
            
            if(exceptions.isEmpty()) {
                return;
            } else if (exceptions.size()==1) {
                propagate(exceptions.get(0));
                assert false;
            } else {
                java.util.Iterator<Exception> it = exceptions.iterator();
                Exception ex = it.next();
                while(it.hasNext()) {
                    ex.addSuppressed(it.next());
                }
                propagate(ex);
                assert false;
            }
        }
        
        private void propagate(Exception ex) throws IOException {
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            } else if(ex instanceof IOException) {
                throw (IOException) ex;
            } else {
                throw new IllegalStateException("Unexpected checked exception", ex);
            }
        }
        
        @Override
        protected List<T> delegate() {
            return delegate;
        }
        
    }
    
    // Used to distinguish between having reached and looked at the last feature, and having dealt with it
    private class FeatureStepper implements Comparable<FeatureStepper>, Closeable {
        FeatureReader<SimpleFeatureType, SimpleFeature> reader;
        SimpleFeature next = null;
        
        public FeatureStepper(FeatureReader<SimpleFeatureType, SimpleFeature> reader) throws IOException {
            super();
            this.reader = reader;
            step();
        }
        
        @Override
        public int compareTo(FeatureStepper o) {
            if(next==null && o.next==null) return 0;
            if(next==null) return 1;
            if(o.next==null) return -1;
            
            return ((Timestamp)next.getAttribute("time")).compareTo(((Timestamp)o.next.getAttribute("time")));
        }
        
        public Geometry getGeometry() {
            return (Geometry) next.getDefaultGeometry();
        }
        
        public Date getTime() {
            return (Date) next.getAttribute("time");
        }
        
        public String getGuid() {
            return (String) next.getAttribute("guid");
        }
        
        public boolean step() throws IOException {
            if(reader.hasNext()) {
                next = reader.next();
                return true;
            } else {
                next = null;
                return false;
            }
        }
        
        public boolean isLive() {
            return next!=null;
        }
        
        @Override
        public void close() throws IOException {
            reader.close();
        }
    }
    
    public void getChanges(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        List<String> logNames = new LinkedList<>();
        for(String loggedLayerName : request.getParameterValues("layers")){
            for(LayerInfo li: realLayers(getPublished(loggedLayerName))){
                logNames.add(li.getId().replace(':', '-'));
            }
        }
        
        DataStoreInfo dsi = gs.getCatalog().getDataStoreByName(workspace, datastore);
        
        final JDBCDataStore dataStore = (JDBCDataStore) dsi.getDataStore(null);
        
        Filter filter=Filter.INCLUDE;// FIXME
        try(
                CloseableList<FeatureStepper> steppers = new CloseableList<>(new ArrayList<>(logNames.size()));
                OutputStream out = response.getOutputStream();
                Transaction t = new DefaultTransaction();
            ) {
            for(String logName : logNames) {
                final Query q = new Query(logName);
                q.setFilter(filter);
                steppers.add(new FeatureStepper(dataStore.getFeatureReader(q, t)));
            }
            
            ChangelogOutput output=new GeoRSSOutput(out);
            output.start();
            FeatureStepper next;
            while ((next = steppers.stream().min(FeatureStepper::compareTo).get()).isLive()) {
                output.entry(next.getGuid(), next.getTime(), next.getGeometry());
                
                next.step();
            }
            output.end();
        }

    }
    
    @Override
    public void initialize(GeoServer geoServer) throws Exception {
        gs = geoServer;
        ff = CommonFactoryFinder.getFilterFactory2( GeoTools.getDefaultHints());
        ftf = new FeatureTypeFactoryImpl(); // Because CommonFactoryFinder.getFeatureTypeFactory doesn't work.
    }
    
}
