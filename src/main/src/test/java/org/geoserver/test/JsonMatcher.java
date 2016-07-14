/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import org.hamcrest.Matcher;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.google.common.base.Objects;

public class JsonMatcher {
    
    /**
     * Test that a string is JSON equivalent to that given.
     * @param json expected JSON
     * @return
     */
    public static Matcher<String> jsonEquals(final String json) {
        return new BaseMatcher<String>() {
            
            @Override
            public boolean matches(Object item) {
                JSONParser parser = new JSONParser();
                try {
                    return Objects.equal(parser.parse((String)item),parser.parse(json));
                } catch (ParseException e) {
                    return false;
                }
            }
            
            @Override
            public void describeTo(Description description) {
                description.appendText("json equivalent to ").appendValue(json);
            }
            
        };
    }
    
    private JsonMatcher() {
        // TODO Auto-generated constructor stub
    }

}
