/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.gwc.layer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geotools.util.logging.Logging;
import org.geowebcache.filter.parameters.ParameterException;
import org.geowebcache.filter.parameters.ParameterFilter;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.thoughtworks.xstream.annotations.XStreamAlias;

import static com.google.common.base.Preconditions.*;

/**
 * ParameterFilter which allows the workspace of the back end to be specified. Maintains a set 
 * of allowed workspaces which are intersected with those available on the layer.
 * 
 * @author Kevin Smith, OpenGeo
 *
 */
@XStreamAlias("workspaceParameterFilter")
public class WorkspaceParameterFilter extends ParameterFilter {

    private static final Logger LOGGER = Logging.getLogger(GeoServerTileLayerInfoImpl.class);

    private Set<String> allowedWorkspaces;
    
    // The following two fields are omitted from REST
    private Set<String> availableWorkspaces;
    
    /** serialVersionUID */
    private static final long serialVersionUID = 1L;
    
    
    /**
     * Check that setLayer has been called
     */
    protected void checkInitialized(){
        checkState(availableWorkspaces!=null, "Current workspaces not available.");
    }
    
    public WorkspaceParameterFilter(){
        super("WORKSPACE");
    }
    
    @Override
    public String getDefaultValue() {
        checkInitialized();
        return super.getDefaultValue();
    }
    
    @Override
    public boolean applies(String parameterValue) {
        checkInitialized();
        return parameterValue==null || getLegalValues().contains(parameterValue);
    }

    @Override
    public String apply(String str) throws ParameterException {
        checkInitialized();
        if(str == null || str.isEmpty()) {
            // Use the default
            return getDefaultValue();
        } else {
            for(String value: getLegalValues()){
                // Find a matching style
                if (value.equalsIgnoreCase(str)) {
                    return value;
                }
            }
            // no match so fail
            throw new ParameterException(str);
        }
    }
    
    @Override
    public void setKey(String key) {
        checkArgument(key.equalsIgnoreCase("WORKSPACE"));
    }
    
    @Override
    public void setDefaultValue(String defaultValue) {
        if(defaultValue==null) defaultValue="";
        if(!defaultValue.isEmpty() && availableWorkspaces!=null && !availableWorkspaces.contains(defaultValue)) {
            LOGGER.log(Level.WARNING, "Selected default style "+defaultValue+" is not in the available styles "+availableWorkspaces+".");
        }
        super.setDefaultValue(defaultValue);
    }
    
    /**
     * Returns the default workspace name, or an empty string if set to use the layer specified default
     * @return
     */
    public String getRealDefault() {
        // Bypass the special processing this class normally does on the default value
        return super.getDefaultValue();
    }
    
    /**
     * @see WorkspaceParameterFilter#setDefaultValue()
     * @param s
     */
    public void setRealDefault(String s) {
        // Just use the regular set method
        setDefaultValue(s);
    }
    
    @Override
    public WorkspaceParameterFilter clone() {
        WorkspaceParameterFilter clone = new WorkspaceParameterFilter();
        clone.setDefaultValue(super.getDefaultValue()); // Want to get the configured value so use super
        clone.setKey(getKey());
        clone.allowedWorkspaces = getStyles();
        clone.availableWorkspaces = availableWorkspaces;
        return clone;
    }
    
    /**
     * Get the names of all the styles supported by the layer
     * @return
     */
    public Set<String> getLayerWorkspaces() {
        checkInitialized();
        return availableWorkspaces;
    }
    
    @Override
    public List<String> getLegalValues() {
        checkInitialized();
        Set<String> layerWorkspaces = getLayerWorkspaces();
        if (allowedWorkspaces==null) {
            // Values is null so allow any of the backing layer's workspaces
            return new ArrayList<String>(layerWorkspaces);
        } else {
            // Values is set so only allow the intersection of the specified workspaces and those of the backing layer.
            return new ArrayList<String>(Sets.intersection(layerWorkspaces, allowedWorkspaces));
        }
    }
    
    /**
     * Set/update the availableStyles and defaultStyle based on the given GeoServer layer.
     * 
     * @param layer
     */
    public void setLayer(Collection<String> availableWorkspaces) {
        this.availableWorkspaces = new TreeSet<String>(availableWorkspaces);
    }
    
    /**
     * Get the styles.
     * @return The set of specified styles, or {@literal null} if all styles are allowed.
     */
    @Nullable public Set<String> getStyles() {
        if(allowedWorkspaces==null) return null;
        return Collections.unmodifiableSet(allowedWorkspaces);
    }
    
    
    /**
     * Set the allowed workspace.  {@code null} to allow only the layer's own wo.
     * @param styles
     */
    public void setWorkspaces(@Nullable Set<String> styles) {
        if(styles==null) {
            this.allowedWorkspaces=null;
        } else {
            this.allowedWorkspaces = new TreeSet<String>(styles);
        }
    }
    
    @Override
    protected ParameterFilter readResolve() {
        super.readResolve();
        Preconditions.checkState(this.getKey().equalsIgnoreCase("WORKSPACE"), "WorkspaceParameterFilter must have a key of \"WORKSPACE\"");
        return this;
    }

}
