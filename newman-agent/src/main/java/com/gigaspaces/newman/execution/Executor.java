/*
 * @(#)Executor.java   Apr 25, 2007
 *
 * Copyright 2007 GigaSpaces Technologies Inc.
 */
package com.gigaspaces.newman.execution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class executes {@link Command} using the Java runtime fork exec model.
 * Executor provides an abstract infrastructure to execute any types of Commands, so client can easily implement 
 * his own Command implementation for i.e: SSHCommand and etc...<br>
 *
 * Sync example, returns CommandResult when forked process finished to run:<br>
 * <pre>
 * CommandResult cmdRes = Executor.execute( "startScript.sh", "/usr/bin");
 * System.out.println( cmdRes.getOut() );
 * System.out.println( cmdRes.getCode() );
 * </pre>
 * 
 * Async example returns AsyncCommandResult after fork process: <br>
 * <pre>
 * AsyncCommandResult asyncCmd = Executor.execute( "startScript.sh", "/usr/bin");
 * asyncCmd.redirectOutputStream( System.out );
 * asyncCmd.waitFor( 5 * 1000 );
 * asyncCmd.stop();
 * </pre>
 * 
 * Command example:<br>
 * <pre>
 * Executor.execute( JavaCommand(...), "/usr/bin");
 * </pre>
 *
 * @author  Igor Goldenberg
 * @version 1.0
 * @see SimpleCommand

 **/
public class Executor
{
    private static final Logger logger = LoggerFactory.getLogger(Executor.class);
	
  /** a single thread which executes flushing output of forkable service process */
  final static ExecutorService _forkableExecutor = Executors.newCachedThreadPool();
  
	/**
	 * A non-closeable PrintStream used to encapsulate the close() logic of
	 * a stream. The System.out PrintStream should never be closed by the
	 * application and therefore overridden to be ignored.
	 */
	private static class StandardOutputStream extends PrintStream
	{
	    public StandardOutputStream() {
		   super(System.out);
	    }
	    
	    @Override
	    public void close() {
	        //don't close the system out
	    }
	}
    
 /** 
  * A very native command executor. This uses the Java Runtime class to spawn off a new process to perform a command. 
  * This waits for the called command to exit before returning and bundles the results in a {@link CommandResult}.
  * 
  * @param command The Command to execute.
  * @param dir The working directory to perform the execution from.
  * @return A CommandResult and never null.
  * 
  * @throws ExecutionException If the command could not be executed.
  **/
  public static CommandResult execute(Command command, File dir) 
  			throws ExecutionException
  {
	 AsyncCommandResult cmdRes = executeAsync(command, dir);
	 cmdRes.bufferOutputStream();
	 
	 try{
		cmdRes.waitFor( Integer.MAX_VALUE );
	 }catch (InterruptedException e){
		throw new ExecutionException("Execute command: [" + command + "] was interrupted.", e); 
	 }

	 return cmdRes; 
  }

  /** 
   * Starts the given command asynchronously. This method returns immediately once the process has been spawned.
   * The AsyncCommandResult can be used to watch the status of the process and control the process. 
   * 
   * @param command The command to execute.
	* @param dir The directory to execute the command from.
	* @return An AsyncCommandResult that is used to watch and control the process.
   **/
  public static AsyncCommandResult executeAsync(Command command, File dir)
    throws ExecutionException 
  {
      logger.info("Executing:\n\t command: " + command + "\n\t directory:" + dir);
      try
      {
          Process process = forkProcess( command, dir );
          return new AsyncCommandResult( process, command );
	  }catch( ExecutionException ex ){
		  throw ex;
	  }catch( Throwable th ){
		  throw new ExecutionException( "Failed to execute async command: [" + command + "]", th );
	  }
  }

  /** 
   * Creates a SimpleCommand from the command String and then calls the executeAsync(Command, File) method.
   * 
   * @param command The command to execute.
	* @param dir The directory to execute the command from.
	* @return An AsyncCommandResult that is used to watch and control the process.
   **/
  public static AsyncCommandResult executeAsync(String command, File dir)
  {
	 return executeAsync( new SimpleCommand( command ) , dir); 
  }  
  	
