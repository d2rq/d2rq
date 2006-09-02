package de.fuberlin.wiwiss.d2rq;

import com.hp.hpl.jena.shared.JenaException;

/**
 * Exception used to signal most D2RQ errors.
 *
 * @author Chris Bizer chris@bizer.de
 * @version $Id: D2RQException.java,v 1.2 2006/09/02 22:41:43 cyganiak Exp $
 */

public class D2RQException extends JenaException {

	public D2RQException(String message) {
		super(message);
	}

	public D2RQException(Throwable cause) {
		super(cause);
	}
	
	public D2RQException(String message, Throwable cause) {
		super(message, cause); 
	}
}

