package com.gigaspaces.newman.utils;

import org.apache.commons.io.output.TeeOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ProcessUtils {

    private static final Logger logger = LoggerFactory.getLogger(ProcessUtils.class);
    public static final long DEFAULT_SCRIPT_TIMEOUT = 20 * 60 * 1000;

    public static ProcessResult executeAndWait(Path file, Path workingFolder, Path outputPath, Map<String,String> customVariables) throws IOException, InterruptedException {
        return executeAndWait(file, Collections.emptyList(), workingFolder, outputPath, customVariables, DEFAULT_SCRIPT_TIMEOUT, false);
    }

    public static ProcessResult executeCommandAndWait(String cmd, long timeout) throws IOException, InterruptedException {
        return executeCommandAndWait(cmd, timeout, null);
    }

    public static ProcessResult executeCommandAndWait(String cmd, long timeout, StringBuilder sb) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder("/bin/sh", "-c", cmd);
        ProcessResult result = new ProcessResult();
        result.setStartTime(System.currentTimeMillis());
        Process process = processBuilder.start();
        boolean exited = process.waitFor(timeout, TimeUnit.MILLISECONDS);
        if (exited) {
            result.setExitCode(process.exitValue());
        } else {
            logger.info("ref ["+process.hashCode()+"] Destroying forcibly due to timeout ("+timeout+") ms - command " + cmd);
            Process destroyed = process.destroyForcibly();
            if (!destroyed.waitFor(10, TimeUnit.SECONDS)) {
                logger.warn("ref ["+process.hashCode()+"] Failed to destroy forcibly after 10 seconds");
            }
        }
        result.setEndTime(System.currentTimeMillis());
        if (sb != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = "";
            while ((line = reader.readLine())!= null) {
                sb.append(line + "\n");
            }
        }
        return result;
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
            environment.put(variableKeyValue.getKey(), variableKeyValue.getValue().replace("\"", ""));
        }
        processBuilder.directory(workingFolder.toFile());
        processBuilder.redirectErrorStream(true);

        // Execute process:
        ProcessResult result = new ProcessResult();
        result.setStartTime(System.currentTimeMillis());
        Process process = processBuilder.start();
        final String pid = getPidFromProcess(process);
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
        forceKillProcessTree(pid);
        result.setEndTime(System.currentTimeMillis());
        consumer.join(10 * 1000);
        consumer.interrupt();
        return result;
    }

    private static void forceKillProcessTree(String pid) {
        if (!pid.equals("Unknown") && !System.getProperty("os.name").startsWith("Windows")){
            String cmd = "kill -9 $(ps -o pid= --ppid "+pid+")";
            try {
                executeCommandAndWait(cmd, 10 * 1000);
            } catch (Exception e) {
                logger.warn("failed to kill process tree with pid: " + pid, e);
            }
        }
    }

    public static class InputStreamConsumer extends Thread {

        private final InputStream is;
        private final OutputStream multiOut;
        private final FileOutputStream fileOutputStream;

        public InputStreamConsumer(InputStream is, Path outputPath, boolean redirectToStdout) {
            setName("NewmanInputStreamConsumer");
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
                while (!Thread.currentThread().isInterrupted())
                {
                    int available = is.available();
                    if (available > 0 && (value = is.read()) != -1) {
                        multiOut.write(value);;
                    }
                    else
                        Thread.sleep(1);
                }
            } catch (IOException exp) {
                logger.error("failed to read/write stream from the process", exp);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
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
        final ProcessResult processResult = ProcessUtils.executeAndWait(file, Collections.emptyList(), basePath, output, new HashMap<>(), 10 * 1000, true);
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

    private static String getPidFromProcess(Process p){
        try {
            Field f = p.getClass().getDeclaredField("pid");
            f.setAccessible(true);
            return f.get(p).toString();
        } catch (Exception e) {
            return "Unknown";
        }
    }
}
