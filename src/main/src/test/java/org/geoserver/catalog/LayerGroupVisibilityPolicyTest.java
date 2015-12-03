/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.catalog;

import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.createMock;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.easymock.classextension.EasyMock;
import org.junit.Before;
import org.junit.Test;

public class LayerGroupVisibilityPolicyTest {
    
    LayerGroupInfo emptyGroup;
    LayerGroupInfo manyGroup;
    List<PublishedInfo> emptyList;
    List<PublishedInfo> manyList;
    
    LayerGroupVisibilityPolicy.Factory factory;
    
    @Before
    public void setUpMocks() {
        emptyGroup = createMock(LayerGroupInfo.class);
        manyGroup = createMock(LayerGroupInfo.class);
        emptyList = Collections.emptyList();
        manyList = Arrays.asList(null, null, null); // Don't expect the current implementations to look at the details so nulls are enough
        
        expect(emptyGroup.getLayers()).andStubReturn(emptyList);
        expect(manyGroup.getLayers()).andStubReturn(manyList);
        
        EasyMock.replay(emptyGroup, manyGroup);
    }
    
    @Before
    public void setUpFactory() {
        factory = new LayerGroupVisibilityPolicy.Factory();
        factory.setDefaultValue("HIDE_NEVER");
    }
     
    @Test
    public void testNever() {
        LayerGroupVisibilityPolicy policy = LayerGroupVisibilityPolicy.HIDE_NEVER;
        
        testNever(policy);
    }
    
    @Test
    public void testFactoryNever() {
        LayerGroupVisibilityPolicy policy = factory.createInstance("HIDE_NEVER");
        
        testNever(policy);
    }
    
    private void testNever(LayerGroupVisibilityPolicy policy) {
        assertThat(policy.hideLayerGroup(manyGroup, manyList), is(false));
        assertThat(policy.hideLayerGroup(manyGroup, emptyList), is(false));
        assertThat(policy.hideLayerGroup(emptyGroup, emptyList), is(false));
    }
    
    @Test
    public void testEmpty() {
        LayerGroupVisibilityPolicy policy = LayerGroupVisibilityPolicy.HIDE_EMPTY;
        
        testEmpty(policy);
    }
    
    @Test
    public void testFactoryEmpty() {
        LayerGroupVisibilityPolicy policy = factory.createInstance("HIDE_EMPTY");
        
        testEmpty(policy);
    }
    
    private void testEmpty(LayerGroupVisibilityPolicy policy) {
        assertThat(policy.hideLayerGroup(manyGroup, manyList), is(false));
        assertThat(policy.hideLayerGroup(manyGroup, emptyList), is(true));
        assertThat(policy.hideLayerGroup(emptyGroup, emptyList), is(true));
    }
    
    @Test
    public void testIfAllHidden() {
        LayerGroupVisibilityPolicy policy = LayerGroupVisibilityPolicy.HIDE_IF_ALL_HIDDEN;
        
        testIfAllHidden(policy);
    }
    
    @Test
    public void testFactoryIfAllHidden() {
        LayerGroupVisibilityPolicy policy = factory.createInstance("HIDE_IF_ALL_HIDDEN");
        
        testIfAllHidden(policy);
    }
    
    private void testIfAllHidden(LayerGroupVisibilityPolicy policy) {
        assertThat(policy.hideLayerGroup(manyGroup, manyList), is(false));
        assertThat(policy.hideLayerGroup(manyGroup, emptyList), is(true));
        assertThat(policy.hideLayerGroup(emptyGroup, emptyList), is(false));
    }
    
    @Test
    public void testFactoryUnknown() {
        LayerGroupVisibilityPolicy policy = factory.createInstance("NOT_A_REAL_VISIBILITY_POLICY");
        
        assertThat(policy, nullValue());
    }
}
