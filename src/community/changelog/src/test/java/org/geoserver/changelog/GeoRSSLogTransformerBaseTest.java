package org.geoserver.changelog;

import static org.junit.Assert.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;

import org.junit.Test;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

public class GeoRSSLogTransformerBaseTest {
    
    @Test
    public void test() throws Exception {
        GeoRSSLogTransformerBase base = new GeoRSSLogTransformerBase();
        base.setIndentation(2);
        GeometryFactory fact = new GeometryFactory();
        
        List<ChangeEntry> entries = new ArrayList<>();
        entries.add(new ChangeEntry(UUID.randomUUID().toString(), Date.from(Instant.now().minusSeconds(5)), fact.createMultiPolygon(new Polygon[]{fact.createPolygon(new Coordinate[]{new Coordinate(11, 11),new Coordinate(11, 12),new Coordinate(12, 11), new Coordinate(11, 11)})})));
        entries.add(new ChangeEntry(UUID.randomUUID().toString(), Date.from(Instant.now()), fact.createMultiPolygon(new Polygon[]{fact.createPolygon(new Coordinate[]{new Coordinate(1, 1),new Coordinate(1, 2),new Coordinate(2, 1), new Coordinate(1, 1)})})));
        base.createTransformTask(entries, new StreamResult(System.out)).run();
    }

}
