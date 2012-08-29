/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2011, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geoserver.jdbcconfig.internal;

import junit.framework.TestCase;

import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.WorkspaceInfo;
import org.opengis.filter.Filter;

/**
 * @author groldan
 * 
 */
public class QueryBuilderTest extends TestCase {

    private JdbcConfigTestSupport testSupport;

    private DbMappings dbMappings;

    public void setUp() throws Exception {
        dbMappings = new DbMappings();
        testSupport = new JdbcConfigTestSupport();
        testSupport.setUp();
        dbMappings = testSupport.getDbMappings();
    }

    public void tearDown() throws Exception {
        testSupport.tearDown();
    }

    public void testQueryAll() {
        Filter filter = Predicates.equal("name", "ws1");
        StringBuilder build = QueryBuilder.forIds(WorkspaceInfo.class, dbMappings).filter(filter)
                .build();

    }
}
