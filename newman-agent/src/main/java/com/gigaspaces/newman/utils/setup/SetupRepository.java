package com.gigaspaces.newman.utils.setup;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Created by Barak Bar Orion
 * 5/6/15.
 */
public class SetupRepository<P, S> {

    private ExecutorService executor;
    private final SetupDeployer<P, S> deployer;
    private Map<P, SetupHolder<S>> setups;

    public SetupRepository(ExecutorService executor, SetupDeployer<P, S> deployer) {
        this.executor = executor;
        this.deployer = deployer;
        this.setups = new HashMap<>();
    }

    public S acquire(P setupProperties) throws Throwable {
        try {
            return getSetup(setupProperties).get();
        } catch (Exception e) {
            remove(setupProperties);
            throw e.getCause();
        }
    }

    public synchronized CompletableFuture<Void> release(P setupProperties) {
        SetupHolder<S> holder = setups.get(setupProperties);
        if (holder != null) {
            if (holder.dec() == 0) {
                setups.remove(setupProperties);
                return holder.getSetup().thenAcceptAsync(this::uninstallSetup, executor);
            }
        }
        return CompletableFuture.completedFuture(null);
    }


    private synchronized CompletableFuture<S> getSetup(P setupProperties) {
        SetupHolder<S> holder = setups.get(setupProperties);
        if (holder != null) {
            holder.inc();
            return holder.getSetup();
        } else {
            CompletableFuture<S> future = CompletableFuture.supplyAsync(() -> installSetup(setupProperties), executor);
            setups.put(setupProperties, new SetupHolder<>(future));
            return future;
        }
    }


    private synchronized void remove(P setupProperties) {
        setups.remove(setupProperties);
    }

    private S installSetup(P setupDescription) {
        return deployer.deploy(setupDescription);
    }

    private void uninstallSetup(S setup) {
        deployer.undeploy(setup);
    }

}
