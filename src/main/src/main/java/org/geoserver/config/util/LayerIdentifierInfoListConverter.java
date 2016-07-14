/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.util;

import java.util.List;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.json.simple.JSONObject;
import org.geoserver.catalog.LayerIdentifierInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.impl.LayerIdentifier;

/**
 * Utility class to serialize and deserialize a list of {@link LayerIdentifierInfo} objects to and
 * from String using a JSON array representation as serialized form so that {@link XStreamPersister}
 * stores it under a single key in a catalog info's {@link MetadataMap}.
 * 
 * @author groldan
 * 
 */
public class LayerIdentifierInfoListConverter {

    private static final String AUTHORITY = "authority";

    private static final String IDENTIFIER = "identifier";

    /**
     * @param str
     *            a JSON array representation of a list of {@link LayerIdentifierInfo} objects
     * @return the list of parsed layer identifiers from the argument JSON array
     * @throws IllegalArgumentException
     *             if {@code str} can't be parsed to a JSONArray
     */
    @SuppressWarnings("unchecked")
    public static List<LayerIdentifierInfo> fromString(String str) throws IllegalArgumentException {
        JSONParser parser = new JSONParser();
        try {
            final Object array;
            array = parser.parse(str);
            if(! (array instanceof JSONArray)) {
                throw new IllegalArgumentException("Expected an array");
            }
            return ((List<JSONObject>)array).stream()
                    .map(jsonAuth->{
                        final LayerIdentifier id = new LayerIdentifier();
                        id.setAuthority((String)jsonAuth.get(AUTHORITY));
                        id.setIdentifier((String)jsonAuth.get(IDENTIFIER));
                        return id;
                    }).collect(Collectors.toList());
        } catch (ParseException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    /**
     * @param list
     *            the list of auth urls to serialize
     * @return {@code null} if {@code list} is null, empty, or contains only null objects; the JSON
     *         array representation of {@code list} otherwise, with any null element stripped off.
     */
    public static String toString(List<LayerIdentifierInfo> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        JSONArray array = new JSONArray();

        for (LayerIdentifierInfo id : list) {
            if (id == null) {
                continue;
            }
            JSONObject jsonId = new JSONObject();
            jsonId.put(AUTHORITY, id.getAuthority());
            jsonId.put(IDENTIFIER, id.getIdentifier());
            array.add(jsonId);
        }

        if (array.size() == 0) {
            // list was made of only null objects?
            return null;
        }
        String serialized = array.toString();
        return serialized;
    }
}
