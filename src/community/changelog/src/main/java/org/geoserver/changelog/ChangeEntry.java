package org.geoserver.changelog;

import java.util.Date;

import com.vividsolutions.jts.geom.Geometry;

public class ChangeEntry {
    String guid;
    Date time;
    Geometry geom;
    
    public ChangeEntry(String guid, Date time, Geometry geom) {
        super();
        this.guid = guid;
        this.time = time;
        this.geom = geom;
    }
    
    /**
     * @return the guid
     */
    public String getGuid() {
        return guid;
    }
    
    /**
     * @return the time
     */
    public Date getTime() {
        return time;
    }
    
    /**
     * @return the geom
     */
    public Geometry getGeom() {
        return geom;
    }
}
