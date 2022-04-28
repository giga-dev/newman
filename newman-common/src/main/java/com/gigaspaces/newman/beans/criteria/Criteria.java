package com.gigaspaces.newman.beans.criteria;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Created by Barak Bar Orion
 * 5/4/15.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AndCriteria.class),
        @JsonSubTypes.Type(value = OrCriteria.class),
        @JsonSubTypes.Type(value = NotCriteria.class),
        @JsonSubTypes.Type(value = PatternCriteria.class),
        @JsonSubTypes.Type(value = PropertyCriteria.class),
        @JsonSubTypes.Type(value = TestCriteria.class),
        @JsonSubTypes.Type(value = SuiteCriteria.class),
})
public interface Criteria {
}
