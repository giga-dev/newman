package com.gigaspaces.newman.utils.setup;

import java.util.concurrent.CompletableFuture;

/**
 * Created by Barak Bar Orion
 * 5/6/15.
 */
public class SetupHolder<S> {
    private final CompletableFuture<S> setup;
    private int referenceCount;

    public SetupHolder(CompletableFuture<S> setup) {
        this.setup = setup;
        this.referenceCount = 1;
    }

    public CompletableFuture<S> getSetup() {
        return setup;
    }

    public CompletableFuture<S> inc(){
        referenceCount += 1;
        return setup;
    }

    public int dec(){
        return --referenceCount;
    }

}
