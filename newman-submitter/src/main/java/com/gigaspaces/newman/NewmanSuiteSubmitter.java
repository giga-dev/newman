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

import static com.gigaspaces.newman.beans.criteria.CriteriaBuilder.exclude;
import static com.gigaspaces.newman.beans.criteria.CriteriaBuilder.include;
import static com.gigaspaces.newman.utils.StringUtils.notEmpty;

/**
 * Suite submitter using environment variables
 */
public class NewmanSuiteSubmitter {
    private static final Logger logger = LoggerFactory.getLogger(NewmanSuiteSubmitter.class);

    //suite env variables
    public static final String NEWMAN_SUITE_NAME = "NEWMAN_SUITE_NAME";
    public static final String NEWMAN_SUITE_CUSTOM_VARIABLES = "NEWMAN_SUITE_CUSTOM_VARIABLES";
    public static final String NEWMAN_CRITERIA_TEST_TYPE = "NEWMAN_CRITERIA_TEST_TYPE";
    public static final String NEWMAN_CRITERIA_INCLUDE_LIST = "NEWMAN_CRITERIA_INCLUDE_LIST";
    public static final String NEWMAN_CRITERIA_EXCLUDE_LIST = "NEWMAN_CRITERIA_EXCLUDE_LIST";
    public static final String NEWMAN_CRITERIA_PERMUTATION_URI = "NEWMAN_CRITERIA_PERMUTATION_URI";
    public static final String NEWMAN_SUITE_REQUIREMENTS = "newman.suite.requirements";

