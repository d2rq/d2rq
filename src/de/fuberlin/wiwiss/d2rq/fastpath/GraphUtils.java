package de.fuberlin.wiwiss.d2rq.fastpath;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;

import de.fuberlin.wiwiss.d2rq.algebra.RDFRelation;
import de.fuberlin.wiwiss.d2rq.algebra.TripleRelation;
import de.fuberlin.wiwiss.d2rq.sql.ConnectedDB;

/**
 * @author jgarbers
 * @version $Id: GraphUtils.java,v 1.3 2006/09/28 12:17:43 cyganiak Exp $
 */
public class GraphUtils {

	/**
	 * Creates a bunch of prefixed property bridge copies for each triple.
	 * @return a list array that has a nonempty list at each position or null
	 */
	public static List[] makePrefixedPropertyBridges(Collection rdfRelations, Triple[] triples) {
		List[] bridges=new List[triples.length];
		if (triples.length!=makePrefixedPropertyBridges(rdfRelations,triples,bridges,true))
			return null;
		return bridges;
	}

	public static Map makeDatabaseToPrefixedPropertyBridges(Collection rdfRelations, Triple[] triples, boolean skipIfNotFull) {
		return makeDatabaseToPrefixedPropertyBridges(byDatabase(rdfRelations), triples, skipIfNotFull);
	}

	private static Map byDatabase(Collection rdfRelations) {
		Map result = new HashMap();
		Iterator it = rdfRelations.iterator();
		while (it.hasNext()) {
			RDFRelation bridge = (RDFRelation) it.next();
			ConnectedDB db = bridge.baseRelation().database();
			List list = (List) result.get(db);
			if (list == null) {
				list = new ArrayList();
				result.put(db, list);
			}
			list.add(bridge);
		}
		return result;
	}
	
