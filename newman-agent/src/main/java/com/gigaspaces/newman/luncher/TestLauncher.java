package com.gigaspaces.newman.luncher;

import com.gigaspaces.newman.execution.AsyncCommandResult;
import com.gigaspaces.newman.execution.Executor;
import com.gigaspaces.newman.util.FileUtilities;

import java.io.File;

/**
 *
 * @author Boris
 * @since  1.0
 */
public class TestLauncher
{
    public enum ExecutionType {
        JUNIT, SGTEST, CPP, DOTNET
    }

    public void execute(String testName ,String workingDirectory, long timeout) throws Exception
    {
        execute(testName, ExecutionType.JUNIT, workingDirectory, timeout);
    }

    public void execute(String testName, ExecutionType executionType ,String workingDirectory, long timeout) throws Exception
    {
        File wd = new File(workingDirectory);
        if (!wd.exists()){
            throw new IllegalArgumentException("wd " + wd + " not exists");
        }
        // TODO add classpath as 2nd parameter to the script
        String prefix = FileUtilities.isWindows() ? "cmd /c " : "";
        String command = prefix + getExecutionScript(executionType) + " " + testName;
        AsyncCommandResult asyncCmd = Executor.executeAsync(command, wd);
        asyncCmd.redirectOutputStream(System.out);
        asyncCmd.waitFor(timeout);
        asyncCmd.stop(false);
    }

    private String getExecutionScript(ExecutionType executionType) {
        if (executionType == ExecutionType.JUNIT){
            return "run-junit"+ FileUtilities.getScriptSuffix();
        }
        else if (executionType == ExecutionType.CPP){
            throw new UnsupportedOperationException("CPP Launcher isn't implemented yet");
        }
        else if (executionType == ExecutionType.SGTEST){
            throw new UnsupportedOperationException("SGTEST Launcher isn't implemented yet");
        }
        else if (executionType == ExecutionType.DOTNET){
            throw new UnsupportedOperationException("DOTNET Launcher isn't implemented yet");
        }
        else {
            throw new IllegalArgumentException("Illegal execution type");
        }
    }

}
