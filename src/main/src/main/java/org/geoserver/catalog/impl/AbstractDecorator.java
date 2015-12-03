/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import org.geoserver.catalog.Wrapper;

/**
 * Generic delegating base class. Provides the follwing features:
 * <ul>
 * <li>null check for the delegate object</li>
 * <li>direct forwarding of {@link #equals(Object)}, {@link #hashCode()} and
 * {@link #toString()} to the delegate</li>
 * <li>implements the Wrapper interface for programmatic extraction</li>
 * </ul>
 */
public abstract class AbstractDecorator<D> implements Wrapper {
    protected D delegate;

    public AbstractDecorator(D delegate) {
        if (delegate == null)
            throw new NullPointerException("Cannot delegate to a null object");
        this.delegate = delegate;
    }

    public boolean isWrapperFor(Class<?> iface) {
        if(iface.isInstance(this)) {
            // We are already the thing we're after
            return true;
        } else if (iface.isInstance(delegate)) {
            // The immediate delegate is what we're after
            return true;
        } else if (delegate instanceof Wrapper) {
            // No sign of what we're after, but we can keep digging
            return ((Wrapper) delegate).isWrapperFor(iface);
        } else {
            // We hit the bottom without finding it.
            return false;
        }
    }

    public <T> T unwrap(Class<T> iface) throws IllegalArgumentException {
        if(iface.isInstance(this)) {
            // We are already the thing we're after
            return iface.cast(this);
        } else if (iface.isInstance(delegate)) {
            // The immediate delegate is what we're after
            return iface.cast(delegate);
        } else if (delegate instanceof Wrapper) {
            // No sign of what we're after, but we can keep digging
            return ((Wrapper) delegate).unwrap(iface);
        } else {
            // We hit the bottom bottom so give up.
            throw new IllegalArgumentException("Cannot unwrap to the requested interface " + iface);
        }
    }

    public boolean equals(Object obj) {
        return delegate.equals(obj);
    }

    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public String toString() {
        return new StringBuilder(getClass().getSimpleName()).append('[').append(delegate).append(
                ']').toString();
    }

}