  /**
	* Allows the caller to wait for the completion of the process, but no longer
	* than a given timeout value.
	* @param timeout - The given timeout value (ms). 
	* @return <code>true</code> if process finished(destroyed), otherwise <code>false</code>.
    * 
	* @throws InterruptedException
	*/
	static boolean waitFor(Process process, long timeout)
			throws InterruptedException
	{
		/* interval constant */
		final int interval = 1000 * 1; // 1 sec
		long timeWaiting = 0;

		while (timeWaiting < timeout)
		{
            if ( !isProcessAlive(process) )
                return true;

    		 logger.debug("Process is still alive [" + process + "] time to wait [" + (timeout - timeWaiting) + "ms], timeout [" + timeout + "]ms");
            
            try{
                Thread.sleep(interval);
            }
            catch (InterruptedException e)
            {
                e.fillInStackTrace();
                throw e;
            }

			timeWaiting += interval;
		}
		
        /* process hasn't been destroyed */
		return false;
	}	
	
	 /** @return <code>true</code> if supplied process is still alive, otherwise <code>false</code> */
    static boolean isProcessAlive( Process process )
	 {
	   try
	   {
		  process.exitValue();
		  return false;
	   }catch (IllegalThreadStateException e){
		  return true; 
	   }
	 }
		
    /**
     * Fork the supplied command from desired directory.
     * @param command the command to fork.
     * @param directory The directory to execute the command from or <code>null</code> to to have current directory.
     * @return the forked process.
     * @throws java.io.IOException Failed to fork process.
     */
    static Process forkProcess( Command command, File directory )
    throws IOException
    {
		command.beforeExecute();

		Process process = forkProcess(directory, command.getCommand());
		redirectOutputStream(process, command.getOutputStreamRedirection(),
				command);

		/* only for debug purposes - also redirect to screen */
		if (logger.isDebugEnabled())
			redirectOutputStream(process, System.out, command);

		command.afterExecute( process);

		return process;
    }


  /**
	 * Create operating system process and redirect all process output stream to
	 * supplied file.
	 *
	 * @param directory
	 *            The new working directory
	 * @param processArgs
	 *            A string array containing the program and its arguments.
	 *            if not <code>null</code> then any error/standard output
	 *            stream generated by this processes will be redirected to the
	 *            supplied file.
	 * @return The started process.
	 * @throws java.io.IOException
	 *             Failed to start process.
	 */
  static Process forkProcess( File directory, String...processArgs  )
          throws IOException
  {
      ProcessBuilder processBuilder = new ProcessBuilder(processArgs);
      processBuilder.directory(directory);
      processBuilder.redirectErrorStream(true);
      return processBuilder.start();
   	/*else
   	  return Runtime.getRuntime().exec(processArgs, null*//*inherit*//*, directory);*/
  }

 /**
  * Redirect process output stream to desired abstract {@link java.io.OutputStream} (FileOutputStream, System.out).
  *  
  * @param process a forked process
  * @param outStream an abstract output stream.
  * @param command an execution command.
  */
  static void redirectOutputStream( final Process process, OutputStream outStream, Command command )
  {
	   //outStream may be null if no redirection is required
	   if (outStream == null)
		  return; //no redirection required
	  	
	   /* override system.out stream to avoid close() */
	   if ( outStream == System.out )
	     outStream = new StandardOutputStream();
	   	
		final PrintStream procOutStream = new PrintStream( outStream );
		final InputStream processInputStream = process.getInputStream();

		procOutStream.println( command + "\n");
		
		/* start in background ProccessOutputCollector */
		_forkableExecutor.execute(
				new Runnable()
				{
				  public void run()
				  {
						String line = null;
						BufferedReader in = new BufferedReader(new InputStreamReader(processInputStream));
						
						try
						{
						  while ((line = in.readLine()) != null)
						  {
						    procOutStream.println(line);
						    procOutStream.flush();
						  }
						}catch( IOException ex )
						{
						  if ( logger.isDebugEnabled())
							    logger.debug("Caught exception by ProcessOutputStream.", ex);
						}catch( NullPointerException ex )
						{
						  // ignore since JDK 1.4 has a bug	
						}finally
						{
						  if ( procOutStream != null )
						    procOutStream.close();
						}
						
						if ( logger.isDebugEnabled())
						     logger.debug("ForkProcess Thread: " + Thread.currentThread().getName() + " was terminated.");
				  }// run()
			}// Runnable
		); 	  
		
		/* give an opportunity to start processor-collector thread before the process was destroyed */
		try{
			Thread.sleep(1500);
		}catch (InterruptedException e){};
  }
}