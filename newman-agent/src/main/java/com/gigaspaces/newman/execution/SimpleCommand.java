/*
 * @(#)SimpleCommand.java   Apr 25, 2007
 *
 * Copyright 2007 GigaSpaces Technologies Inc.
 */
package com.gigaspaces.newman.execution;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;

/**
 * This is a simple command that executes any arbitrary string command with arguments.
 *
 * @author Igor Goldenberg
 * @since 1.0
 **/
public class SimpleCommand
	implements Command
{
	private String 	 _commandName = "SimpleCommand: ";
	private Argument[] _arguments;
	
	/** Constructs a SimpleCommand from the given command name and the list of arguments. */
	public SimpleCommand(String commandName, Argument... arguments)
	{
		_commandName = commandName;
		_arguments   = arguments;
	}
	
	/** 
	 * Constructs a SimpleCommand by tokenizing the command given into the command name and 
	 * list of arguments to the command. 
	 **/
	public SimpleCommand(String command)
	{
	  StringTokenizer tokenizer = new StringTokenizer( command );
	  _arguments = new Argument[tokenizer.countTokens() ]; 
	  int i = 0;
	  while( tokenizer.hasMoreTokens() )
	  {
		 _arguments[i++] = new Argument( tokenizer.nextToken().trim() );   
	  }
	}

	/** Constructs a SimpleCommand from the given command name and the list of arguments. */
	public SimpleCommand(String commandName, String... args)
	{
		this( commandName, (Argument)null );
		
		ArrayList<Argument> argList = new ArrayList<Argument>();
		for( String arg : args )
			argList.add( new Argument( arg ) );

		_arguments = argList.toArray( new Argument[0] );
	}
	
	/** Returns an array of command line arguments, if any, that the command requires for execution. */
	public Argument[]	getArguments()
	{
	  return _arguments;
	}
	
	/** @return the name of the command to execute. */
	public String[] getCommand() 
	{
		return Argument.toString( getArguments() );
	}
	
	/**
	 * Explicitly set to redirect the output stream to {@link System#out}.
	 * @return {@link System#out}.
	 */
	public OutputStream getOutputStreamRedirection() {
		return System.out;
	}
	
	public String toString()
	{
	  return _commandName + " " + Arrays.asList( getArguments() ).toString();
	}

	@Override
	public int hashCode()
	{
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + Arrays.hashCode(_arguments);
		
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		
		if (obj == null)
			return false;
		
		if (getClass() != obj.getClass())
			return false;
		
		final SimpleCommand other = (SimpleCommand) obj;
		
		if (!Arrays.equals(_arguments, other._arguments))
			return false;
		
		return true;
	}

	/** {@link Executor} events */
	public void beforeExecute() {}
	public void destroy(boolean force ) {}
	public void afterExecute(Process process) { }
}