    /**
     * All input is done using environment variables.
     * @param args none are expected
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        NewmanSuiteSubmitter submitter = new NewmanSuiteSubmitter();
//        submitter.submit();
        submitter.manualSubmitKubernetesInsightedge();
//        String ssd_tests = "file:///home/tamirs-pcu/my_xap/xap/tests/sanity/ssd_rocksdb_all_tests";
//        submitter.manualSubmitSSD(ssd_tests);
//        submitter.manualSubmitTgridRocksDB();
//        submitter.manualSubmitTgridMapdb();
//        submitter.manualSubmitTgridOffHeap();
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
            suite.setCustomVariables("SUITE_TYPE=sgtest,CUSTOM_SETUP_TIMEOUT=1800000,THREADS_LIMIT=1,JETTY_VERSION=9,WEBUI_CUSTOM_SYSTEM_PROPS=-Dselenium.browser=Firefox -Dcom.gs.test.use.newman=true");
            // TODO note - if set is empty, mongodb will NOT write that filed to DB
            String Requirements = "DOCKER,LINUX";
            suite.setRequirements(CapabilitiesAndRequirements.parse(Requirements));
            String testType = "sgtest";

            Criteria criteria = CriteriaBuilder.join(
                    CriteriaBuilder.include(
                            PatternCriteria.containsCriteria(".PUInstanceLifeCycleTest#"),
                            PatternCriteria.containsCriteria(".PUStatusChangesTest#"),
                            PatternCriteria.containsCriteria(".PureSpringMVCWebAppSharedModeTest#"),
                            PatternCriteria.containsCriteria(".PureSpringMVCWebAppTest#"),
                            PatternCriteria.containsCriteria(".PureSpringMVCWithEmbeddedSpaceWebAppTest#"),
                            PatternCriteria.containsCriteria(".PureSpringMVCWithRemoteSpaceWebAppTest#"),
                            PatternCriteria.containsCriteria(".JettyUnitTest#"),
                            PatternCriteria.containsCriteria("test.webui.security.BasicSslWebuiTest#"),
                            PatternCriteria.containsCriteria("test.webui.WebSSLJetty9Test#")),
                    TestCriteria.createCriteriaByTestType(testType)

            );
            suite.setCriteria(criteria);
            logger.info("Adding suite: " + suite);
            Suite result = newmanClient.addSuite(suite).toCompletableFuture().get();
            logger.info("result: " + result);
        } finally {
            newmanClient.close();
        }
    }

    public void manualSubmitCustomJetty9() throws Exception {
        NewmanClient newmanClient = getNewmanClient();
        try {
            Suite suite = new Suite();
            suite.setName("dev-meshi-jetty9");
            suite.setCustomVariables("SUITE_TYPE=sgtest,CUSTOM_SETUP_TIMEOUT=1800000,THREADS_LIMIT=1,JETTY_VERSION=9,WEBUI_CUSTOM_SYSTEM_PROPS=-Dselenium.browser=Firefox -Dcom.gs.test.use.newman=true");
            // TODO note - if set is empty, mongodb will NOT write that filed to DB
            String Requirements = "DOCKER,LINUX";
            suite.setRequirements(CapabilitiesAndRequirements.parse(Requirements));
            String testType = "sgtest";

            Criteria criteria = CriteriaBuilder.join(
                    CriteriaBuilder.include(
                            PatternCriteria.containsCriteria(".PUInstanceLifeCycleTest#"),
                            PatternCriteria.containsCriteria(".PUStatusChangesTest#"),
                            PatternCriteria.containsCriteria(".PureSpringMVCWebAppSharedModeTest#"),
                            PatternCriteria.containsCriteria(".PureSpringMVCWebAppTest#"),
                            PatternCriteria.containsCriteria(".PureSpringMVCWithEmbeddedSpaceWebAppTest#"),
                            PatternCriteria.containsCriteria(".PureSpringMVCWithRemoteSpaceWebAppTest#"),
                            PatternCriteria.containsCriteria(".JettyUnitTest#")),
                    TestCriteria.createCriteriaByTestType(testType)

            );
            suite.setCriteria(criteria);
            logger.info("Adding suite: " + suite);
            Suite result = newmanClient.addSuite(suite).toCompletableFuture().get();
            logger.info("result: " + result);
        } finally {
            newmanClient.close();
        }
    }

    public void manualSubmitTgrid() throws Exception {
        NewmanClient newmanClient = getNewmanClient();
        try {
            Suite suite = new Suite();
            suite.setName("xap-core");
            suite.setCustomVariables("SUITE_TYPE=tgrid,THREADS_LIMIT=2");
            // TODO note - if set is empty, mongodb will NOT write that filed to DB
            String Requirements = "LINUX";
            suite.setRequirements(CapabilitiesAndRequirements.parse(Requirements));
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
                            PatternCriteria.containsCriteria("com.gigaspaces.test.transaction.ConcurrentTxnTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.blobstore.rocksdb")
                    )
            );
            suite.setCriteria(criteria);
            logger.info("Adding suite: " + suite);
            Suite result = newmanClient.addSuite(suite).toCompletableFuture().get();
            logger.info("result: " + result);
        } finally {
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
            String Requirements = "DOCKER,LINUX";
            suite.setRequirements(CapabilitiesAndRequirements.parse(Requirements));
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
        } finally {
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
            String Requirements = "DOCKER,LINUX";
            suite.setRequirements(CapabilitiesAndRequirements.parse(Requirements));
            Criteria criteria = CriteriaBuilder.join(
                    CriteriaBuilder.include(
                            PatternCriteria.containsCriteria("test.disconnect"),
                            PatternCriteria.containsCriteria("test.gateway.network_manipulation.latency")),
                    TestCriteria.createCriteriaByTestType("sgtest"),
                    exclude(PatternCriteria.containsCriteria(".manager."))
            );
            suite.setCriteria(criteria);
            logger.info("Adding suite: " + suite);
            Suite result = newmanClient.addSuite(suite).toCompletableFuture().get();
            logger.info("result: " + result);
        } finally {
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
            String Requirements = "DOCKER,LINUX";
            suite.setRequirements(CapabilitiesAndRequirements.parse(Requirements));
            Criteria criteria = CriteriaBuilder.join(
                    CriteriaBuilder.include(PatternCriteria.containsCriteria("security")),
                    CriteriaBuilder.exclude(
                            PatternCriteria.containsCriteria("manager.security"),
                            PatternCriteria.recursivePackageNameCriteria("test.gsm.security"),
                            PatternCriteria.recursivePackageNameCriteria("test.gateway.security"),
                            PatternCriteria.recursivePackageNameCriteria("test.webui.security")),
                    TestCriteria.createCriteriaByTestType("sgtest")

            );
            suite.setCriteria(criteria);
            logger.info("Adding suite: " + suite);
            Suite result = newmanClient.addSuite(suite).toCompletableFuture().get();
            logger.info("result: " + result);
        } finally {
            newmanClient.close();
        }
    }

    public void manualSubmitSGTestEsm() throws Exception {
        NewmanClient newmanClient = getNewmanClient();
        try {
            Suite suite = new Suite();
            suite.setName("esm");
            suite.setCustomVariables("SUITE_TYPE=sgtest,CUSTOM_SETUP_TIMEOUT=1800000,THREADS_LIMIT=1");
            // TODO note - if set is empty, mongodb will NOT write that filed to DB
            String Requirements = "DOCKER,LINUX";
            suite.setRequirements(CapabilitiesAndRequirements.parse(Requirements));
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
        } finally {
            newmanClient.close();
        }
    }

    public void manualSubmitSGTestEsmSecurity() throws Exception {
        NewmanClient newmanClient = getNewmanClient();
        try {
            Suite suite = new Suite();
            suite.setName("esm-security");
            suite.setCustomVariables("SUITE_TYPE=sgtest,CUSTOM_SETUP_TIMEOUT=1800000,THREADS_LIMIT=1");
            // TODO note - if set is empty, mongodb will NOT write that filed to DB
            String Requirements = "DOCKER,LINUX";
            suite.setRequirements(CapabilitiesAndRequirements.parse(Requirements));
            Criteria criteria = CriteriaBuilder.join(
                    CriteriaBuilder.include(
                            PatternCriteria.recursivePackageNameCriteria("test.gsm.security")),
                    TestCriteria.createCriteriaByTestType("sgtest")

            );
            suite.setCriteria(criteria);
            logger.info("Adding suite: " + suite);
            Suite result = newmanClient.addSuite(suite).toCompletableFuture().get();
            logger.info("result: " + result);
        } finally {
            newmanClient.close();
        }
    }

    public void manualSubmitManager() throws Exception {
        NewmanClient newmanClient = getNewmanClient();
        try {
            Suite suite = new Suite();
            suite.setName("manager");
            suite.setCustomVariables("SUITE_TYPE=sgtest,SUITE_SUB_TYPE=manager,THREADS_LIMIT=1,CUSTOM_SETUP_TIMEOUT=1800000");
            // TODO note - if set is empty, mongodb will NOT write that filed to DB
            String Requirements = "DOCKER,LINUX";
            suite.setRequirements(CapabilitiesAndRequirements.parse(Requirements));
            Criteria criteria = CriteriaBuilder.join(TestCriteria.createCriteriaByTestType("sgtest"),
                    include(PatternCriteria.containsCriteria(".manager.")),
                    exclude(PatternCriteria.containsCriteria(".manager.rest.security."))
            );
            suite.setCriteria(criteria);
            logger.info("Adding suite: " + suite);
            Suite result = newmanClient.addSuite(suite).toCompletableFuture().get();
            logger.info("result: " + result);
        } finally {
            newmanClient.close();
        }
    }

    public void manualSubmitManagerSecurity() throws Exception {
        NewmanClient newmanClient = getNewmanClient();
        try {
            Suite suite = new Suite();
            suite.setName("manager-security");
            suite.setCustomVariables("SUITE_TYPE=sgtest,SUITE_SUB_TYPE=manager-security,THREADS_LIMIT=1,CUSTOM_SETUP_TIMEOUT=1800000");
            // TODO note - if set is empty, mongodb will NOT write that filed to DB
            String Requirements = "DOCKER,LINUX";
            suite.setRequirements(CapabilitiesAndRequirements.parse(Requirements));
            Criteria criteria = CriteriaBuilder.join(TestCriteria.createCriteriaByTestType("sgtest"),
                    include(PatternCriteria.containsCriteria(".manager.rest.security."))
            );
            suite.setCriteria(criteria);
            logger.info("Adding suite: " + suite);
            Suite result = newmanClient.addSuite(suite).toCompletableFuture().get();
            logger.info("result: " + result);
        } finally {
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
            String Requirements = "DOCKER,LINUX";
            suite.setRequirements(CapabilitiesAndRequirements.parse(Requirements));
            Criteria criteria = CriteriaBuilder.join(TestCriteria.createCriteriaByTestType("sgtest"),
                    exclude(PatternCriteria.containsCriteria(".cloudify."),
                            PatternCriteria.containsCriteria(".security."),
                            PatternCriteria.containsCriteria(".webui."),
                            PatternCriteria.containsCriteria("test.gsm."),
                            PatternCriteria.containsCriteria(".wan."),
                            PatternCriteria.containsCriteria(".gateway."),
                            PatternCriteria.containsCriteria(".xen."),
                            PatternCriteria.containsCriteria(".manager."),
                            PatternCriteria.containsCriteria(".usm."),
                            PatternCriteria.containsCriteria(".cpp."),
                            PatternCriteria.containsCriteria(".disconnect."),
                            PatternCriteria.containsCriteria(".lookup.locators.dynamic."),
                            PatternCriteria.containsCriteria(".DBShutdownTest#"),
                            PatternCriteria.containsCriteria(".ClusterAndMirrorDeploymentWhileDBisDownTest#"),
                            PatternCriteria.containsCriteria(".EdsDocClassNotInCLientTest#"),
                            PatternCriteria.containsCriteria(".web.jboss."),
                            PatternCriteria.containsCriteria("test.i9e.")
                            //---------------------------------- SSHUtils ------------------------------
                            //                      containsCriteria(".examples."),
                            //                      containsCriteria(".groovy."),
                            //                      containsCriteria(".httpsession."),
                            //                      containsCriteria(".maven."),
                            //                      containsCriteria(".mongoEDS."),
                            //                      containsCriteria(".scripts."),
                            //                      containsCriteria(".servicegrid.initialLoadQuery."),
                            //                      containsCriteria(".servicegrid.metrics."),
                    )


            );
            suite.setCriteria(criteria);
            logger.info("Adding suite: " + suite);
            Suite result = newmanClient.addSuite(suite).toCompletableFuture().get();
            logger.info("result: " + result);
        } finally {
            newmanClient.close();
        }
    }

    public void manualSubmitI9ESgtestCLI() throws Exception {
        NewmanClient newmanClient = getNewmanClient();
        try {
            Suite suite = new Suite();
            //suite.setName("i9e-sgtest");
            suite.setName("i9e-sgtest-cli");
            suite.setCustomVariables("SUITE_TYPE=sgtest,SUITE_SUB_TYPE=i9e-sgtest,THREADS_LIMIT=1,CUSTOM_SETUP_TIMEOUT=1800000");
            // TODO note - if set is empty, mongodb will NOT write that filed to DB
            String Requirements = "DOCKER,LINUX";
            suite.setRequirements(CapabilitiesAndRequirements.parse(Requirements));

            Criteria criteria = CriteriaBuilder.join(
                    CriteriaBuilder.include(PatternCriteria.containsCriteria("test.manager.cli")),
                    CriteriaBuilder.exclude(PatternCriteria.containsCriteria("test.manager.cli.security.FailedSecuredHostListCliTest")),
                    TestCriteria.createCriteriaByTestType("sgtest")
            );
            suite.setCriteria(criteria);
            logger.info("Adding suite: " + suite);
            Suite result = newmanClient.addSuite(suite).toCompletableFuture().get();
            logger.info("result: " + result);
        } finally {
            newmanClient.close();
        }
    }

    public void manualSubmitI9ESgtest() throws Exception {
        NewmanClient newmanClient = getNewmanClient();
        try {
            Suite suite = new Suite();
            suite.setName("i9e-sgtest");
            suite.setCustomVariables("SUITE_TYPE=sgtest,SUITE_SUB_TYPE=i9e-sgtest,THREADS_LIMIT=1,CUSTOM_SETUP_TIMEOUT=1800000");
            // TODO note - if set is empty, mongodb will NOT write that filed to DB
            String Requirements = "DOCKER,LINUX";
            suite.setRequirements(CapabilitiesAndRequirements.parse(Requirements));
            Criteria criteria = CriteriaBuilder.join(TestCriteria.createCriteriaByTestType("sgtest"),
                    include(PatternCriteria.containsCriteria("test.i9e."))
            );
            suite.setCriteria(criteria);
            logger.info("Adding suite: " + suite);
            Suite result = newmanClient.addSuite(suite).toCompletableFuture().get();
            logger.info("result: " + result);
        } finally {
            newmanClient.close();
        }
    }


    public void manualSubmitTgridMapdb() throws Exception {
        NewmanClient newmanClient = getNewmanClient();
        try {
            Suite suite = new Suite();
            suite.setName("map-db");
            suite.setCustomVariables("SUITE_TYPE=tgrid,THREADS_LIMIT=2,TGRID_CUSTOM_SYSTEM_PROPS=-Dblobstore.persistent=false -Dblobstore.entriespercentage=0 -Dcom.gigaspaces.quality.tf.mapdb-blobstore.enabled=true");
            // TODO note - if set is empty, mongodb will NOT write that filed to DB
            String Requirements = "LINUX";
            suite.setRequirements(CapabilitiesAndRequirements.parse(Requirements));
            String testType = "tgrid";
            String mapdbExcludePermutationFile = "file:///home/kobi/dev/github/xap-premium/xap-tests/xap-tests-datagrid/sanity/mapdb_excluded_permutations.txt";
            Criteria criteria = CriteriaBuilder.join(
                    CriteriaBuilder.include(TestCriteria.createCriteriaByTestType(testType)),
                    CriteriaBuilder.exclude(
                            PatternCriteria.containsCriteria("com.gigaspaces.test.database.sql.Performance"),
                            PatternCriteria.containsCriteria("org.openspaces.itest"),
                            PatternCriteria.containsCriteria("org.openspaces.test.rest.SpaceAPIControllerTest"),
                            PatternCriteria.containsCriteria("org.openspaces.test.core.map.simple"),
                            PatternCriteria.containsCriteria("org.openspaces.test.core.cluster.info.ClusterInfoAnnotationsTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.httpsession.serialize.KryoSerializerImpLargeTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.blobstore"),
                            PatternCriteria.containsCriteria("com.gigaspaces.persistency"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.cluster.replication.oneway_replication.OnewayMultithreaded"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.multicast"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.tg"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.stress"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.async.AsyncExtensionTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.blobstore.zetascale"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.blobstore.disableoffheap"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.blobstore.ssdspacemock"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.dcache.Extends"),
                            PatternCriteria.containsCriteria("com.gigaspaces.internal"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.transaction.ConcurrentTxnTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.support.avnza.case10089.ValueOfSqlQueryTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.support.avnza.case10089.ValueOfQueryTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.blobstore.rocksdb"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.support.ubs.case00011147.MultiIndexExplanPlanTest")
                    ),
                    CriteriaBuilder.exclude(getTestCriteriasFromPermutationURI(mapdbExcludePermutationFile))
            );
            suite.setCriteria(criteria);
            logger.info("Adding suite: " + suite);
            Suite result = newmanClient.addSuite(suite).toCompletableFuture().get();
            logger.info("result: " + result);
        } finally {
            newmanClient.close();
        }
    }

    public void manualSubmitTgridRocksDB() throws Exception {
        NewmanClient newmanClient = getNewmanClient();
        try {
            Suite suite = new Suite();
            suite.setName("rocksdb");
            suite.setCustomVariables("SUITE_TYPE=tgrid,THREADS_LIMIT=1,TGRID_CUSTOM_SYSTEM_PROPS=-Dblobstore.persistent=true -Dblobstore.entriespercentage=0 -Dcom.gigaspaces.quality.tf.rocksdb-blobstore.enabled=true");
            // TODO note - if set is empty, mongodb will NOT write that filed to DB
            String Requirements = "LINUX";
            suite.setRequirements(CapabilitiesAndRequirements.parse(Requirements));
            String testType = "tgrid";
            String fullPath = new File(NewmanSuiteSubmitter.class.getClassLoader().getResource("rocksDbPermutations.txt").getFile()).getAbsolutePath();
            Criteria criteria = CriteriaBuilder.join(
                    CriteriaBuilder.include(TestCriteria.createCriteriaByTestType(testType)),
                    CriteriaBuilder.include(getTestCriteriasFromPermutationURI("file://" + fullPath))
            );
            suite.setCriteria(criteria);
            logger.info("Adding suite: " + suite);
            Suite result = newmanClient.addSuite(suite).toCompletableFuture().get();
            logger.info("result: " + result);
        } finally {
            newmanClient.close();
        }
    }

    public void manualSubmitOneTestSuite() throws Exception {
        Suite suite = new Suite();
        suite.setName("mx-pmem");
        suite.setCustomVariables("SUITE_TYPE=tgrid,THREADS_LIMIT=1,TGRID_CUSTOM_SYSTEM_PROPS=-Dblobstore.persistent=false -Dblobstore.pmem.memory=1024M -Dblobstore.pmem.fileName=\"/tmp/testPmemPool.txt\" -Dblobstore.pmem.verbose=false -Dcom.gigaspaces.quality.tf.pmem-blobstore.enabled=true -Dblobstore.entriespercentage=0, LD_LIBRARY_PATH=~/.nix-profile/lib");
        String Requirements = "PMEM";
        suite.setRequirements(CapabilitiesAndRequirements.parse(Requirements));
        String testType = "tgrid";
        Criteria criteria = CriteriaBuilder.join(
                CriteriaBuilder.include(TestCriteria.createCriteriaByTestType(testType)),
                CriteriaBuilder.include(getTestCriteriasFromPermutationURI("file:///tmp/permutations.txt")));
        suite.setCriteria(criteria);
        logger.info("Adding suite: " + suite);
        NewmanClient newmanClient = getNewmanClient();
        Suite result = newmanClient.addSuite(suite).toCompletableFuture().get();
        logger.info("result: " + result);
        newmanClient.close();
    }

    public void manualSubmitTgridOffHeap() throws Exception {
        NewmanClient newmanClient = getNewmanClient();
        try {
            Suite suite = new Suite();
            suite.setName("off-heap");
            suite.setCustomVariables("SUITE_TYPE=tgrid,THREADS_LIMIT=1,TGRID_CUSTOM_SYSTEM_PROPS=-Dcom.gs.OffHeapData=true -Dcom.gs.OffHeapDataNewInterface=true -Dcom.gs.DirectPersistencyLastPrimaryStatePath=./output/lastprimary.properties");
            // TODO note - if set is empty, mongodb will NOT write that filed to DB
            String Requirements = "LINUX";
            suite.setRequirements(CapabilitiesAndRequirements.parse(Requirements));
            String testType = "tgrid";
            Criteria criteria = CriteriaBuilder.join(
                    CriteriaBuilder.include(TestCriteria.createCriteriaByTestType(testType)),
                    CriteriaBuilder.exclude(
                            PatternCriteria.containsCriteria("com.gigaspaces.test.database.sql.Performance"),
                            PatternCriteria.containsCriteria("org.openspaces.itest"),
                            PatternCriteria.containsCriteria("org.openspaces.test.rest.SpaceAPIControllerTest"),
                            PatternCriteria.containsCriteria("org.openspaces.test.core.map.simple"),
                            PatternCriteria.containsCriteria("org.openspaces.test.core.cluster.info.ClusterInfoAnnotationsTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.httpsession.serialize.KryoSerializerImpLargeTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.blobstore"),
                            PatternCriteria.containsCriteria("com.gigaspaces.persistency"),
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
                            PatternCriteria.containsCriteria("com.gigaspaces.internal"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.dcache.Extends"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.transaction.ConcurrentTxnTest"),
                            PatternCriteria.containsCriteria("org.openspaces.remoting.scripting.ScalaLocalScriptExecutorTest"),
                            PatternCriteria.containsCriteria("org.openspaces.scala.core.ScalaEnhancedGigaSpaceWrapperTest"),
                            PatternCriteria.containsCriteria("org.openspaces.scala.immutabledata.ScalaImmutableDataTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.unittest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.database.sql.AndOrScanTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.database.sql.ExplainPlanScanningInfoTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.database.sql.ExplainPlanIndexInfoTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.database.sql.ExplainPlanQueryTreeTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.blobstore.rocksdb"),
                            PatternCriteria.containsCriteria("test com.gigaspaces.test.support.ubs.case00011147.MultiIndexExplanPlanTest")
                    )
            );
            suite.setCriteria(criteria);
            logger.info("Adding suite: " + suite);
            Suite result = newmanClient.addSuite(suite).toCompletableFuture().get();
            logger.info("result: " + result);
        } finally {
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
            String Requirements = "DOCKER,LINUX";
            suite.setRequirements(CapabilitiesAndRequirements.parse(Requirements));
            String testType = "httpsession";
            Criteria criteria = CriteriaBuilder.include(TestCriteria.createCriteriaByTestType(testType));
            suite.setCriteria(criteria);
            logger.info("Adding suite: " + suite);
            Suite result = newmanClient.addSuite(suite).toCompletableFuture().get();
            logger.info("result: " + result);
        } finally {
            newmanClient.close();
        }
    }

    public void manualSubmitMongoDb() throws Exception {
        NewmanClient newmanClient = getNewmanClient();
        try {
            Suite suite = new Suite();
            suite.setName("mongo-db");
            suite.setCustomVariables("SUITE_TYPE=mongodb,THREADS_LIMIT=1");
            // TODO note - if set is empty, mongodb will NOT write that filed to DB
            String Requirements = "DOCKER,LINUX";
            suite.setRequirements(CapabilitiesAndRequirements.parse(Requirements));
            String testType = "mongodb";
            Criteria criteria = CriteriaBuilder.include(TestCriteria.createCriteriaByTestType(testType));
            suite.setCriteria(criteria);
            logger.info("Adding suite: " + suite);
            Suite result = newmanClient.addSuite(suite).toCompletableFuture().get();
            logger.info("result: " + result);
        } finally {
            newmanClient.close();
        }
    }

    public void manualSubmitDotNet() throws Exception {
        NewmanClient newmanClient = getNewmanClient();
        try {
            Suite suite = new Suite();
            suite.setName("xap-dotnet");
            suite.setCustomVariables("SUITE_TYPE=dotnet,DOTNET_VERSION=3.5");
            String Requirements = "DOTNET,WINDOWS";
            suite.setRequirements(CapabilitiesAndRequirements.parse(Requirements));
            String testType = "dotnet";
            Criteria criteria = CriteriaBuilder.join(
                    CriteriaBuilder.include(TestCriteria.createCriteriaByTestType(testType))
            );

            suite.setCriteria(criteria);
            logger.info("Adding suite: " + suite);
            Suite result = newmanClient.addSuite(suite).toCompletableFuture().get();
            logger.info("result: " + result);
        } finally {
            newmanClient.close();
        }
    }

    private Criteria[] getCriteriasFromExcludedFile(String path) {
        ArrayList<Criteria> criteria = new ArrayList<>();
        File file = new File(path);
        try {
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                criteria.add(PatternCriteria.containsCriteria(line));
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        return criteria.toArray(new Criteria[criteria.size()]);
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
        } finally {
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

    public void manualSubmitXapCoreZookeeper() throws Exception {
        NewmanClient newmanClient = getNewmanClient();
        try {
            Suite suite = new Suite();
            suite.setName("xap-core-zookeeper");
            suite.setCustomVariables("SUITE_TYPE=tgrid,THREADS_LIMIT=2,TGRID_CUSTOM_SYSTEM_PROPS=-Dzookeeper.enable=true");
            // TODO note - if set is empty, mongodb will NOT write that filed to DB
            String Requirements = "LINUX";
            suite.setRequirements(CapabilitiesAndRequirements.parse(Requirements));
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
                            PatternCriteria.containsCriteria("com.gigaspaces.test.transaction.ConcurrentTxnTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.events.DurableNotificationsChangeBasicDurabilityTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.events.DurableNotificationsPartialUpdateBasicDurabilityTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.support.avnza.case7656.LocalViewSpaceWithMirrorTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.cluster.replication.basic_replication.LocalViewSpaceWithMirrorRemoveTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.cluster.primaryelection.SuspendPartitionPrimariesOnSplitBrainDetectionTest"),
                            PatternCriteria.containsCriteria("com.gigaspaces.test.space_filter.InitFailureFilterTest")
                    )
            );
            suite.setCriteria(criteria);
            logger.info("Adding suite: " + suite);
            Suite result = newmanClient.addSuite(suite).toCompletableFuture().get();
            logger.info("result: " + result);
        } finally {
            newmanClient.close();
        }
    }

    public void manualSubmitInsightEdgeCommunity() throws Exception {
        NewmanClient newmanClient = getNewmanClient();
        try {
            Suite suite = new Suite();
            suite.setName("insightedge-community");
            suite.setCustomVariables("SUITE_TYPE=insightedge,CUSTOM_SETUP_TIMEOUT=2700000,THREADS_LIMIT=1,DIST_EDITION=community");
            String Requirements = "DOCKER,LINUX,MVN";
            suite.setRequirements(CapabilitiesAndRequirements.parse(Requirements));
            String testType = "insightedge-integration";
            Criteria criteria = CriteriaBuilder.join(
                    CriteriaBuilder.include(TestCriteria.createCriteriaByTestType(testType)),
                    CriteriaBuilder.exclude(
                            PatternCriteria.containsCriteria("org.insightedge.spark.failover")
                    )
            );
            suite.setCriteria(criteria);
            logger.info("Adding suite: " + suite);
            Suite result = newmanClient.addSuite(suite).toCompletableFuture().get();
            logger.info("result: " + result);
        } finally {
            newmanClient.close();
        }
    }

    public void manualSubmitInsightEdgePremium() throws Exception {
        NewmanClient newmanClient = getNewmanClient();
        try {
            Suite suite = new Suite();
            suite.setName("insightedge-premium");
            suite.setCustomVariables("SUITE_TYPE=insightedge,CUSTOM_SETUP_TIMEOUT=2700000,THREADS_LIMIT=1,DIST_EDITION=premium");
            String Requirements = "DOCKER,LINUX,MVN";
            suite.setRequirements(CapabilitiesAndRequirements.parse(Requirements));
            String testType = "insightedge-integration";
            Criteria criteria = CriteriaBuilder.include(TestCriteria.createCriteriaByTestType(testType));
            suite.setCriteria(criteria);
            logger.info("Adding suite: " + suite);
            Suite result = newmanClient.addSuite(suite).toCompletableFuture().get();
            logger.info("result: " + result);
        } finally {
            newmanClient.close();
        }
    }

    private static NewmanClient getNewmanClient() throws Exception {
        return NewmanClientUtil.getNewmanClient(logger);
    }

    public void manualSubmitKubernetesXAP() throws Exception {
        NewmanClient newmanClient = getNewmanClient();
        try {
            Suite suite = new Suite();
            suite.setName("kubernetes-tests-xap");
            suite.setCustomVariables("SUITE_TYPE=kubernetes,SUITE_SUB_TYPE=XAP,THREADS_LIMIT=1");
            String Requirements = "DOCKER,LINUX";
            suite.setRequirements(CapabilitiesAndRequirements.parse(Requirements));
            Criteria criteria = CriteriaBuilder.include(
                    PatternCriteria.containsCriteria("kubernetes.tests.insightedge.xap")
            );
            suite.setCriteria(criteria);
            logger.info("Adding suite: " + suite);
            Suite result = newmanClient.addSuite(suite).toCompletableFuture().get();
            logger.info("result: " + result);
        } finally {
            newmanClient.close();
        }
    }


    public void manualSubmitKubernetesInsightedge() throws Exception {
        NewmanClient newmanClient = getNewmanClient();
        try {
            Suite suite = new Suite();
            suite.setName("kubernetes-tests-insightedge");
            suite.setCustomVariables("SUITE_TYPE=kubernetes,SUITE_SUB_TYPE=InsightEdge,THREADS_LIMIT=1");
            String Requirements = "DOCKER,LINUX";
            suite.setRequirements(CapabilitiesAndRequirements.parse(Requirements));
            Criteria criteria = CriteriaBuilder.include(
                    PatternCriteria.containsCriteria("kubernetes.tests.insightedge")
            );
            suite.setCriteria(criteria);
            logger.info("Adding suite: " + suite);
            Suite result = newmanClient.addSuite(suite).toCompletableFuture().get();
            logger.info("result: " + result);
        } finally {
            newmanClient.close();
        }
    }

}
