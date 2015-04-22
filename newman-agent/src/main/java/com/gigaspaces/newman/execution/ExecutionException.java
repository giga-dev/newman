/*
 * @(#)ExecutionException.java   Apr 25, 2007
 *
 * Copyright 2007 GigaSpaces Technologies Inc.
 */
package com.gigaspaces.newman.execution;

/**
 * This exception is thrown when the Executor fails during command execution.
 *
 * @author Igor Goldenberg
 * @since  1.0
 **/
public class ExecutionException
		extends RuntimeException
{
	private static final long	serialVersionUID	= 1L;
	
	public ExecutionException()
	{
	}
   
	public ExecutionException(String message) 
	{
	  super( message );
	}
	           
	public ExecutionException(String message, Throwable cause) 
	{
		super( message, cause );
	}
	           
	public ExecutionException(Throwable cause)
	{
	  super( cause );	
	}
}
