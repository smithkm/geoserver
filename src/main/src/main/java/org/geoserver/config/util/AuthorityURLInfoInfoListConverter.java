/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.util;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.json.simple.JSONObject;
import org.geoserver.catalog.AuthorityURLInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.impl.AuthorityURL;
import org.geoserver.catalog.impl.LayerIdentifier;

/**
 * Utility class to serialize and deserialize a list of {@link AuthorityURLInfo} objects to and from
 * String using a JSON array representation as serialized form so that {@link XStreamPersister}
 * stores it under a single key in a catalog info's {@link MetadataMap}.
 * 
 * @author groldan
 * 
 */
public class AuthorityURLInfoInfoListConverter {

    private static final String NAME = "name";

    private static final String HREF = "href";

    /**
     * @param str
     *            a JSON array representation of a list of {@link AuthorityURLInfo} objects
     * @return the list of parsed authrority URL from the argument JSON array
     * @throws IllegalArgumentException
     *             if {@code str} can't be parsed to a JSONArray
     */
    @SuppressWarnings("unchecked")
    public static List<AuthorityURLInfo> fromString(String str) throws IllegalArgumentException {
        JSONParser parser = new JSONParser();
        try {
            final Object array;
            array = parser.parse(str);
            if(! (array instanceof JSONArray)) {
                throw new IllegalArgumentException("Expected an array");
            }
            return ((List<JSONObject>)array).stream()
                    .map(jsonAuth->{
                        final AuthorityURL id = new AuthorityURL();
                        id.setName((String)jsonAuth.get(NAME));
                        id.setHref((String)jsonAuth.get(HREF));
                        return id;
                    }).collect(Collectors.toList());
        } catch (ParseException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    /**
     * @param list
     *            the list of layer identifiers to serialize
     * @return {@code null} if {@code list} is null, empty or contains only null objects; the JSON
     *         array representation of {@code list} otherwise, with any null element stripped off.
     */
    public static String toString(List<AuthorityURLInfo> obj) {
        if (obj == null || obj.isEmpty()) {
            return null;
        }
        JSONArray array = new JSONArray();

        for (AuthorityURLInfo auth : obj) {
            if (auth == null) {
                continue;
            }
            JSONObject jsonAuth = new JSONObject();
            jsonAuth.put(NAME, auth.getName());
            jsonAuth.put(HREF, auth.getHref());
            array.add(jsonAuth);
        }

        if (array.size() == 0) {
            // list was made of only null objects?
            return null;
        }

        String serialized = array.toString();
        return serialized;
    }
}
