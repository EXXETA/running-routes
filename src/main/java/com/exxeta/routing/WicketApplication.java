package com.exxeta.routing;

import org.apache.wicket.protocol.http.WebApplication;

/**
 * Application object for your web application.
 */
public class WicketApplication extends WebApplication
{    
    /**
     * Constructor
     */
	public WicketApplication()
	{
	}
	
	/**
	 * @see org.apache.wicket.Application#getHomePage()
	 */
    @Override
	public Class<HomePage> getHomePage()
	{
		return HomePage.class;
	}

}
