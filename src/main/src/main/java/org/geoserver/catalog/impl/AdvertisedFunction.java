package org.geoserver.catalog.impl;

import java.util.List;

import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geotools.filter.FunctionExpressionImpl;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.capability.FunctionName;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.InternalFunction;
import org.opengis.filter.expression.VolatileFunction;
import org.geotools.filter.capability.FunctionNameImpl;
import org.geotools.filter.expression.InternalVolatileFunction;

/**
 * Filter function that takes a PublishedInfo and determines if it should be advertised.
 * 
 * @author Kevin Smith, Boundless
 *
 */
public class AdvertisedFunction extends InternalVolatileFunction implements VolatileFunction, InternalFunction, Expression {

    public static FunctionName NAME = new FunctionNameImpl("advertised", new String[]{});

    protected AdvertisedFunction() {
        super(NAME.getName());
    }

    @Override
    public Boolean evaluate(Object object) {
        if(object instanceof LayerInfo) {
            LayerInfo info = (LayerInfo) object;
            return info.enabled() && info.isAdvertised();
        } else if (object instanceof LayerGroupInfo) {
            LayerGroupInfo info = (LayerGroupInfo) object;
            boolean enabled = true;
            for (LayerInfo layer : info.layers()) {
                enabled &= layer.enabled();
            }
            
            return enabled && info.layers().size() > 0;
            
        } else if (object instanceof ResourceInfo) {
            ResourceInfo info = (ResourceInfo) object;
            return info.enabled() && info.isAdvertised();
        } else if (object instanceof PublishedInfo) {
            throw new UnsupportedOperationException("Advertised does not yet support PublishedInfo subclass "+object.getClass());
        } else {
            throw new IllegalArgumentException("Advertised should only be applied to PublishedInfo objects, not "+object);
        }
    }

    @Override
    public void setParameters(List<Expression> parameters) {
        if (parameters.size()!=0) {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public InternalFunction duplicate(Expression... parameters) {
        return this;
    }


}
