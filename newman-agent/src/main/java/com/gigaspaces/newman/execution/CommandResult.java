/*
 * @(#)CommandResult.java   Apr 25, 2007
 *
 * Copyright 2007 GigaSpaces Technologies Inc.
 */
package com.gigaspaces.newman.execution;

import java.io.ByteArrayOutputStream;

/**
 * This is a command result that is returned from an sync execution.
 * The CommandResult will be return from {@link Executor#execute(Command, java.io.File)}  method when
 * forked process will be finished.<br>
 * To get the buffered process stream use {@link #getOut()}.
 * 
 * <pre>
 * CommandResult cmdRes = Executor.execute( "startScript.sh", "/usr/bin");
 * System.out.println( cmdRes.getOut() );
 * System.out.println( cmdRes.getCode() );
 * </pre>
 * 
 * @author	 Igor Goldenberg
 * @since	 1.0
 * @see Executor
 **/
public class CommandResult
{
  private final Command command;
  private final Process process;
  private ByteArrayOutputStream byteOutStream;
  
  /** Construct command result instance with given arguments */	
  CommandResult(Process process, Command command)
  {
	 this.process = process; 
	 this.command = command; 
  }
    
  /**
   * @return the buffered process stream
   * @see Command#getOutputStreamRedirection().
   */
  public String getOut()
  {
	return byteOutStream != null ? new String( byteOutStream.toByteArray() ) : "";  
  }
      
  /** @return the forked process */
  public Process getProcess()
  {
	 return process;
  }
  
  /** @return the execution command */
  public Command getCommand()
  {
	 return command;
  }
  
  /** @return the exit process code */
  public int getCode()
  {
	 return process.exitValue();
  }
      
  void bufferOutputStream()
  {
	 byteOutStream = new ByteArrayOutputStream(); 
	 Executor.redirectOutputStream(process, byteOutStream, command);
  }
}