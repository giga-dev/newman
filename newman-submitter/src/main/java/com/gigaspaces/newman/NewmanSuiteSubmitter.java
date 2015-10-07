package com.gigaspaces.newman;

import com.gigaspaces.newman.beans.CapabilitiesAndRequirements;
import com.gigaspaces.newman.beans.Suite;
import com.gigaspaces.newman.beans.criteria.*;
import com.gigaspaces.newman.utils.EnvUtils;
import com.gigaspaces.newman.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import static com.gigaspaces.newman.beans.criteria.CriteriaBuilder.exclude;
import static com.gigaspaces.newman.beans.criteria.CriteriaBuilder.include;
import static com.gigaspaces.newman.utils.StringUtils.getNonEmptySystemProperty;
import static com.gigaspaces.newman.utils.StringUtils.notEmpty;

/**
 * Suite submitter using environment variables
 */
public class NewmanSuiteSubmitter {
    private static final Logger logger = LoggerFactory.getLogger(NewmanSuiteSubmitter.class);

    //connection env variables
    public static final String NEWMAN_HOST = "NEWMAN_HOST";
    public static final String NEWMAN_PORT = "NEWMAN_PORT";
    public static final String NEWMAN_USER_NAME = "NEWMAN_USER_NAME";
    public static final String NEWMAN_PASSWORD = "NEWMAN_PASSWORD";

