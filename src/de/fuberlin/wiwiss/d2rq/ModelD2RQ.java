/*
  (c) Copyright 2004 by Chris Bizer (chris@bizer.de)
*/

package de.fuberlin.wiwiss.d2rq;

import com.hp.hpl.jena.rdf.model.impl.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.enhanced.*;

/** A D2RQ read-only Jena model backed by a non-RDF database.
    * 
    * D2RQ is a declarative mapping language for describing mappings between ontologies and relational data models.
    * More information about D2RQ is found at: http://www.wiwiss.fu-berlin.de/suhl/bizer/d2rq/
    * 
    * <BR>History: 06-14-2004   : Initial version of this class.
    * @author Chris Bizer chris@bizer.de
    * @version V0.1
    *
    * @see de.fuberlin.wiwiss.d2rq.GraphD2RQ
 */
public class ModelD2RQ extends ModelCom implements Model {

    /**  Make a non-RDF database-based model
     *  @param mappingFileName filename of the D2RQ map to be used for this model
     */
    public ModelD2RQ(String mappingFileName)
        { super( new GraphD2RQ(mappingFileName), BuiltinPersonalities.model ); }

    /**  Make a non-RDF database-based model
     *  @param mappingFileName filename of the D2RQ map to be used for this model
     * @param parameter parameter for D2RQ
     */
    public ModelD2RQ(String mappingFileName, String parameter)
        { super( new GraphD2RQ(mappingFileName, parameter), BuiltinPersonalities.model ); }
}


