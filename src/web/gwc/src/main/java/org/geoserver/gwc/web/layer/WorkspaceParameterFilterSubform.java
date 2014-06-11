/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.gwc.web.layer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.CheckBoxMultipleChoice;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.gwc.layer.WorkspaceParameterFilter;

/**
 * Subform that displays basic information about a ParameterFilter
 * @author Kevin Smith, Boundless
 *
 */
public class WorkspaceParameterFilterSubform extends AbstractParameterFilterSubform<WorkspaceParameterFilter> {

    /**
     * Model Set<String> as a List<String> and optionally add a dummy element at the beginning.
     */
    static class SetAsListModel implements IModel<List<String>> {

        /** serialVersionUID */
        private static final long serialVersionUID = 1L;
        
        final private IModel<Set<String>> realModel;
        
        final private List<String> fakeObject;
        
        final protected String extra;
        
        public SetAsListModel(IModel<Set<String>> realModel, String extra) {
            super();
            this.realModel = realModel;
            this.extra = extra;
            
            Set<String> realObj =  realModel.getObject();
            
            int size;
            if(realObj==null) {
                size = 0;
            } else {
                size = realObj.size();
            }
            if(extra!=null){
                size++;
            }
            fakeObject = new ArrayList<String>(size);
        }

        @Override
        public void detach() {
            realModel.detach();
        }

        @Override
        public List<String> getObject() {
            Set<String> realObj = realModel.getObject();
            
            fakeObject.clear();
            
            if(extra!=null) fakeObject.add(extra);
            if(realObj != null) fakeObject.addAll(realObj);
            
            return fakeObject;
        }

        @Override
        public void setObject(List<String> object) {
            if(object == null){
                realModel.setObject(null);
            } else {
                Set<String> newObj = new HashSet<String>(object);
                newObj.remove(extra);
                realModel.setObject(new HashSet<String>(object));
            }
        }
    }
    static class LabelledEmptyStringModel implements IModel<String> {
    
        final private IModel<String> realModel;
        
        final String label;

        public LabelledEmptyStringModel(IModel<String> realModel, String label) {
            super();
            this.realModel = realModel;
            this.label = label;
        }

        @Override
        public void detach() {
            realModel.detach();
        }

        @Override
        public String getObject() {
            String s = realModel.getObject();
            if (s==null || s.isEmpty()){
                return label;
            } else {
                return s;
            }
        }

        @Override
        public void setObject(String object) {
            if (object.equals(label)) {
                realModel.setObject("");
            } else {
                realModel.setObject(object);
            }
        }
    
    }
    /**
     * Model Set<String> as a List<String> and add an option to represent the set being 
     * {@literal null}
     */
    static class NullableSetAsListModel implements IModel<List<String>> {

        /** serialVersionUID */
        private static final long serialVersionUID = 1L;
        
        final private IModel<Set<String>> realModel;
        
        final private List<String> fakeObject;
        
        final protected String nullify;
        
        public NullableSetAsListModel(IModel<Set<String>> realModel, String nullify) {
            super();
            this.realModel = realModel;
            this.nullify = nullify;
            
            Set<String> realObj =  realModel.getObject();
            
            int size;
            if(realObj==null) {
                size = 1;
            } else {
                size = realObj.size();
            }
            fakeObject = new ArrayList<String>(size);
        }

        @Override
        public void detach() {
            realModel.detach();
        }

        @Override
        public List<String> getObject() {
            Set<String> realObj = realModel.getObject();
            
            fakeObject.clear();
            
            if(realObj!=null) {
                fakeObject.addAll(realObj);
            } else {
                fakeObject.add(nullify);
            }
            
            return fakeObject;
        }

        @Override
        public void setObject(List<String> object) {
            if(object == null || object.contains(nullify)){
                realModel.setObject(null);
            } else {
                Set<String> newObj = new HashSet<String>(object);
                newObj.remove(nullify);
                realModel.setObject(new HashSet<String>(object));
            }
        }
    }
    


    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    public WorkspaceParameterFilterSubform(String id,
            IModel<WorkspaceParameterFilter> model) {
        super(id, model);
        
        final Component defaultValue;
        
        final String allWorkspaces = getLocalizer().getString("allWorkspaces", this);
        final String layerDefault = getLocalizer().getString("layerDefault", this);
        
        final IModel<List<String>> availableWorkspacesModelDefault = 
                new SetAsListModel(new PropertyModel<Set<String>>(model, "layerWorkspaces"), layerDefault);
        final IModel<List<String>> availableWorkspacesModelAllowed = 
                new SetAsListModel(new PropertyModel<Set<String>>(model, "layerWorkspaces"), allWorkspaces);
        final IModel<List<String>> selectedWorkspacesModel = 
                new NullableSetAsListModel(new PropertyModel<Set<String>>(model, "workspaces"), allWorkspaces);
        final IModel<String> selectedDefaultModel = 
                new LabelledEmptyStringModel(new PropertyModel<String>(model, "realDefault"), layerDefault);
        
        defaultValue = new DropDownChoice<String>("defaultValue", selectedDefaultModel, availableWorkspacesModelDefault);
        add(defaultValue);
        
        final CheckBoxMultipleChoice<String> workspaces = new CheckBoxMultipleChoice<String>("workspaces", selectedWorkspacesModel, availableWorkspacesModelAllowed);
        workspaces.setPrefix("<li>");workspaces.setSuffix("</li>");
        add(workspaces);
    }

}
