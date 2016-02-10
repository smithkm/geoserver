package org.geoserver.changelog;

import java.io.IOException;
import java.util.Date;

import com.vividsolutions.jts.geom.Geometry;

public interface ChangelogOutput {
    
    void start() throws IOException;
    
    void entry(String guid, Date time, Geometry geom) throws IOException;
    
    void end() throws IOException;
    
}
