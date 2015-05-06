package com.gigaspaces.newman.utils.setup;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Created by Barak Bar Orion
 * 5/6/15.
 */
public class SetupRepository {

    private ExecutorService executor;
    private Map<String, SetupHolder> setups;

    public SetupRepository(ExecutorService executor) {
        this.executor = executor;
        this.setups = new HashMap<>();
    }

    public Setup acquire(String name) throws Throwable {
        try {
            return getSetup(name).get();
        } catch (Exception e) {
            remove(name);
            throw e.getCause();
        }
    }

    public synchronized void release(String name) {
        SetupHolder holder = setups.get(name);
        if (holder != null) {
            if (holder.dec() == 0) {
                setups.remove(name);
                executor.submit(() ->  uninstallSetup(holder.getSetup()));
            }
        }
    }


    private synchronized CompletableFuture<Setup> getSetup(String name) {
        SetupHolder holder = setups.get(name);
        if (holder != null) {
            holder.inc();
            return holder.getSetup();
        } else {
            CompletableFuture<Setup> future = CompletableFuture.supplyAsync(() -> installSetup(name), executor);
            setups.put(name, new SetupHolder(future));
            return future;
        }
    }


    private synchronized void remove(String name) {
        setups.remove(name);
    }

    private Setup installSetup(String name) {
        // todo.
        return new Setup(name);
    }

    private void uninstallSetup(CompletableFuture<Setup> setup) {
        // todo.
    }

}
