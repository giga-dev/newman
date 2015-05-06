package com.gigaspaces.newman.utils.setup;

import java.util.concurrent.CompletableFuture;

/**
 * Created by Barak Bar Orion
 * 5/6/15.
 */
public class SetupHolder {
    private final CompletableFuture<Setup> setup;
    private int referenceCount;

    public SetupHolder(CompletableFuture<Setup> setup) {
        this.setup = setup;
        this.referenceCount = 1;
    }

    public CompletableFuture<Setup> getSetup() {
        return setup;
    }

    public CompletableFuture<Setup> inc(){
        referenceCount += 1;
        return setup;
    }

    public int dec(){
        return --referenceCount;
    }

}
