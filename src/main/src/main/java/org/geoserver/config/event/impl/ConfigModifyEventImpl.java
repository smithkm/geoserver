package org.geoserver.config.event.impl;

import java.util.ArrayList;
import java.util.List;

import org.geoserver.config.event.ConfigModifyEvent;
import org.geoserver.config.ConfigInfo;

public class ConfigModifyEventImpl<T extends ConfigInfo> 
    extends ConfigEventImpl<T> implements ConfigModifyEvent<T> {

    List<String> propertyNames = new ArrayList<String>();
    List<Object> oldValues = new ArrayList<Object>();
    List<Object> newValues = new ArrayList<Object>();

    public List<String> getPropertyNames() {
        return propertyNames;
    }

    public void setPropertyNames(List<String> propertyNames) {
        this.propertyNames = propertyNames;
    }

    public List<Object> getNewValues() {
        return newValues;
    }

    public void setNewValues(List<Object> newValues) {
        this.newValues = newValues;
    }

    public List<Object> getOldValues() {
        return oldValues;
    }

    public void setOldValues(List<Object> oldValues) {
        this.oldValues = oldValues;
    }

}
