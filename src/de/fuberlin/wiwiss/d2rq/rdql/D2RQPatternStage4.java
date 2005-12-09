/*
  (c) Copyright 2005 by Joerg Garbers (jgarbers@zedat.fu-berlin.de)
*/

package de.fuberlin.wiwiss.d2rq.rdql;


import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.query.Domain;
import com.hp.hpl.jena.graph.query.Expression;
import com.hp.hpl.jena.graph.query.ExpressionSet;
import com.hp.hpl.jena.graph.query.Mapping;
import com.hp.hpl.jena.graph.query.PatternStage;
import com.hp.hpl.jena.graph.query.Pattern;
import com.hp.hpl.jena.graph.query.Pipe;
import com.hp.hpl.jena.graph.query.ValuatorSet;
import com.hp.hpl.jena.util.iterator.ClosableIterator;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.NiceIterator;

import de.fuberlin.wiwiss.d2rq.GraphD2RQ;
import de.fuberlin.wiwiss.d2rq.find.TripleQuery;
import de.fuberlin.wiwiss.d2rq.map.Database;


/**
 * Instances of this {@link com.hp.hpl.jena.graph.query.Stage} are created by {@link D2RQQueryHandler} to handle 
 * a set of query triples that by D2RQ-mapping refer to the same database.
 * Created by Joerg Garbers on 25.02.05.
 * 
 * @author jg
 * @since V0.3
 * @see CombinedPatternStage
 * @see PatternQueryCombiner
 */
public class D2RQPatternStage4 extends CombinedPatternStage4 {
    // TODO keep just one instance of PatternQueryCombiner and update Property Bridges
    // only when updated with previous stage (see varInfo.boundDomainIndexToShared)

	private GraphD2RQ graph;
	protected Map candidateBridgeLists; // Database -> List[] 
	protected List[] soleCandidateBridgeLists;
	boolean refersToMultipleDatabases;
    BitSet multipleDatabasesMarker;


	// instanciate just one PatternQueryCombiner? it could do some caching
	// or leave the caching for graph? e.g. triple -> list of bridges

	public D2RQPatternStage4(GraphD2RQ graph, Mapping map,
			ExpressionSet constraints, Triple[] triples) {
		super((Graph) graph, map, constraints, triples);
		// some contraints are eaten up at this point!
		// so use s.th. like a clone() and setter method at invocation time
		D2RQPatternStage4.this.graph = graph;
	}
	
	public void setup() {
		Triple[] query=stageInfo.queryTriples;
		Triple[] mostGeneral=GraphUtils.varsToANY(query);
		candidateBridgeLists=GraphUtils.makeDatabaseToPrefixedPropertyBridges(graph,mostGeneral,false);
		multipleDatabasesMarker=new BitSet(query.length);
		mayYieldResults=GraphUtils.existsEntryInEveryPosition(candidateBridgeLists,query.length,multipleDatabasesMarker);
		if (!mayYieldResults)
			return;
		refersToMultipleDatabases=candidateBridgeLists.size()>1;
		if (refersToMultipleDatabases) 
			stageInfo.setupForPartsProcessing();
		else
			stageInfo.setupForAllProcessing();
	}
	
	private List[] getSoleCandidateBridgeLists() {
		if (soleCandidateBridgeLists==null) {
			Iterator it=candidateBridgeLists.values().iterator();
			List[] first=null;
			if (it.hasNext())
				first=(List[])it.next();
			if (it.hasNext())
				throw new RuntimeException("more than one element");
			soleCandidateBridgeLists=first;
		}
		return soleCandidateBridgeLists;
	}

	/**
	 * Sets up {@link PatternQueryCombiner} and returns its resultTriplesIterator.
	 * Passes stage information to the PatternQueryCombiner.
	 * @param triples identical to this.triples
	 * @return an iterator. Each result is a possible and full instanciation of
	 * triples according to D2RQ. 
	 */
	protected ClosableIterator resultIteratorForTriplePattern(Triple[] triples, int nextToFind, Object aboutPrevious) {
		if (!refersToMultipleDatabases) {
			List[] candidates=getSoleCandidateBridgeLists();
			return makeCombinedIterator(candidates,triples,stageInfo.vars.allBindings,stageInfo.vars.allExpressions);
		} else {
			Map reducedCandidateBridgeLists;
			if (aboutPrevious==null)
				reducedCandidateBridgeLists=candidateBridgeLists;
			else {
				Database previousDatabase=(Database)aboutPrevious;
				reducedCandidateBridgeLists=new HashMap();
				reducedCandidateBridgeLists.putAll(candidateBridgeLists);
				reducedCandidateBridgeLists.remove(previousDatabase);
			}
			Map cuttedCandidateBridgeLists=new HashMap();
			GraphUtils.cutLists(reducedCandidateBridgeLists,nextToFind,cuttedCandidateBridgeLists,true);
			ExtendedIterator ret=NiceIterator.emptyIterator();
			Iterator it=cuttedCandidateBridgeLists.entrySet().iterator();
			while (it.hasNext()) {
		        Map.Entry e=(Map.Entry)it.next();
		        List[] dbBridges=(List[])e.getValue();
        		    Triple[] triplesCut=new Triple[dbBridges.length];
        		    System.arraycopy(triples,nextToFind,triplesCut,0,triplesCut.length);
				PatternQueryCombiner4 combiner = new PatternQueryCombiner4(graph, dbBridges, triplesCut); 
				combiner.setCareForPossible(false);
				combiner.setup();
				int maxlen=combiner.possibleLength();
		        for (int len=maxlen; len>0; len--) {
		        	  // either full length or devide where options from other databases
		        	  if (len==maxlen || multipleDatabasesMarker.get(nextToFind+len)) {
		        		TripleQuery[][] tripleQueries=new TripleQuery[len][];
			        	System.arraycopy(combiner.tripleQueries,nextToFind,tripleQueries,0,len);
			        	int partEnd=nextToFind+len-1;
			        PQCResultIterator4 item=makeCombinedIterator(tripleQueries,stageInfo.vars.partBindings[nextToFind][partEnd],stageInfo.vars.partExpressions[nextToFind][partEnd]);
			        if (item!=null)
			        	  ret.andThen(item);		        		  
		        	  }
		        }
			}
			return ret;
		}
	}


	private PQCResultIterator4 makeCombinedIterator(List[] candidates, Triple[] triples, VariableBindings variableBindings, Collection constraints) {
		PatternQueryCombiner4 combiner = new PatternQueryCombiner4(graph, candidates, triples); 
		combiner.setup();
		if (!combiner.possible)
			return null;
		return makeCombinedIterator(combiner.tripleQueries,variableBindings,constraints);
	}
	private PQCResultIterator4 makeCombinedIterator(TripleQuery[][] tripleQueries, VariableBindings variableBindings, Collection constraints) {
		PQCResultIterator4 it=new PQCResultIterator4(tripleQueries, 
				variableBindings, constraints);
		return it;
	}
	
} // class
