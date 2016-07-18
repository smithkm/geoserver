/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.format;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONStreamAware;
import org.json.simple.JSONValue;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;

/**
 * A format that automatically converts a map into JSON and vice versa.
 * <p>
 * The <a href="http://json-lib.sourceforge.net/">json-lib</a> library is used to read and 
 * write JSON.
 * </p>
 * 
 *
 * @author David Winslow <dwinslow@openplans.org>
 */
public class MapJSONFormat extends StreamDataFormat {

    public MapJSONFormat(){
        super(MediaType.APPLICATION_JSON);
    }

    @Override
    protected void write(Object object, OutputStream out) throws IOException {
        //TODO: character set
        Writer outWriter = new BufferedWriter(new OutputStreamWriter(out));

        //JD: why does this initial flush occur?
        outWriter.flush();
        
        Object obj = toJSONObject(object);
        if(obj instanceof JSONStreamAware) {
            ((JSONStreamAware) obj).writeJSONString(outWriter);
        } else {
            JSONValue.writeJSONString(obj, outWriter);
        }
        outWriter.flush();
    }
    
    public Object toJSONObject(Object obj) {
        if (obj instanceof Map) {
            final Map<String,?> m = (Map<String,?>) obj;
            final JSONObject json = new JSONObject();
            m.entrySet().stream()
                    .forEach(e->json.put(e.getKey(), toJSONObject(e.getValue())));
            return json;
        } else if (obj instanceof Collection) {
            Collection<?> col = (Collection<?>) obj;
            return col.stream()
                    .map(e->toJSONObject(e))
                    .collect(Collectors.toCollection(JSONArray::new));
        } else if (obj instanceof Number) {
            return obj;
        } else if (obj instanceof Boolean) {
            return obj;
        } else if (obj == null) {
            return null;
        } else {
            return obj.toString();
        }
    }
    
    public Representation createRepresentation(Object data, Resource resource,
            Request request, Response response) {
        return null;
    }

    @Override
    protected Object read(InputStream in) throws IOException {
        //TODO: character set
        JSONParser parser = new JSONParser();
        BufferedReader reader = new BufferedReader( new InputStreamReader( in ) );
        try {
            return parser.parse(reader);
        } catch (ParseException e) {
            throw new IOException("Error reading JSON");
        }
    }
}
