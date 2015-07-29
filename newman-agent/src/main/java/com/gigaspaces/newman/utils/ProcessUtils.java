package com.gigaspaces.newman.utils;

import org.apache.commons.io.output.TeeOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ProcessUtils {

    private static final Logger logger = LoggerFactory.getLogger(ProcessUtils.class);
    public static final long DEFAULT_SCRIPT_TIMEOUT = 20 * 60 * 1000;

    public static ProcessResult executeAndWait(Path file, Path workingFolder, Path outputPath, Map<String,String> customVariables) throws IOException, InterruptedException {
        return executeAndWait(file, Collections.emptyList(), workingFolder, outputPath, customVariables, DEFAULT_SCRIPT_TIMEOUT, false);
    }

    public static ProcessResult executeAndWait(Path file, Collection<String> arguments, Path workingFolder,
                                               Path outputPath, Map<String,String> customVariables, long timeout)
            throws IOException, InterruptedException {
        return executeAndWait(file, arguments, workingFolder, outputPath, customVariables, timeout, false);
    }

    public static ProcessResult executeAndWait(Path file, Collection<String> arguments, Path workingFolder,
                                               Path outputPath, Map<String,String> customVariables, long timeout, boolean redirectToStdout)
            throws IOException, InterruptedException {

        // Setup:
        ProcessBuilder processBuilder = new ProcessBuilder(file.toString());
        if (arguments != null)
            processBuilder.command().addAll(arguments);
        // pass custom environment variables to scripts
        final Map<String, String> environment = processBuilder.environment();
        for (Map.Entry<String,String> variableKeyValue : customVariables.entrySet()){
            environment.put(variableKeyValue.getKey(), variableKeyValue.getValue());
        }
        processBuilder.directory(workingFolder.toFile());
        processBuilder.redirectErrorStream(true);

        // Execute process:
        ProcessResult result = new ProcessResult();
        result.setStartTime(System.currentTimeMillis());
        Process process = processBuilder.start();
        // consume stream from the process and print it to file/file + stdout
        InputStreamConsumer consumer = new InputStreamConsumer(process.getInputStream(), outputPath, redirectToStdout);
        consumer.start();
        boolean exited = process.waitFor(timeout, TimeUnit.MILLISECONDS);
        if (exited) {
            result.setExitCode(process.exitValue());
        } else {
            logger.info("ref ["+process.hashCode()+"] Destroying forcibly due to timeout ("+timeout+") ms - file: " + file.getFileName() + " args: " + arguments);
            Process destroyed = process.destroyForcibly();
            if (!destroyed.waitFor(10, TimeUnit.SECONDS)) {
                logger.warn("ref ["+process.hashCode()+"] Failed to destroy forcibly after 10 seconds");
            }
        }
        result.setEndTime(System.currentTimeMillis());
        consumer.join();
        return result;
    }

    public static class InputStreamConsumer extends Thread {

        private final InputStream is;
        private final OutputStream multiOut;
        private final FileOutputStream fileOutputStream;

        public InputStreamConsumer(InputStream is, Path outputPath, boolean redirectToStdout) {
            this.is = is;
            try {
                fileOutputStream = new FileOutputStream(outputPath.toFile());
                multiOut = redirectToStdout ? new TeeOutputStream(System.out, fileOutputStream) : fileOutputStream;
            } catch (FileNotFoundException e) {
                throw new RuntimeException("cant create OutputStream", e);
            }
        }

        @Override
        public void run() {

            try {
                int value;
                while ((value = is.read()) != -1) {
                    multiOut.write(value);
                }
            } catch (IOException exp) {
                logger.error("failed to read/write stream from the process", exp);
            }
            finally {
                try {
                    //close only file stream to avoid closing stdout
                    fileOutputStream.close();
                } catch (IOException e) {
                    logger.error("failed to close InputStreamConsumer", e);
                }
            }
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        String base = "/home/boris/dev/sources/git/newman/test";
        final Path basePath = FileUtils.append(base, "");
        final Path file = FileUtils.append(basePath, "test.sh");
        final Path output = FileUtils.append(basePath, "test.log");
        final ProcessResult processResult = ProcessUtils.executeAndWait(file, Collections.emptyList(), basePath, output, new HashMap<>(), DEFAULT_SCRIPT_TIMEOUT, true);
        System.out.println("result: " + processResult);
    }

    public static String getProcessId(final String fallback) {
        // Note: may fail in some JVM implementations
        // therefore fallback has to be provided
        final String jvmName = ManagementFactory.getRuntimeMXBean().getName();
        final int index = jvmName.indexOf('@');

        if (index < 1) {
            return fallback;
        }
        try {
            return Long.toString(Long.parseLong(jvmName.substring(0, index)));
        } catch (NumberFormatException ignored) {}
        return fallback;
    }
}
