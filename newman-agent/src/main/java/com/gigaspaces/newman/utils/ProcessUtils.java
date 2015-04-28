package com.gigaspaces.newman.utils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class ProcessUtils {

    private static final long DEFAULT_SCRIPT_TIMEOUT = 10 * 60 * 1000;

    public static ProcessResult executeAndWait(Path file, Path workingFolder, Path outputPath) throws IOException, InterruptedException {
        return executeAndWait(file, Collections.EMPTY_LIST, workingFolder, outputPath, DEFAULT_SCRIPT_TIMEOUT);
    }

    public static ProcessResult executeAndWait(Path file, Collection<String> arguments, Path workingFolder,
                                               Path outputPath, long timeout)
            throws IOException, InterruptedException {
        // Setup:
        ProcessBuilder processBuilder = new ProcessBuilder(file.toString());
        if (arguments != null)
            processBuilder.command().addAll(arguments);
        processBuilder.directory(workingFolder.toFile());
        processBuilder.redirectErrorStream(true);
        if (outputPath != null)
            processBuilder.redirectOutput(outputPath.toFile());

        // Execute process:
        ProcessResult result = new ProcessResult();
        result.setStartTime(System.currentTimeMillis());
        Process process = processBuilder.start();
        boolean exited = process.waitFor(timeout, TimeUnit.MILLISECONDS);
        if (exited) {
            result.setExitCode(process.exitValue());
        } else {
            process.destroyForcibly();
        }
        result.setEndTime(System.currentTimeMillis());

        return result;
    }
}
