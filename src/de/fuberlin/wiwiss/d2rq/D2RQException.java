/*
  (c) Copyright 2004 by Chris Bizer (chris@bizer.de)
*/

package de.fuberlin.wiwiss.d2rq;

import com.hp.hpl.jena.shared.*;

/** Exception used to signal most D2RQ errors.
 *
 * <BR>History: 06-10-2004   : Initial version of this class.
 * @author Chris Bizer chris@bizer.de
 * @version V0.1
 */

public class D2RQException extends JenaException {

    /** Construct an exception with given error message */
    public D2RQException( String message ) {
        super( message );
    }

    /** Construct an exception with given error message */
    public D2RQException(String message, Exception e) {
        super( message, e ); 
    }

}

