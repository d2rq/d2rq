package de.fuberlin.wiwiss.d2rq.sesame;

import java.io.File;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

import org.openrdf.model.BNode;
import org.openrdf.model.Graph;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.sesame.repository.local.LocalRepository;
import org.openrdf.sesame.sail.StatementIterator;

//import com.eaio.uuid.UUID;
import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.graph.Factory;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.impl.LiteralLabel;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;




/**
 * @author tgauss
 * @author oliver-maresch@gmx.de
 */
public class SesameJenaUtilities {
	
	final static public String s_bnodePrefix = "urn:bnode:";

	    
    /**
     * Builds a Sesame Statement out of an Jena Triple.
     * @param t the jena triple
     * @param vf the ValueFactory of the Sesame RdfSource, where the Sesame Statement should be stored.
     * @return the Sesame Statement
     */
    public static Statement makeSesameStatement(Triple t, ValueFactory vf){
        Resource subj = makeSesameSubject(t.getSubject(), vf);
        URI pred = makeSesamePredicate(t.getPredicate(), vf);
        Value obj = makeSesameObject(t.getObject(), vf);
        return new StatementImpl(subj, pred, obj);
    }
	
    /**
     * Builds a Jena subject node from a Sesame Resource.
     */
    public static Node makeJenaSubject(Resource sub){
        if(sub == null){
            return Node.ANY;
        } else if(sub instanceof URI){
            URI uri = (URI) sub;            
            if(uri.getURI().startsWith("urn:bnode:")){
                return Node.createAnon(new AnonId(uri.getURI().substring(10)));
            }else{
                return Node.createURI(uri.getURI());
            }
        } else if(sub instanceof BNode){
            return Node.ANY;
        }
        return null;
    }
    
    /**
     * Builds a Jena predicate Node form a Sesame predicate URI.
     */
    public static Node makeJenaPredicate(URI pred){
        if(pred == null){
            return Node.ANY;
        } else {
            return Node.createURI(pred.getURI());
        }
    }
    
    /**
     * Builds a Jena object Node from a Sesame object Value.
     */
    public static Node makeJenaObject(Value obj){
        Node objnode = null;
        if(obj == null){
            return Node.ANY;
        } else if (obj instanceof Literal) {
            Literal objlit = (Literal) obj;

            String objlabel = objlit.getLabel();
            //String objlang = objlit.getLanguage();
            URI objDatatype = objlit.getDatatype();

            if(objDatatype != null  ){
                Model m = ModelFactory.createDefaultModel();
                RDFDatatype dt = null;
                dt = getTypedLiteral(objDatatype.getURI());
                RDFNode o = m.createTypedLiteral(" "+objlabel,dt);
                objnode = o.asNode();
            }else{
                objnode = Node.createLiteral(objlabel, null, false);
            }
        }else{
            URI objuri = (URI)obj;
            if(objuri.getURI().startsWith("urn:bnode:")){
                objnode = Node.createAnon(new AnonId(objuri.getURI().substring(10)));
            }else{
                objnode = Node.createURI(objuri.getURI());
            }
        }		
        return objnode;
    }
    
    
    /**
     * Builds a Sesame subject Resource from a Jena subject Node.
     */
    public static Resource makeSesameSubject(Node subnode,ValueFactory myFactory){
        Resource mySubject = null;
        if(subnode.isBlank()){
            AnonId id = subnode.getBlankNodeId();
            String subid = id.toString();
            if(subid.startsWith(s_bnodePrefix)){
                mySubject = (URI) myFactory.createURI(subid);
            }else{
                mySubject = (URI) myFactory.createURI(s_bnodePrefix+subid);
            }
        }else{
            mySubject = (URI) myFactory.createURI(subnode.getURI());
        }
        
        return mySubject;
    }
   
    /**
     * Builds a Sesame predicate URI from a Jena predicate Node.
     */
    public static URI makeSesamePredicate(Node prednode,ValueFactory myFactory){
		return myFactory.createURI(prednode.getURI());
    }
    
    /**
     * Builds a Sesame object Value from a Jena object Node.
     */
    public static Value makeSesameObject(Node objnode,ValueFactory myFactory){
		Value myobj = null;
		if(objnode.isBlank()){
				AnonId id = objnode.getBlankNodeId();
				String objid = id.toString();
				if(objid.startsWith(s_bnodePrefix)){
					myobj = (URI) myFactory.createURI(objid);
				}else{
					myobj = (URI) myFactory.createURI(s_bnodePrefix+objid);
				}
		}else{
			if(objnode.isLiteral()){
				LiteralLabel liter = objnode.getLiteral();
				String label = liter.getLexicalForm();
				RDFDatatype dtype = liter.getDatatype();
				String dtypeURI = null;
				if(dtype != null){
					dtypeURI = dtype.getURI();
				}
				if(dtypeURI != null){
					myobj= myFactory.createLiteral(label,myFactory.createURI(dtypeURI));	
				}else{
					if(liter.language()!= null){
						myobj = myFactory.createLiteral(label,liter.language());
					}else{
						myobj= myFactory.createLiteral(label);
					}
                }
			}else{
				myobj = (URI) myFactory.createURI(objnode.getURI());
			}
        }
        
        return myobj;
    }

    /**
	 * Returns the corresponding RDFDatatype to a given URI. 
	 * 
	 * @param dtype         the datatypes uri
	 * @return corresponding RDFDatatype
	 */
	private static RDFDatatype getTypedLiteral(String dtype){
		RDFDatatype dt = TypeMapper.getInstance().getTypeByName(dtype);
		return dt;
	}
	
}