package org.geoserver.jdbcconfig.internal;

import javax.annotation.Nullable;

import com.google.common.base.Joiner;

public class Dialect {

    public void applyOffsetLimit(StringBuilder sql, @Nullable Integer offset,
            @Nullable Integer limit) {
        // some db's require limit to be present of offset is
        if(offset != null && limit == null){
            limit = Integer.MAX_VALUE;
        }
        if (limit != null) {
            sql.append(" limit ").append(limit);
        }
        if (offset != null) {
            sql.append(" offset ").append(offset);
        }
    }

    public CharSequence propertyName(String propertyName) {
        return Joiner.on("").join(identifierQualifier(), propertyName, identifierQualifier());
    }

    private String identifierQualifier() {
        return "";
    }

    public CharSequence iLikeArgument(CharSequence subsequence) {
        return Joiner.on("").join("%", String.valueOf(subsequence).toLowerCase(), "%");
    }

    public CharSequence iLikeNamedPreparedConstruct(String attributeName, String valueParam) {
        return Joiner.on("").join("lower(", propertyName(attributeName), ") like :", valueParam);
    }
}
