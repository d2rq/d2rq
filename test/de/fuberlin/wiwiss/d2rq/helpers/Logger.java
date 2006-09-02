package de.fuberlin.wiwiss.d2rq.helpers;

/**
 * TODO Replace with commons-logging
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: Logger.java,v 1.1 2006/09/02 22:41:44 cyganiak Exp $
 */
public class Logger {
	private boolean debug = false;
	
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
}