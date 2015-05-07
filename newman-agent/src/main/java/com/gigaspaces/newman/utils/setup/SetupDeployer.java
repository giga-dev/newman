package com.gigaspaces.newman.utils.setup;

/**
 * Created by Barak Bar Orion
 * 5/7/15.
 */
public interface SetupDeployer<P, S> {

     S deploy(P properties);

    void undeploy(S setup);
}
