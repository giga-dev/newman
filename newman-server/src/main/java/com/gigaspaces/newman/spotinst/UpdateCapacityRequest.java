package com.gigaspaces.newman.spotinst;

public class UpdateCapacityRequest {
    private Capacity capacity;

    public UpdateCapacityRequest() {
    }

    public UpdateCapacityRequest(int target) {
        this.capacity = new Capacity(target);
    }

    public Capacity getCapacity() {
        return capacity;
    }

    public void setCapacity(Capacity capacity) {
        this.capacity = capacity;
    }

    private class Capacity {
        private int target;

        public Capacity() {
        }

        public Capacity(int target) {
            this.target = target;
        }

        public int getTarget() {
            return target;
        }

        public void setTarget(int target) {
            this.target = target;
        }
    }
}
