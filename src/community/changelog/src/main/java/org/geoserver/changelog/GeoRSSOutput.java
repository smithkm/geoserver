package org.geoserver.changelog;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Date;

import javax.xml.namespace.QName;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;

import org.geotools.gml.producer.FeatureTransformer;
import org.geotools.gml.producer.GeometryTransformer;
import org.geotools.gml.producer.GeometryTransformer.GeometryTranslator;
import org.geotools.gml2.GMLConfiguration;
import org.geotools.xml.Encoder;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.DefaultHandler;

import com.vividsolutions.jts.geom.Geometry;

public class GeoRSSOutput implements ChangelogOutput {
    
    GeometryTransformer transform;
    
    private OutputStream out;
    
    public GeoRSSOutput(OutputStream out) {
        transform = new GeometryTransformer();
        transform.setEncoding(Charset.defaultCharset());
        transform.setIndentation(2);
        transform.setNamespaceDeclarationEnabled(false);
        transform.setOmitXMLDeclaration(true);
        this.out = out;
    }
    
    @Override
    public void start() throws IOException {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void entry(String guid, Date time, Geometry geom) throws IOException {
        try {
            transform.transform(geom, out);
        } catch (TransformerException e) {
            throw new IOException(e);
        }
    }
    
    @Override
    public void end() throws IOException {
        // TODO Auto-generated method stub
    
    }
    
}
