/*
 * $Id: Logger.java,v 1.2 2004/08/09 20:16:52 cyganiak Exp $
 */
package de.fuberlin.wiwiss.d2rq;

/**
 * Logging class for handling debug information, warnings and errors. Implemented
 * as a singleton. The motivation for this class is to limit the impact if we
 * decide to change to some "real" logging facility like log4j or java.util.logging.
 *
 * <p>History:<br>
 * 08-03-2004: Initial version of this class.<br>
 * 
 * @author Richard Cyganiak <richard@cyganiak.de>
 * @version V0.2
 */
class Logger {
	private static Logger instance = null;
	private boolean debug = false;

	/**
	 * Returns an instance of this class.
	 * @return a Logger
	 */
	public static Logger instance() {
		if (Logger.instance == null) {
			Logger.instance = new Logger();
		}
		return Logger.instance;
	}

	/**
	 * Sets the instance. Useful for testing.
	 * @param instance
	 */
	static void setInstance(Logger instance) {
		Logger.instance = instance;
	}

	/**
	 * Enable or disable debug output. If disabled, calls to {@link #debug} are
	 * discarded.
	 * @param enabled <tt>true</tt> to enable debug output
	 */
	public void setDebug(boolean enabled) {
		this.debug = enabled;
	}

	/**
	 * Check if debug output is enabled. Useful to avoid calling {@link #debug} if
	 * the call contains arguments that are expensive.
	 * @return <tt>true</tt> if debug output is enabled.
	 */
	public boolean debugEnabled() {
		return this.debug;
	}

	/**
	 * Sends a debug message. Debug message are discarded during normal operation.
	 * The default implementation prints the message to standard out if debug
	 * output ist enabled.
	 * @param message
	 */
	public void debug(String message) {
		if (!this.debug) {
			return;
		}
		System.out.println(message);
	}
	
	/**
	 * Sends a warning message. Warning messages are displayed to the user during
	 * normal operation. The default implementation prints the message to standard
	 * error.
	 * @param message
	 */
	public void warning(String message) {
		System.err.println("Warning: " + message);
	}
	
	/**
	 * Sends an error message. Error messages are fatal and will cancel program
	 * execution during normal operation. The default implementation is to throw
	 * a {@link D2RQException} which can be caught.
	 * @param message
	 */
	public void error(String message) {
		throw new D2RQException(message);
	}
}