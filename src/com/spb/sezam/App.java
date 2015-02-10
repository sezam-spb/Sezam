package com.spb.sezam;

import java.lang.Thread.UncaughtExceptionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Application;

public class App extends Application {
	// uncaught exception handler variable
    private UncaughtExceptionHandler defaultUEH;
    
 // handler listener
    private UncaughtExceptionHandler unCaughtExceptionHandler =
        new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {

                // here I do logging of exception to a db
            	 Logger SlfLogger = LoggerFactory.getLogger(App.class);
                 SlfLogger.error("Error accured in thread " + thread.getName(), ex);

                // re-throw critical exception further to the os (important)
                defaultUEH.uncaughtException(thread, ex);
            }
        };
        
    public App() {
        defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
        // setup handler for uncaught exception 
        Thread.setDefaultUncaughtExceptionHandler(unCaughtExceptionHandler);
    }
}
