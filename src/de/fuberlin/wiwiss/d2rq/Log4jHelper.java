package de.fuberlin.wiwiss.d2rq;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * All Log4j-specific stuff is encapsulated here.
 * 
 * Default configuration is in /etc/log4j.properties. We always
 * have to put that on the classpath so Log4j will find it.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class Log4jHelper {

	public static void turnLoggingOff() {
		System.err.println("Logging is turned off!");
		Logger.getLogger("de.fuberlin.wiwiss.d2rq").setLevel(Level.OFF);
	}
	
	public static void setVerboseLogging() {
		// Adjust Log4j log level to show more stuff
		Logger.getLogger("d2rq").setLevel(Level.INFO);
		Logger.getLogger("de.fuberlin.wiwiss.d2rq").setLevel(Level.INFO);
		Logger.getLogger("org.eclipse.jetty").setLevel(Level.INFO);
		Logger.getLogger("org.joseki").setLevel(Level.INFO);
	}
	
	public static void setDebugLogging() {
		// Adjust Log4j log level to show MUCH more stuff 
		Logger.getLogger("d2rq").setLevel(Level.ALL);
		Logger.getLogger("de.fuberlin.wiwiss.d2rq").setLevel(Level.ALL);
		Logger.getLogger("org.eclipse.jetty").setLevel(Level.INFO);
		Logger.getLogger("org.joseki").setLevel(Level.INFO);
	}
}
