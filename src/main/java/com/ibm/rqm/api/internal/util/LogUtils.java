/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2011, 2020. All Rights Reserved.
 *  
 * U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 ******************************************************************************/
package com.ibm.rqm.api.internal.util;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import com.ibm.rqm.api.internal.APIUtilities;

/**
 * <p>API logger.</p>
 * 
 * <p>Note: Copied from <code>com.ibm.rqm.ct</code> plug-in.</p>
 * 
 *  
 * @author  Paul Slauenwhite
 * @version 0.9
 * @since   0.9
 */
public class LogUtils {

    private static Logger logger = null;
    
    private static Logger getLogger() {
       
    	if (logger == null) {
        	
    		logger = Logger.getLogger(APIUtilities.class.getName());
    		logger.setLevel(Level.SEVERE);    		
        }
        
    	return logger;
    }
    
    public static void setLogFile(String file) {
       
    	try {
           
			//Remove all handlers:
    		Logger logger = getLogger();

    		while(logger != null){

    			for(Handler handler : logger.getHandlers()){
    				logger.removeHandler(handler);           
    			}

    			logger = logger.getParent();
    		}
            
            //Create new file handler:
            FileHandler fileHandler = new FileHandler(file);
            fileHandler.setFormatter(new SimpleFormatter());
            fileHandler.setLevel(Level.ALL);
            
            //Configure the logger:
            getLogger().addHandler(fileHandler);
            getLogger().setLevel(Level.ALL);
        } 
    	catch (IOException e) {
    		LogUtils.logError("Error initializing file logging", e); //$NON-NLS-1$
        }
    }

    public static void setLevel(Level level) {
        getLogger().setLevel(level);
    }
    
    public static void logTrace(String message) {
        getLogger().log(Level.FINE, message);
    }

    public static void logTrace(String message, Throwable throwable) {
        getLogger().log(Level.FINE, message, throwable);
    }

    public static void logInfo(String message) {
        getLogger().log(Level.INFO, message);
    }

    public static void logInfo(String message, Throwable throwable) {
        getLogger().log(Level.INFO, message, throwable);
    }

    public static void logWarning(String message) {
        getLogger().log(Level.WARNING, message);
    }

    public static void logWarning(String message, Throwable throwable) {
        getLogger().log(Level.WARNING, message, throwable);
    }

    public static void logError(String message) {
        getLogger().log(Level.SEVERE, message);        
    }

    public static void logError(String message, Throwable throwable) {
        getLogger().log(Level.SEVERE, message, throwable);        
    }
}
