/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.event.impl;

import java.util.ArrayList;
import java.util.List;

import org.geoserver.catalog.event.CatalogModifyEvent;

public class CatalogModifyEventImpl extends CatalogEventImpl implements
        CatalogModifyEvent {

    List<String> propertyNames = new ArrayList<String>();
    List<?> oldValues = new ArrayList<Object>();
    List<?> newValues = new ArrayList<Object>();

    public List<String> getPropertyNames() {
        return propertyNames;
    }

    public void setPropertyNames(List<String> propertyNames) {
        this.propertyNames = propertyNames;
    }

    public List<?> getNewValues() {
        return newValues;
    }

    public void setNewValues(List<?> newValues) {
        this.newValues = newValues;
    }

    public List<?> getOldValues() {
        return oldValues;
    }

    public void setOldValues(List<?> oldValues) {
        this.oldValues = oldValues;
    }

}
