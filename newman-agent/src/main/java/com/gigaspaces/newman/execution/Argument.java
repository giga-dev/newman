/*
 * @(#)Argument.java   Apr 25, 2007
 *
 * Copyright 2007 GigaSpaces Technologies Inc.
 */
package com.gigaspaces.newman.execution;

import java.util.ArrayList;

/**
 * This class defines a command line argument.
 *
 * @author Igor Goldenberg
 * @since  1.0
 * @see Command
 **/
public class Argument
{
   private String _name;
   private String _value;
   
  /**
	* Constructs a new Argument that is a single value argument,
	* meaning that the argument doesn't take additional information. 
	**/
	public Argument(String name) 
	{
	  _name = name;	
	}

	/**
	 * Constructs a new Argument that has a value argument, 
	 * meaning that the argument takes one additional parameter that is the value of the argument.
	 */
	public Argument(String name, String value)
	{
	  this(name);
	  
	  _value = value;
	}
    
	public String getName() 
	{
	  return _name;
	}
    
	
	public String getValue() 
	{
	  return _value;	
	}
    	
	@Override
	public int hashCode()
	{
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((_name == null) ? 0 : _name.hashCode());
		result = PRIME * result + ((_value == null) ? 0 : _value.hashCode());
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
		final Argument other = (Argument) obj;
		if (_name == null)
		{
			if (other._name != null)
				return false;
		}
		else
			if (!_name.equals(other._name))
				return false;
		if (_value == null)
		{
			if (other._value != null)
				return false;
		}
		else
			if (!_value.equals(other._value))
				return false;
		
		return true;
	}

	public String toString()
	{
	  if ( _value == null )
		  return _name;
	  else
		  return _name + " " + _value;
	}
	
	/** a helper method to convert arrays or {@link Argument} to array of String[] */
	static String[] toString( Argument[] args )
	{
	  if ( args == null || args.length == 0 )
		  throw new IllegalArgumentException( "Argument array can not be null or with length of zero" );
		
	  ArrayList<String> strArgs = new ArrayList<String>();
	  
	  for( Argument arg : args )
	  {
		 if ( arg.getValue() != null )
		 {
			strArgs.add( arg.getName() );
			strArgs.add( arg.getValue() );			
		 }
		 else
		 {
			strArgs.add( arg.getName() ); 
		 }
	  }
	  
	  return strArgs.toArray( new String[0] );
	}
}