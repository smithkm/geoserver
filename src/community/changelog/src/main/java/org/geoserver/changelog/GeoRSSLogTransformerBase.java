package org.geoserver.changelog;

import java.util.Collection;

import org.geotools.gml.producer.GeometryTransformer.GeometryTranslator;
import org.geotools.xml.transform.TransformerBase;
import org.geotools.xml.transform.Translator;
import org.xml.sax.ContentHandler;

public class GeoRSSLogTransformerBase extends TransformerBase {
    
    public GeoRSSLogTransformerBase() {
        // TODO Auto-generated constructor stub
    }
    
    @Override
    public Translator createTranslator(ContentHandler handler) {
        return new TranslatorSupport(handler, null, "http://www.w3.org/2005/Atom") {
            GeometryTranslator geometryTranslator;
            {
                nsSupport.declarePrefix("georss","http://www.georss.org/georss");
                nsSupport.declarePrefix("gml","http://www.opengis.net/gml");
                geometryTranslator = new GeometryTranslator(handler);
            }
            @Override
            public void encode(Object o) throws IllegalArgumentException {
                if(o instanceof ChangeEntry) {
                    ChangeEntry entry = (ChangeEntry) o;
                    start("entry"); {
                        element("title", null); // TODO
                        element("link", null); // TODO
                        element("id", entry.getGuid());
                        element("updated", entry.getTime().toString()); // TODO get format right
                        element("content", null);
                        start("georss:where"); {
                        geometryTranslator.encode(entry.getGeom());
                        } end("georss:where");
                    } end("entry");
                } else if(o instanceof Collection) {
                    start("feed"); {
                        element("title", null); // TODO
                        element("subtitle", null); // TODO
                        element("link", null); // TODO
                        element("updated", null); // TODO
                        start("author");{
                            element("name", null); // TODO
                            element("email", null); // TODO
                        }end("author");
                        for(Object element: (Collection<?>)o) {
                            encode(element);
                        }
                    }end("feed");
                } else {
                    throw new IllegalArgumentException();
                }
            }
        };
    }
    
}
