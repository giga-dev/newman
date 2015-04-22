/*
 * @(#)AsyncCommandResult.java   Apr 25, 2007
 *
 * Copyright 2007 GigaSpaces Technologies Inc.
 */
package com.gigaspaces.newman.execution;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;


/**
 * This is a command result that is returned from an async execution. 
 * The result stores the state of the execution and also allows clients to 
 * <li>wait for execution to finish.</li> 
 * <li>stop/destroy process.</li> 
 * <li>redirect process stream to desired {@link java.io.OutputStream} or to file.</li>
 *
 * @author	Igor Goldenberg
 * @since	1.0
 * @see Command
 * @see Executor
 **/
public class AsyncCommandResult
		extends CommandResult
{
	/** Constructs a CommandResult for an asynchronous process that is running. */
	AsyncCommandResult(Process proc, Command command)
	{
	  super(proc, command);
	}


	/** Checks if the process is finished yet. */
	public boolean	isFinished()
	{
	  return !Executor.isProcessAlive( getProcess() );
	}

	/**
	 * Stops/destroy the asynchronous process. Blocks until the process has
	 * terminated or until a timeout of 1 minute occurs.
     *
	 * @param force if <code>false</code> forcibly stop this command process,
     *  if <code>true</code> also stop all child processes.
     *
     * @throws IllegalStateException if the timeout has expired.
	 */
    public void stop(boolean force)
    {
        getProcess().destroy();

        try{ /* yield to ShutdownHook 60 sec */
            if ( !force )
               waitFor(60 * 1000);
        }catch (InterruptedException e1) {}

        getCommand().destroy( force );

        try
        {
            //wait an additional ~1.5 seconds
            boolean destroyed = waitFor( 1500);

            if (!destroyed)
               throw new IllegalStateException("Timeout of 1 minute occurred while waiting for the process to terminate. (force=" + force + ")");
        }catch (InterruptedException e) {}
    }


	/**
	 * Allows the caller to wait for the completion of the process, but no longer
	 * than a given timeout value.
	 * @param timeout - The given timeout value (ms).
	 * @return Returns <code>true</code> if process finished, otherwise <code>false</code>.
	 *
	 * @throws InterruptedException
	 */
	public boolean waitFor( long timeout )
	  throws InterruptedException
	{
		return Executor.waitFor( getProcess() , timeout);
	}

	/**
	 * Redirects process stream to file.
	 * @param fileName the file path to redirect.
	 * @throws java.io.FileNotFoundException Failed to initialize output file, file path is wrong or permission denied.
	 * @see Command#getOutputStreamRedirection().
	 */
	public void redirectOutputStream( String fileName )
		throws FileNotFoundException
	{
	  Executor.redirectOutputStream( getProcess(), new FileOutputStream( fileName, true /*append*/ ), getCommand() );
	}

	/**
	 * Redirect process stream to supplied output stream, i.e : {@link System#out}, {@link java.io.FileOutputStream}, {@link java.io.BufferedOutputStream}
	 * @param outStream the output stream.
	 * @see Command#getOutputStreamRedirection().
	 */
	public void redirectOutputStream( OutputStream outStream )
	{
	  Executor.redirectOutputStream( getProcess(), outStream, getCommand() );	
	}
}