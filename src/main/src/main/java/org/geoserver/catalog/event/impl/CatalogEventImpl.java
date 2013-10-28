/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.event.impl;

import java.util.HashMap;
import java.util.Map;

import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.event.CatalogEvent;

public class CatalogEventImpl implements CatalogEvent {
    
    CatalogInfo source;
    
    Map<String, Object> hints = new HashMap<String, Object>();
    
    public CatalogInfo getSource() {
        return source;
    }
    
    public void setSource(CatalogInfo source) {
        this.source = source;
    }
    
    @Override
    public Map<String, Object> getHints() {
        return hints;
    }
}
