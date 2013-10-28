/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.event;

import org.geoserver.catalog.CatalogInfo;
import org.geoserver.event.GeoServerEvent;

/**
 * Catalog event.
 * 
 * @author Justin Deoliveira, The Open Planning Project
 */
public interface CatalogEvent extends GeoServerEvent {

    /**
     * The source of the event.
     */
    CatalogInfo getSource();

}
