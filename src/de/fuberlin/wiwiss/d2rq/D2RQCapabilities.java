package de.fuberlin.wiwiss.d2rq;

import com.hp.hpl.jena.graph.Capabilities;

/**
 * Description of the capabilities of a GraphD2RQ.
 * @author Chris Bizer chris@bizer.de
 * @version $Id: D2RQCapabilities.java,v 1.5 2006/09/03 00:08:12 cyganiak Exp $
 */
class D2RQCapabilities implements Capabilities {
    public boolean sizeAccurate() { return true; }
    public boolean addAllowed() { return addAllowed( false ); }
    public boolean addAllowed( boolean every ) { return false; }
    public boolean deleteAllowed() { return deleteAllowed( false ); }
    public boolean deleteAllowed( boolean every ) { return false; }
    public boolean canBeEmpty() { return true; }
    public boolean iteratorRemoveAllowed() { return false; }
    public boolean findContractSafe() { return false; }
    public boolean handlesLiteralTyping() { return true; }
}