    //suite env variables
    public static final String NEWMAN_SUITE_NAME = "NEWMAN_SUITE_NAME";
    public static final String NEWMAN_SUITE_CUSTOM_VARIABLES = "NEWMAN_SUITE_CUSTOM_VARIABLES";
    public static final String NEWMAN_CRITERIA_TEST_TYPE = "NEWMAN_CRITERIA_TEST_TYPE";
    public static final String NEWMAN_CRITERIA_INCLUDE_LIST = "NEWMAN_CRITERIA_INCLUDE_LIST";
    public static final String NEWMAN_CRITERIA_EXCLUDE_LIST = "NEWMAN_CRITERIA_EXCLUDE_LIST";
    public static final String NEWMAN_CRITERIA_PERMUTATION_URI = "NEWMAN_CRITERIA_PERMUTATION_URI";
    private static final String NEWMAN_AGENT_DEFAULT_REQUIREMENTS = "";
    public static final String NEWMAN_SUITE_REQUIREMENTS = "newman.suite.requirements";
    /**
     * All input is done using environment variables.
     * @param args none are expected
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        NewmanSuiteSubmitter submitter = new NewmanSuiteSubmitter();
        submitter.submit();
    }

    public void submit() throws Exception {
        NewmanClient newmanClient = getNewmanClient();
        try {
            Suite suite = new Suite();
            suite.setName(EnvUtils.getEnvironment(NEWMAN_SUITE_NAME, true /*required*/, logger));
            suite.setCustomVariables(EnvUtils.getEnvironment(NEWMAN_SUITE_CUSTOM_VARIABLES, false, logger));
            suite.setCriteria(getNewmanSuiteCriteria());
            // TODO note - if set is empty, mongodb will NOT write that filed to DB
            suite.setRequirements(CapabilitiesAndRequirements.parse(EnvUtils.getEnvironment(NEWMAN_SUITE_REQUIREMENTS, false /*required*/, logger)));

            logger.info("Adding suite: " + suite);
            Suite result = newmanClient.addSuite(suite).toCompletableFuture().get();
            logger.info("result: " + result);

        } finally {
            newmanClient.close();
        }
    }

    public void manualSubmitJetty9() throws Exception {
        NewmanClient newmanClient = getNewmanClient();
        try {
            Suite suite = new Suite();
            suite.setName("jetty9");
            suite.setCustomVariables("SUITE_TYPE=sgtest,CUSTOM_SETUP_TIMEOUT=1800000,THREADS_LIMIT=1,JETTY_VERSION=9");
            // TODO note - if set is empty, mongodb will NOT write that filed to DB
            suite.setRequirements(CapabilitiesAndRequirements.parse(EnvUtils.getEnvironment(NEWMAN_SUITE_REQUIREMENTS, false /*required*/, logger)));
            String testType = "sgtest";

            Criteria criteria = CriteriaBuilder.join(
                    CriteriaBuilder.include(
                            PatternCriteria.containsCriteria(".PUInstanceLifeCycleTest#"),
                            PatternCriteria.containsCriteria(".PUStatusChangesTest#"),
                            PatternCriteria.containsCriteria(".PureSpringMVCWebAppSharedModeTest#"),
                            PatternCriteria.containsCriteria(".PureSpringMVCWebAppTest#"),
                            PatternCriteria.containsCriteria(".PureSpringMVCWithEmbeddedSpaceWebAppTest#"),
                            PatternCriteria.containsCriteria(".PureSpringMVCWithRemoteSpaceWebAppTest#")),
                    TestCriteria.createCriteriaByTestType(testType)

            );
            suite.setCriteria(criteria);
            logger.info("Adding suite: " + suite);
            Suite result = newmanClient.addSuite(suite).toCompletableFuture().get();
            logger.info("result: " + result);
        }
        finally {
            newmanClient.close();
        }
    }

    public void manualSubmitTgrid() throws Exception {
        NewmanClient newmanClient = getNewmanClient();
        try {
            Suite suite = new Suite();
            suite.setName("xap-core");
            suite.setCustomVariables("SUITE_TYPE=tgrid");
            // TODO note - if set is empty, mongodb will NOT write that filed to DB
            suite.setRequirements(CapabilitiesAndRequirements.parse(EnvUtils.getEnvironment(NEWMAN_SUITE_REQUIREMENTS, false /*required*/, logger)));
            String testType = "tgrid";
            Criteria criteria = CriteriaBuilder.join(
                    CriteriaBuilder.include(TestCriteria.createCriteriaByTestType(testType)),
                    CriteriaBuilder.exclude(
                            PatternCriteria.containsCriteria("com.gigaspaces.test.database.sql.Performance"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.cluster.replication.oneway_replication.OnewayMultithreaded"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.multicast"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.tg"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.stress"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.async.AsyncExtensionTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.blobstore.zetascale"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.blobstore.disableoffheap"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.blobstore.ssdspacemock"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.blobstore.mapdb"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.dcache.Extends"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.transaction.ConcurrentTxnTest")
                    )
            );
            suite.setCriteria(criteria);
            logger.info("Adding suite: " + suite);
            Suite result = newmanClient.addSuite(suite).toCompletableFuture().get();
            logger.info("result: " + result);
        }
        finally {
            newmanClient.close();
        }
    }

    public void manualSubmitSGTestWan() throws Exception {
        NewmanClient newmanClient = getNewmanClient();
        try {
            Suite suite = new Suite();
            suite.setName("wan");
            suite.setCustomVariables("SUITE_TYPE=sgtest,CUSTOM_SETUP_TIMEOUT=1800000,THREADS_LIMIT=1");
            // TODO note - if set is empty, mongodb will NOT write that filed to DB
            suite.setRequirements(CapabilitiesAndRequirements.parse(EnvUtils.getEnvironment(NEWMAN_SUITE_REQUIREMENTS, false /*required*/, logger)));
            Criteria criteria = CriteriaBuilder.join(
                    CriteriaBuilder.include(
                            PatternCriteria.containsCriteria("test.gateway")),
                    CriteriaBuilder.exclude(
                            PatternCriteria.recursivePackageNameCriteria("test.gateway.network_manipulation.latency")),
                    TestCriteria.createCriteriaByTestType("sgtest")

            );
            suite.setCriteria(criteria);
            logger.info("Adding suite: " + suite);
            Suite result = newmanClient.addSuite(suite).toCompletableFuture().get();
            logger.info("result: " + result);
        }
        finally {
            newmanClient.close();
        }
    }

    public void manualSubmitDisconnect() throws Exception {
        NewmanClient newmanClient = getNewmanClient();
        try {
            Suite suite = new Suite();
            suite.setName("disconnect");
            suite.setCustomVariables("SUITE_TYPE=sgtest,CUSTOM_SETUP_TIMEOUT=1800000,THREADS_LIMIT=1");
            // TODO note - if set is empty, mongodb will NOT write that filed to DB
            suite.setRequirements(CapabilitiesAndRequirements.parse(EnvUtils.getEnvironment(NEWMAN_SUITE_REQUIREMENTS, false /*required*/, logger)));
            Criteria criteria = CriteriaBuilder.join(
                    CriteriaBuilder.include(
                            PatternCriteria.containsCriteria("test.disconnect"),
                            PatternCriteria.containsCriteria("test.gateway.network_manipulation.latency")),
                    TestCriteria.createCriteriaByTestType("sgtest")

            );
            suite.setCriteria(criteria);
            logger.info("Adding suite: " + suite);
            Suite result = newmanClient.addSuite(suite).toCompletableFuture().get();
            logger.info("result: " + result);
        }
        finally {
            newmanClient.close();
        }
    }

    public void manualSubmitSGTestSecurity() throws Exception {
        NewmanClient newmanClient = getNewmanClient();
        try {
            Suite suite = new Suite();
            suite.setName("security");
            suite.setCustomVariables("SUITE_TYPE=sgtest,CUSTOM_SETUP_TIMEOUT=1800000,THREADS_LIMIT=1");
            // TODO note - if set is empty, mongodb will NOT write that filed to DB
            suite.setRequirements(CapabilitiesAndRequirements.parse(EnvUtils.getEnvironment(NEWMAN_SUITE_REQUIREMENTS, false /*required*/, logger)));
            Criteria criteria = CriteriaBuilder.join(
                    CriteriaBuilder.include(
                            PatternCriteria.containsCriteria("security")),
                    CriteriaBuilder.exclude(
                            PatternCriteria.recursivePackageNameCriteria("test.gsm.security"),
                            PatternCriteria.recursivePackageNameCriteria("test.gateway.security")),
                    TestCriteria.createCriteriaByTestType("sgtest")

            );
            suite.setCriteria(criteria);
            logger.info("Adding suite: " + suite);
            Suite result = newmanClient.addSuite(suite).toCompletableFuture().get();
            logger.info("result: " + result);
        }
        finally {
            newmanClient.close();
        }
    }

    public void manualSubmitSGTestEsm() throws Exception {
        NewmanClient newmanClient = getNewmanClient();
        try {
            Suite suite = new Suite();
            suite.setName("elastic-service-manager");
            suite.setCustomVariables("SUITE_TYPE=sgtest,CUSTOM_SETUP_TIMEOUT=1800000,THREADS_LIMIT=1");
            // TODO note - if set is empty, mongodb will NOT write that filed to DB
            suite.setRequirements(CapabilitiesAndRequirements.parse(EnvUtils.getEnvironment(NEWMAN_SUITE_REQUIREMENTS, false /*required*/, logger)));
            Criteria criteria = CriteriaBuilder.join(
                    CriteriaBuilder.include(
                            PatternCriteria.recursivePackageNameCriteria("test.gsm")),
                    CriteriaBuilder.exclude(
                            PatternCriteria.recursivePackageNameCriteria("test.gsm.security")),
                    TestCriteria.createCriteriaByTestType("sgtest")

            );
            suite.setCriteria(criteria);
            logger.info("Adding suite: " + suite);
            Suite result = newmanClient.addSuite(suite).toCompletableFuture().get();
            logger.info("result: " + result);
        }
        finally {
            newmanClient.close();
        }
    }

    public void manualSubmitSGTestEsmSecurity() throws Exception {
        NewmanClient newmanClient = getNewmanClient();
        try {
            Suite suite = new Suite();
            suite.setName("elastic-service-manager-security");
            suite.setCustomVariables("SUITE_TYPE=sgtest,CUSTOM_SETUP_TIMEOUT=1800000,THREADS_LIMIT=1");
            // TODO note - if set is empty, mongodb will NOT write that filed to DB
            suite.setRequirements(CapabilitiesAndRequirements.parse(EnvUtils.getEnvironment(NEWMAN_SUITE_REQUIREMENTS, false /*required*/, logger)));
            Criteria criteria = CriteriaBuilder.join(
                    CriteriaBuilder.include(
                            PatternCriteria.recursivePackageNameCriteria("test.gsm.security")),
                    TestCriteria.createCriteriaByTestType("sgtest")

            );
            suite.setCriteria(criteria);
            logger.info("Adding suite: " + suite);
            Suite result = newmanClient.addSuite(suite).toCompletableFuture().get();
            logger.info("result: " + result);
        }
        finally {
            newmanClient.close();
        }
    }


    public void manualSubmitServiceGrid() throws Exception {
        NewmanClient newmanClient = getNewmanClient();
        try {
            Suite suite = new Suite();
            suite.setName("service-grid");
            suite.setCustomVariables("SUITE_TYPE=sgtest,THREADS_LIMIT=1,CUSTOM_SETUP_TIMEOUT=1800000");
            // TODO note - if set is empty, mongodb will NOT write that filed to DB
            suite.setRequirements(CapabilitiesAndRequirements.parse(EnvUtils.getEnvironment(NEWMAN_SUITE_REQUIREMENTS, false /*required*/, logger)));
            Criteria criteria = CriteriaBuilder.join(TestCriteria.createCriteriaByTestType("sgtest"),
                    exclude(PatternCriteria.containsCriteria(".cloudify."),
                            PatternCriteria.containsCriteria(".security."),
                            PatternCriteria.containsCriteria(".webui."),
                            PatternCriteria.containsCriteria(".gsm."),
                            PatternCriteria.containsCriteria(".wan."),
                            PatternCriteria.containsCriteria(".gateway."),
                            PatternCriteria.containsCriteria(".xen."),
                            PatternCriteria.containsCriteria(".usm."),
                            PatternCriteria.containsCriteria(".cpp."),
                            PatternCriteria.containsCriteria(".disconnect."),
                            PatternCriteria.containsCriteria(".lookup.locators.dynamic."),
                            PatternCriteria.containsCriteria(".DBShutdownTest#"),
                            PatternCriteria.containsCriteria(".ClusterAndMirrorDeploymentWhileDBisDownTest#"),
                            PatternCriteria.containsCriteria(".EdsDocClassNotInCLientTest#")
                            //---------------------------------- SSHUtils ------------------------------
                            //                      containsCriteria(".examples."),
                            //                      containsCriteria(".groovy."),
                            //                      containsCriteria(".httpsession."),
                            //                      containsCriteria(".maven."),
                            //                      containsCriteria(".mongoEDS."),
                            //                      containsCriteria(".scripts."),
                            //                      containsCriteria(".servicegrid.initialLoadQuery."),
                            //                      containsCriteria(".servicegrid.metrics."),
                            //                      containsCriteria(".web.jboss.")
                    )


            );
            suite.setCriteria(criteria);
            logger.info("Adding suite: " + suite);
            Suite result = newmanClient.addSuite(suite).toCompletableFuture().get();
            logger.info("result: " + result);
        }
        finally {
            newmanClient.close();
        }
    }

    public void manualSubmitTgridMapdb() throws Exception {
        NewmanClient newmanClient = getNewmanClient();
        try {
            Suite suite = new Suite();
            suite.setName("map-db");
            suite.setCustomVariables("SUITE_TYPE=tgrid,TGRID_CUSTOM_SYSTEM_PROPS=-Dblobstore.persistent=false -Dblobstore.entriespercentage=0 -Dcom.gigaspaces.quality.tf.mapdb-blobstore.enabled=true");
            // TODO note - if set is empty, mongodb will NOT write that filed to DB
            suite.setRequirements(CapabilitiesAndRequirements.parse(EnvUtils.getEnvironment(NEWMAN_SUITE_REQUIREMENTS, false /*required*/, logger)));
            String testType = "tgrid";
            String mapdbExcludePermutationFile = "file:///home/boris/dev/sources/git/xap/tests/sanity/mapdb_excluded_permutations.txt";
            Criteria criteria = CriteriaBuilder.join(
                    CriteriaBuilder.include(TestCriteria.createCriteriaByTestType(testType)),
                    CriteriaBuilder.exclude(
                            PatternCriteria.containsCriteria("com.gigaspaces.test.database.sql.Performance"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.cluster.replication.oneway_replication.OnewayMultithreaded"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.multicast"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.tg"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.stress"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.async.AsyncExtensionTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.blobstore.zetascale"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.blobstore.disableoffheap"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.blobstore.ssdspacemock"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.dcache.Extends"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.transaction.ConcurrentTxnTest")
                    ),
                    CriteriaBuilder.exclude(getTestCriteriasFromPermutationURI(mapdbExcludePermutationFile))
            );
            suite.setCriteria(criteria);
            logger.info("Adding suite: " + suite);
            Suite result = newmanClient.addSuite(suite).toCompletableFuture().get();
            logger.info("result: " + result);
        }
        finally {
            newmanClient.close();
        }
    }

    public void manualSubmitTgridRocksDB() throws Exception {
        NewmanClient newmanClient = getNewmanClient();
        try {
            Suite suite = new Suite();
            suite.setName("rocksdb");
            suite.setCustomVariables("SUITE_TYPE=tgrid,TGRID_CUSTOM_SYSTEM_PROPS=-Dblobstore.persistent=true -Dblobstore.entriespercentage=0 -Dcom.gigaspaces.quality.tf.rocksdb-blobstore.enabled=true");
            // TODO note - if set is empty, mongodb will NOT write that filed to DB
            suite.setRequirements(CapabilitiesAndRequirements.parse(EnvUtils.getEnvironment(NEWMAN_SUITE_REQUIREMENTS, false /*required*/, logger)));
            String testType = "tgrid";
            String mapdbExcludePermutationFile = "file:///home/kobi/dev/github/xap/tests/sanity/full-blobstore.txt";
            Criteria criteria = CriteriaBuilder.join(
                    CriteriaBuilder.include(TestCriteria.createCriteriaByTestType(testType)),
                    CriteriaBuilder.exclude(
                            PatternCriteria.containsCriteria("com.gigaspaces.test.database.sql.Performance"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.cluster.replication.oneway_replication.OnewayMultithreaded"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.multicast"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.tg"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.stress"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.async.AsyncExtensionTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.blobstore.zetascale"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.blobstore.disableoffheap"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.blobstore.ssdspacemock"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.dcache.Extends"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.transaction.ConcurrentTxnTest")
                    ),
                    CriteriaBuilder.include(getTestCriteriasFromPermutationURI(mapdbExcludePermutationFile))
            );
            suite.setCriteria(criteria);
            logger.info("Adding suite: " + suite);
            Suite result = newmanClient.addSuite(suite).toCompletableFuture().get();
            logger.info("result: " + result);
        }
        finally {
            newmanClient.close();
        }
    }

    public void manualSubmitTgridOffHeap() throws Exception {
        NewmanClient newmanClient = getNewmanClient();
        try {
            Suite suite = new Suite();
            suite.setName("off-heap");
            suite.setCustomVariables("SUITE_TYPE=tgrid,TGRID_CUSTOM_SYSTEM_PROPS=-Dcom.gs.OffHeapData=true -Dcom.gs.OffHeapDataNewInterface=true -Dcom.gs.DirectPersistencyLastPrimaryStatePath=./output/lastprimary.properties");
            // TODO note - if set is empty, mongodb will NOT write that filed to DB
            suite.setRequirements(CapabilitiesAndRequirements.parse(EnvUtils.getEnvironment(NEWMAN_SUITE_REQUIREMENTS, false /*required*/, logger)));
            String testType = "tgrid";
            Criteria criteria = CriteriaBuilder.join(
                    CriteriaBuilder.include(TestCriteria.createCriteriaByTestType(testType)),
                    CriteriaBuilder.exclude(
                            PatternCriteria.containsCriteria("com.gigaspaces.test.database.sql.Performance"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.cluster.replication.oneway_replication.OnewayMultithreaded"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.multicast"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.tg"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.stress"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.async.AsyncExtensionTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.blobstore.zetascale"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.blobstore.mapdb"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.async.AsyncExtensionTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.basic.EvictOnlyLRUSpaceTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.basic.RecentUpdatesTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.basic.UIDMatchTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.cacheloader"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.transaction.LRUMultiThreadedTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.unique_constraint.UniqueConstraintLRUNegativeTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.view.local.EDSLRULocalViewReliabilityFailOverTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.view.local.EDSLRULocalViewReliabilityTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.lru"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.cluster.replication.reliable_async"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.change.persistency.PersistencyLruChangeAutoGenFalseTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.change.persistency.PersistencyLruChangeAutoGenTrueTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.cluster.failover.TransactionFailOverLoadWithMirror"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.cluster.lb.LB_primaryBackup_WarmInitTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.cluster.recovery.PrimaryBackupClusterRecovery"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.cluster.replication.basic_replication.LocalViewSpaceWithMirrorRemoveTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.cluster.replication.primary_backup.DropStayBlockedTargetInSync"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.writeMultipleTimeout.mirror.WriteMultipleTimeoutMirror"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.transaction.TransactionMirrorBulkSizeTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.support.fs.case6483.ReadByIdObjectNotInSpaceTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.support.commerzebankcase7833.DirectPersistencyLruCentralDistTransactionTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.support.cabank.case6613.PartialUpdateTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.support.cabank.case6500.QueryParamTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.support.betamedia.case8555.LeaseCancelAfterEvictTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.support.betamedia.case8439.ChangeSwapAndMirrorTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.support.barclays.LruLeaseTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.support.barclays.Case8259"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.support.SocieteGenerale.Case4232.SimpleReader"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.space_filter.Filters"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.space_api.GetRuntimeInfoTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.redolog.RedoLogMonitorBlockMirrorDropBackupTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.persistent.UnsafeWriteUpdateOnlyTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.persistent.BasicUnsafeWriteTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.persistent.BasicUnsafeWriteMultipleTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.persistency.sharediterator.SharedIteratorModeEDSTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.persistency.cassandra.SpaceCassandraPojoInitalLoadTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.persistency.cassandra.SpaceCassandraLoadTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.persistency.cassandra.SpaceCassandraDocumentInitalLoadTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.persistency.cassandra"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.mem_usage.MemoryManagerReadThruTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.mem_usage.BackupMemoryManagerTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.lease"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.change.basic.ChangeMirrorSpecificSupportTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.cluster.replication.swap.SwapRedoLogNotifyTemplateTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.cluster.replication.sync_replication.FailOverBackupTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.database.sql.SQLQueryEdsCollectionQuery"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.datasource.MissingTypeDescMirrorDelegateToSyncEndpointTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.datasource.SynchronizationStorageAdapterInitialMetadataLoadTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.datasource.SynchronizationStorageAdapterSyncTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.datasource.SynchronizationStorageAdapterTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.fifo.LRUFifoTransientTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.indexing.DynamicIndexInheritanceLRUPersistentTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.indexing.IndexUpdateEDSTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.indexing.UniqueIndexBasicTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.cleanup.MemoryCleanupAfterShutdown"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.blobstore.zetascale"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.clear_by_id.ClearByIdsExceptionTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.indexing.IllegalIndexValueChangeTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.dcache.Extends"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.transaction.ConcurrentTxnTest")
                    )
            );
            suite.setCriteria(criteria);
            logger.info("Adding suite: " + suite);
            Suite result = newmanClient.addSuite(suite).toCompletableFuture().get();
            logger.info("result: " + result);
        }
        finally {
            newmanClient.close();
        }
    }

    public void manualSubmitHTTPSession() throws Exception {
        NewmanClient newmanClient = getNewmanClient();
        try {
            Suite suite = new Suite();
            suite.setName("http-session");
            suite.setCustomVariables("SUITE_TYPE=httpsession,JAVA_VERSION=7");
            // TODO note - if set is empty, mongodb will NOT write that filed to DB
            suite.setRequirements(CapabilitiesAndRequirements.parse(EnvUtils.getEnvironment(NEWMAN_SUITE_REQUIREMENTS, false /*required*/, logger)));
            String testType = "httpsession";
            Criteria criteria = CriteriaBuilder.include(TestCriteria.createCriteriaByTestType(testType));
            suite.setCriteria(criteria);
            logger.info("Adding suite: " + suite);
            Suite result = newmanClient.addSuite(suite).toCompletableFuture().get();
            logger.info("result: " + result);
        }
        finally {
            newmanClient.close();
        }
    }
    public void manualSubmitMongoDb() throws Exception {
        NewmanClient newmanClient = getNewmanClient();
        try {
            Suite suite = new Suite();
            suite.setName("mongo-db");
            suite.setCustomVariables("SUITE_TYPE=mongodb");
            // TODO note - if set is empty, mongodb will NOT write that filed to DB
            suite.setRequirements(CapabilitiesAndRequirements.parse(EnvUtils.getEnvironment(NEWMAN_SUITE_REQUIREMENTS, false /*required*/, logger)));
            String testType = "mongodb";
            Criteria criteria = CriteriaBuilder.include(TestCriteria.createCriteriaByTestType(testType));
            suite.setCriteria(criteria);
            logger.info("Adding suite: " + suite);
            Suite result = newmanClient.addSuite(suite).toCompletableFuture().get();
            logger.info("result: " + result);
        }
        finally {
            newmanClient.close();
        }
    }

    /**
     * example:
     *   String fullBlobstorePath = "file:///home/tamirs-pcu/my_xap/xap/tests/sanity/full-blobstore.txt"
     *   String excludeSSDTestsPath= "/home/tamirs-pcu/my_xap/xap/tests/sanity/ssd_excluded_tests.txt";
     */
    public void manualSubmitSSD(String fullBlobstorePath, String excludeSSDTestsPath) throws Exception {
        NewmanClient newmanClient = getNewmanClient();
        try {
            Suite suite = new Suite();
            suite.setName("test-SSD");
            suite.setCustomVariables("SUITE_TYPE=tgrid,THREADS_LIMIT=1,TGRID_CUSTOM_SYSTEM_PROPS=-Dcom.gigaspaces.quality.tf.blobstore.enabled=true " +
                    "-Dblobstore.capacityGB=150 -Dblobstore.cache.capacityMB=50 -Dblobstore.devices=\"[/dev/sdb1,/dev/sdb2,/dev/sdc1,/dev/sdc2]\" " +
                    "-Dblobstore.persistent=true -Dblobstore.writethru=true -Dblobstore.entriespercentage=0 -Dcom.gs.blobstore-devices=/tmp/blobstore " +
                    "-Dcom.gs.blobstore.license.path=/export/tgrid/TestingGrid-latest/libfdf/fdf-license.txt -Dcom.gs.enabled-backward-space-lifecycle-admin=true");
            String req = getNonEmptySystemProperty(NEWMAN_SUITE_REQUIREMENTS, NEWMAN_AGENT_DEFAULT_REQUIREMENTS);
            Set<String> requirements = CapabilitiesAndRequirements.parse(req);
            // TODO note - if set is empty, mongodb will NOT write that filed to DB
            suite.setRequirements(requirements);
            String testType = "tgrid";
            Criteria criteria = CriteriaBuilder.join(

                    CriteriaBuilder.include(TestCriteria.createCriteriaByTestType(testType)),
                    CriteriaBuilder.include(getTestCriteriasFromPermutationURI(fullBlobstorePath)),

                    CriteriaBuilder.exclude(getCriteriasFromExcludedFile(excludeSSDTestsPath)),
                    CriteriaBuilder.exclude(
                            ArgumentsCriteria.containsCriteria("schema=persistent_eds"),
                            ArgumentsCriteria.containsCriteria("total_members=8"),
                            ArgumentsCriteria.containsCriteria("total_members=2,2"),
                            ArgumentsCriteria.containsAllCriteria("total_members=1,1", "embedded"),
                            ArgumentsCriteria.containsAllCriteria("total_members=1,1", "hybrid")
                    ));

            suite.setCriteria(criteria);
            logger.info("Adding suite: " + suite);
            Suite result = newmanClient.addSuite(suite).toCompletableFuture().get();
            logger.info("result: " + result);
        }
        finally {
            newmanClient.close();
        }
    }

    private Criteria[] getCriteriasFromExcludedFile(String path){
        ArrayList<Criteria> criteria = new ArrayList<>();
        File file = new File(path);
        try {
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()){
                String line = scanner.nextLine();
                criteria.add(PatternCriteria.containsCriteria(line));
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        return criteria.toArray(new Criteria[criteria.size()]);
    }

    private static NewmanClient getNewmanClient() throws Exception {
        // connection arguments
        String host = EnvUtils.getEnvironment(NEWMAN_HOST, true /*required*/, logger);
        String port = EnvUtils.getEnvironment(NEWMAN_PORT, true /*required*/, logger);
        String username = EnvUtils.getEnvironment(NEWMAN_USER_NAME, true /*required*/, logger);
        String password = EnvUtils.getEnvironment(NEWMAN_PASSWORD, true /*required*/, logger);

        return NewmanClient.create(host, port, username, password);
    }

    private static Criteria[] parseCommaDelimitedList(String list) {
        if (list.length() == 0) return new Criteria[0];

        List<Criteria> listOfCriterias = new ArrayList<>();
        String[] split = list.trim().split(",");
        for (String criteria : split) {
            criteria = criteria.trim();
            if (notEmpty(criteria)) {
                listOfCriterias.add(PatternCriteria.containsCriteria(criteria));
            }
        }
        return listOfCriterias.toArray(new Criteria[listOfCriterias.size()]);
    }

    @SuppressWarnings("unchecked")
    private static Criteria[] getTestCriteriasFromPermutationURI(String permutationURI) throws Exception {
        List<Criteria> criterias = new ArrayList<>();
        InputStream is = null;
        try {
            is = URI.create(permutationURI).toURL().openStream();
            List<String> permutations = FileUtils.readTextFileLines(is);
            for (String permutation : permutations) {
                if (permutation.length() <= 1)
                    continue;
                if (permutation.trim().charAt(0) == '#')
                    continue;
                TestCriteria criteria = TestCriteria.createCriteriaByTestArgs(permutation.split(" "));
                criterias.add(criteria);
            }
        }
        finally {
            if (is != null) {
                is.close();
            }
        }
        return criterias.toArray(new Criteria[criterias.size()]);
    }

    public static Criteria getNewmanSuiteCriteria() throws Exception {
        List<Criteria> criterias = new ArrayList<>();

        String testType = EnvUtils.getEnvironment(NEWMAN_CRITERIA_TEST_TYPE, false /*required*/, logger);
        if (notEmpty(testType)) {
            criterias.add(TestCriteria.createCriteriaByTestType(testType));
        }

        String includeList = EnvUtils.getEnvironment(NEWMAN_CRITERIA_INCLUDE_LIST, false /*required*/, logger);
        if (notEmpty(includeList)) {
            criterias.add(include(parseCommaDelimitedList(includeList)));
        }

        String excludeList = EnvUtils.getEnvironment(NEWMAN_CRITERIA_EXCLUDE_LIST, false /*required*/, logger);
        if (notEmpty(excludeList)) {
            criterias.add(exclude(parseCommaDelimitedList(excludeList)));
        }

        String permutationURI = EnvUtils.getEnvironment(NEWMAN_CRITERIA_PERMUTATION_URI, false /*required*/, logger);
        if (notEmpty(permutationURI)) {
            criterias.add(include(getTestCriteriasFromPermutationURI(permutationURI)));
        }
        return CriteriaBuilder.join(criterias.toArray(new Criteria[criterias.size()]));
    }
}
