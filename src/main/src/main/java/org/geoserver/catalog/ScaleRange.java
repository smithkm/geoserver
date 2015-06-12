/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.catalog;

import java.io.Serializable;

import com.google.common.base.Preconditions;

public class ScaleRange implements Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = 741052656801937258L;
    
    private double largeDenom;
    private double smallDenom;
    
    public static final ScaleRange EXCLUDE = new ScaleRange(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
    public static final ScaleRange INCLUDE = new ScaleRange(0d, Double.POSITIVE_INFINITY);
    
    public ScaleRange(double largeDenom, double smallDenom) {
        Preconditions.checkArgument(largeDenom>=0, "largeDenom must be non-negative");
        Preconditions.checkArgument(smallDenom>=0, "smallDenom must be non-negative");
        Preconditions.checkArgument(largeDenom<=smallDenom, "largeDenom must be less than or equal to smallDenom");
        this.largeDenom = largeDenom;
        this.smallDenom = smallDenom;
    }
    
    /**
     * @return The denominator of the largest scale in the range
     */
    public double getLargeDenom() {
        return largeDenom;
    }

    /**
     * @return The denominator of the smallest scale in the range
     */
    public double getSmallDenom() {
        return smallDenom;
    }

    /**
     * Find the intersection with another scale range.
     * @param r
     * @return
     */
    public ScaleRange intersect(ScaleRange r) {
        double large = Math.max(this.getLargeDenom(), r.getLargeDenom());
        double small = Math.min(this.getSmallDenom(), r.getSmallDenom());
        if(large<=small) return EXCLUDE;
        return new ScaleRange(large, small);
    }
    
    /**
     * Does the scale range exclude all scales.
     */
    public boolean isEmpty() {
        return largeDenom==smallDenom;
    }
    
    /**
     * Does the scale range include all scales.
     */
    public boolean isUniversal() {
        return largeDenom==0 && smallDenom==Double.POSITIVE_INFINITY;
    }
    
    @Override
    public String toString() {
        return String.format("[1:%d, 1:%d)", largeDenom, smallDenom);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(largeDenom);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(smallDenom);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof ScaleRange))
            return false;
        ScaleRange other = (ScaleRange) obj;
        if (Double.doubleToLongBits(largeDenom) != Double
                .doubleToLongBits(other.largeDenom))
            return false;
        if (Double.doubleToLongBits(smallDenom) != Double
                .doubleToLongBits(other.smallDenom))
            return false;
        return true;
    }
    
    
}