	/**
	 * Creates a map from database to List[] of prefixed PropertyBridges.
	 * @param input Database -> List of PropertyBridge
	 * @param triples
	 * @param skipIfNotFull if true return only List[] that have entries at each array position.
	 */
	public static Map makeDatabaseToPrefixedPropertyBridges(Map input, Triple[] triples, boolean skipIfNotFull) {
	    Map ret=new HashMap();
	    Iterator it=input.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry e=(Map.Entry)it.next();
	        ConnectedDB db=(ConnectedDB)e.getKey();
	        List pbCand=(List)e.getValue();
			List[] bridges=new List[triples.length];
			int len=makePrefixedPropertyBridges(pbCand,triples,bridges,false);
			if (len>0) {
			    if (len==triples.length || !skipIfNotFull) {
			        ret.put(db,bridges);
			    }
			}
	    }
	    return ret;
	}
		
	/**
	 * Filters input map to List[] into output map.
	 * @param input object -> List[]
	 * @param inputStart cuts everything left to position inputStart
	 * @param output object -> List[]
	 * @param skipEmpty
	 */
	public static void cutLists(Map input, int inputStart, Map output, boolean skipEmpty) {
	    Iterator it=input.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry e=(Map.Entry)it.next();
	        List[] val=(List[])e.getValue();
	        List[] cut=cutArrayOfLists(val,inputStart,true,true);
	        if (cut.length>0 || !skipEmpty)
	            output.put(e.getKey(),cut);	        
	    }
	}
	
	public static boolean existsEntryInEveryPosition(Map map, int len, BitSet mult) {
	    BitSet exists=new BitSet(len);
	    checkForSources(map,exists,mult);
	    return exists.cardinality()==len;
	}
	/**
	 * Checks if there exists at least one or multiple non empty Lists at each position.
	 * @param map
	 * @param existSource
	 * @param multipleSources
	 */
	public static void checkForSources(Map map, BitSet existSource, BitSet multipleSources) {
	    Iterator it=map.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry e=(Map.Entry)it.next();
	        List[] val=(List[])e.getValue();
	        for (int i=0; i<val.length; i++) {
	            if (val[i]!=null && !val[i].isEmpty()) {
	            	  if (existSource.get(i))
	            		  multipleSources.set(i);
	            	  else
	            		  existSource.set(i);
	            }
	        }
	    }
	}

	/**
	 * Creates an array smaller than input that is cut where the array item is null or empty.
	 * @param input
	 * @param inputStart cuts everything left to position inputStart
	 * @param onNull
	 * @param onEmpty
	 */
	public static List[] cutArrayOfLists(List[] input, int inputStart, boolean onNull, boolean onEmpty) {
		int i;
		for (i=inputStart; i<input.length; i++) {
	        List list=input[i];
		    boolean cut=(onNull && list==null) || (onEmpty && list!=null && list.size()==0);
	        if (cut) {
	        	  break;
	        }
	    }
		if (i<input.length || inputStart!=0) {
			List[] output=new List[i-inputStart];
			System.arraycopy(input,inputStart,output,0,i-inputStart);
			return output;
		} else
			return input;
	}
	
	
	/**
	 * Creates a bunch of prefixed property bridge copies for each triple.
	 * @param candidateBridges
	 * @param triples
	 * @param bridges provides store to put results
	 * @param stopEarly if set stops at first empty PropertyBridge list.
	 * @return the number of successful assignments (including empty lists if not stopEarly)
	 */
	public static int makePrefixedPropertyBridges(Collection candidateBridges, Triple[] triples, List[] bridges, boolean stopEarly) {
		int nonEmptyLists=0;
		for (int i=0; i<triples.length; i++) {
			Triple t=triples[i];
			List tBridges = propertyBridgesForTriple(t, candidateBridges);
			bridges[i]=tBridges;
			int bridgesCount=tBridges.size();
			if (bridgesCount==0) {
				if (stopEarly)
					return nonEmptyLists;
				else {
					continue;
				}
			}
		    nonEmptyLists++;
			for (int j=0; j<bridgesCount; j++) {
				TripleRelation bridge = (TripleRelation) tBridges.get(j);
				tBridges.set(j, bridge.withPrefix(i));
			}
		}
		return nonEmptyLists;
	}

	/**
	 * Refines the bridge candidate lists with refined triples.
	 * @param triples
	 * @param bridges
	 */
	public static List[] refinePropertyBridges(List[] bridges, Triple[] triples) {
		if (bridges==null || triples==null || bridges.length!=triples.length)
			return null;
		List[] refined=new List[Math.min(bridges.length,triples.length)];
		if (!refinePropertyBridges(bridges,triples,refined,true))
			return null;
		return refined;
	}

	/**
	 * 
	 * @param bridges
	 * @param triples
	 * @param refined provides store to put results
	 * @param stopEarly stopEarly if set stops at first empty PropertyBridge list.
	 */
	public static boolean refinePropertyBridges(List[] bridges, Triple[] triples, List[] refined, boolean stopEarly) {
		boolean fullSuccess=true;
		for (int i=0; i<refined.length; i++) {
			Triple t=triples[i];
			List tBridges = propertyBridgesForTriple(t, bridges[i]);
			if (tBridges.size()==0) {
				if (stopEarly)
					return false;
				else {
					fullSuccess=false;
				}
			}
			refined[i]=tBridges;
		}
		return fullSuccess;
	}
	
	public static Node varToANY(Node node) {
		if (node.isVariable())
			return Node.ANY;
		return node;
	}
	public static Triple varsToANY(Triple triple) {
		Node s,p,o;
		s=triple.getSubject();
		p=triple.getPredicate();
		o=triple.getObject();
		return new Triple(varToANY(s),varToANY(p),varToANY(o));
	}
	public static Triple[] varsToANY(Triple[] triples) {
		Triple[] ret=new Triple[triples.length];
		for (int i=0; i<triples.length; i++)
			ret[i]=varsToANY(triples[i]);
		return ret;
	}

	/**
	 * Finds all property bridges from a collection that match a triple.
	 * @param t the triple
	 * @param propertyListCandidates the collection (elements must be of type PropertyBridge)
	 * @return list of items from pbIt
	 */
	public static ArrayList propertyBridgesForTriple(Triple t, Collection propertyListCandidates) { // PropertyBridge[]
		ArrayList list=new ArrayList(2);
		Iterator pbIt=propertyListCandidates.iterator();
		while (pbIt.hasNext()) {
			TripleRelation bridge = (TripleRelation) pbIt.next();
			if (bridge.selectTriple(t).equals(RDFRelation.EMPTY)) {
				continue;
			}
			list.add(bridge);
		}
		return list;
	}
}
