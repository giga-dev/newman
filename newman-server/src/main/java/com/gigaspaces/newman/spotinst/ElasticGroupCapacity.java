package com.gigaspaces.newman.spotinst;

public class ElasticGroupCapacity {
    private Integer minimum;
    private Integer maximum;
    private Integer target;

    public ElasticGroupCapacity() {
    }

    public Integer getMinimum() {
        return minimum;
    }

    public void setMinimum(Integer minimum) {
        this.minimum = minimum;
    }

    public Integer getMaximum() {
        return maximum;
    }

    public void setMaximum(Integer maximum) {
        this.maximum = maximum;
    }

    public Integer getTarget() {
        return target;
    }

    public void setTarget(Integer target) {
        this.target = target;
    }


    @Override
    public String toString() {
        return "ElasticGroupCapacity{" +
                "minimum=" + minimum +
                ", maximum=" + maximum +
                ", target=" + target +
                '}';
    }
}
