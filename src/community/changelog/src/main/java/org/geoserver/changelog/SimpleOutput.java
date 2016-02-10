package org.geoserver.changelog;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

import com.vividsolutions.jts.geom.Geometry;

public class SimpleOutput implements ChangelogOutput {
    
    OutputStream out;
    
    public SimpleOutput(OutputStream out) {
        this.out = out;
    }
    
    @Override
    public void start() throws IOException {
        out.write("<changelog>\n".getBytes());
    }
    
    @Override
    public void entry(String guid, Date time, Geometry geom) throws IOException {
        out.write("<entry>".getBytes());
        out.write("<guid>".getBytes());
        out.write(guid.getBytes());
        out.write("</guid>".getBytes());
        out.write("<time>".getBytes());
        out.write(time.toString().getBytes());
        out.write("</time>".getBytes());
        out.write("<geom>".getBytes());
        out.write(geom.toText().getBytes());
        out.write("</geom>".getBytes());
        out.write("</geom>\n".getBytes());
    }
    
    @Override
    public void end() throws IOException {
        out.write("</changelog>".getBytes());
    }
    
}
