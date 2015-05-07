package com.gigaspaces.newman.utils.setup;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.junit.Assert.assertThat;

/**
 * Created by Barak Bar Orion
 * 5/7/15.
 */
public class SetupRepositoryTest {
    private ExecutorService executor;
    private MockDeployer mockDeployer;
    private SetupRepository<MockSetupProperties, MockSetup> repository;

    @Before
    public void setUp() throws Exception {
        executor = Executors.newCachedThreadPool();
        mockDeployer = new MockDeployer();
        repository = new SetupRepository<>(executor, mockDeployer);

    }

    @After
    public void tearDown() throws Exception {
        executor.shutdownNow();
    }

    @Test
    public void testReferenceCount() throws Throwable {
        MockSetupProperties foo = new MockSetupProperties("foo");
        MockSetup fooSetup1 = repository.acquire(foo);
        assertThat(mockDeployer.getDeployed(), hasKey(foo));

        MockSetup fooSetup2 = repository.acquire(foo);
        assertThat(fooSetup1, is(fooSetup2));

        CompletableFuture<Void> future = repository.release(foo);
        // At this point the repository does not contains foo, but the uninstall is schedule to run at the background.
        // if you wish to wait for after the uninstall finishes do future.get().
        future.get();
        assertThat(mockDeployer.getDeployed(), hasKey(foo));


        repository.release(foo).get();
        assertThat(mockDeployer.getDeployed(), not(hasKey(foo)));
    }

}