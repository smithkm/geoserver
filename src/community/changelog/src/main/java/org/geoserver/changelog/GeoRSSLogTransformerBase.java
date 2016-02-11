package org.geoserver.changelog;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

import javax.annotation.Nullable;

import org.geotools.gml.producer.GeometryTransformer.GeometryTranslator;
import org.geotools.xml.transform.TransformerBase;
import org.geotools.xml.transform.Translator;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

public class GeoRSSLogTransformerBase extends TransformerBase {
    
    private static final String GML_NS = "http://www.opengis.net/gml";
    private static final String GEORSS_NS = "http://www.georss.org/georss";
    private static final String ATOM_NS = "http://www.w3.org/2005/Atom";
    private static final SimpleDateFormat RFC_3339 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
    
    Date updated;
    String link;
    
    public GeoRSSLogTransformerBase() {
        updated = new Date(); // FIXME Should be more careful about this
        link = "http://localhost:8080/geoserver/changelog"; // TODO
    }
    
    @Override
    public Translator createTranslator(ContentHandler handler) {
        return new TranslatorSupport(handler, null, ATOM_NS) {
            GeometryTranslator geometryTranslator;
            {
                nsSupport.declarePrefix("georss",GEORSS_NS);
                nsSupport.declarePrefix("gml",GML_NS);
                geometryTranslator = new GeometryTranslator(handler);
            }
            
            public void atomLink(String uri, @Nullable String rel) {
                AttributesImpl a = new AttributesImpl();
                if(rel != null) {
                    a.addAttribute(ATOM_NS, "rel", "rel", "CDATA", rel);
                }
                a.addAttribute(ATOM_NS, "href", "href", "CDATA", uri);
                element("link", link, a);
            }
            
            @Override
            public void encode(Object o) throws IllegalArgumentException {
                if(o instanceof ChangeEntry) {
                    ChangeEntry entry = (ChangeEntry) o;
                    encode(entry);
                } else if(o instanceof Collection) {
                    encode((Collection<?>) o);
                } else {
                    throw new IllegalArgumentException();
                }
            }
            
            public void encode(Collection<?> log) {
                start("feed"); {
                    element("title", null); // TODO
                    atomLink(link, "self");
                    element("updated", RFC_3339.format(updated));
                    start("author");{
                        element("name", null); // TODO
                        element("email", null); // TODO
                    }end("author");
                    for(Object element: log) {
                        encode(element);
                    }
                }end("feed");
            }
            
            public void encode(ChangeEntry entry) {
                start("entry"); {
                    element("title", null); // TODO
                    element("id", entry.getGuid());
                    element("updated", RFC_3339.format(entry.getTime()));
                    element("content", null);
                    start("georss:where"); {
                    geometryTranslator.encode(entry.getGeom());
                    } end("georss:where");
                } end("entry");
            }
        };
    }
    
}
