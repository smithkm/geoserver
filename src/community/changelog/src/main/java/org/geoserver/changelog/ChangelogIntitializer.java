package org.geoserver.changelog;

import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInitializer;
import org.geotools.data.DataAccess;
import org.geotools.data.FeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.NameImpl;
import org.geotools.jdbc.JDBCDataStore;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class ChangelogIntitializer implements GeoServerInitializer {
    
    @Override
    public void initialize(GeoServer geoServer) throws Exception {
        DataStoreInfo dsi = geoServer.getCatalog().getDataStoreByName("georss", "georss");
        Name logName = new NameImpl("changelog");
        final JDBCDataStore dataStore = (JDBCDataStore) dsi.getDataStore(null);
        FeatureSource<?,SimpleFeature> fs = dataStore.getFeatureSource(logName);
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2( GeoTools.getDefaultHints());
        Filter filter = ff.equals(ff.property("layer"), ff.literal("blah"));
        try(FeatureIterator<SimpleFeature> fi = fs.getFeatures(filter).features();) {
            while (fi.hasNext()) {
                SimpleFeature f = fi.next();
                Object o = f.getDefaultGeometry();
                CoordinateReferenceSystem crs = f.getDefaultGeometryProperty().getDescriptor().getCoordinateReferenceSystem();
                o.getClass();
            }
        }
    }
    
}
