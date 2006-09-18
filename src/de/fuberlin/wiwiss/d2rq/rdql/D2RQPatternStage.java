package de.fuberlin.wiwiss.d2rq.rdql;

import java.util.Collection;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.query.Bind;
import com.hp.hpl.jena.graph.query.Bound;
import com.hp.hpl.jena.graph.query.BufferPipe;
import com.hp.hpl.jena.graph.query.ExpressionSet;
import com.hp.hpl.jena.graph.query.Fixed;
import com.hp.hpl.jena.graph.query.Mapping;
import com.hp.hpl.jena.graph.query.PatternStage;
import com.hp.hpl.jena.graph.query.Pipe;
import com.hp.hpl.jena.graph.query.Stage;

import de.fuberlin.wiwiss.d2rq.fastpath.FastpathEngine;

/**
 * TODO Review comments
 *  
 * Instances of this {@link com.hp.hpl.jena.graph.query.Stage} are created by {@link D2RQQueryHandler} to handle 
 * a set of query triples that by D2RQ-mapping refer to the same database.
 * Created by Joerg Garbers on 25.02.05.
 *
 * A CombinedPatternStage is a {@link Stage} that handles a conjunction of triples (a pattern). 
 * This class is a corrected, clarified and optimized version of {@link PatternStage}.
 * In the following we try to document the logic and machinery behind it:
 * <p>
 * An RDQL query (<code>triples</code>) contains nodes, some of which are
 * (shared) variables (a-z) which will be bound by successive Stages. 
 * This Stage for example sees variables a,b,c,i,j,k,x,y,z in <code>triples</code>.
 * The type of the triple nodes (fixed, bound or bind Element) is 
 * <code>compiled</code> according to the following schema:
 * <p>
 * The mapping <code>map</code> lists variables a,b,c...h (pointers to Domain indices)
 * that will be bound by a previous stage and put into the Pipe. ({@link Bound})
 * This stage will pick up each binding (on its own thread), 
 * substitute variables with existing bindings, and produce additional bindings.
 * This stage is first to bind some variables i,j,k,x,y,z (1st time {@link Bind})
 * Some variables x,y,z are used in more than one triple/node => (2nd time: {@link Bound}) 
 * <p>
 * Some variables l,m,n...u,v,w will still not be bound by this stage and left over 
 * for the next stage.
 * 
 * compiled: for each query triple the compiled node binding information ({@link Bind}, {@link Bound}, {@link Fixed})
 * guard: a condition checker for the conditions that come with the query and can
 * be checked after this stage found a matching solution for the variables, 
 * for example (?x < ?y)
 * varInfo: fast lookup information for different types of variables (shared, bind, bound)
 * triples: list of find-triples with inserted bindings by previous stages, 
 * build each time a binding arrives on the {@link Pipe}.
 * 
 * Pulls variable bindings from the previous stage adds bindings and pushes all
 * into the following stage.
 * A more efficient version of PatternStage
 * Includes its run(Pipe,Pipe) and nest() functionality.
 * Handles the case, where bindings for <code>triples</code> are not found triple by triple,
 * but in one step. This is the case for D2RQ.
 * <p>
 * In our example:
 *  a,b,c get bound by source. They are substituted by asTripleMatch(). (Bound)
 *  i,j,k,x,y,z  are bound by p.match() the first time they are seen. (Bind)
 *  x,y,z are checked by p.match() the second time they are seen. (Bound)
 *  
 * @author jg
 * @version $Id: D2RQPatternStage.java,v 1.12 2006/09/18 19:06:54 cyganiak Exp $
 */
public class D2RQPatternStage extends Stage {
	private Collection rdfRelations;
	private Mapping map;
	private ExpressionSet expressions;
	private Triple[] triplePattern;
	private FastpathEngine fastpathEngine;

	public D2RQPatternStage(Collection rdfRelations, Mapping map,
			ExpressionSet expressions, Triple[] triplePattern) {
		this.rdfRelations = rdfRelations;
		this.map = map;
		this.expressions = expressions;
		this.triplePattern = triplePattern;
	}

	/**
	 * Realizes the piping between the previous, this and the following {@link Stage}.
	 * A new thread is created for this stage.
	 * @see PatternStage
	 */
	public Pipe deliver(final Pipe output) {
		Pipe input = new BufferPipe();
		this.previous.deliver(input);
		this.fastpathEngine = new FastpathEngine(
				input, output,
				this.rdfRelations, this.map, this.expressions, this.triplePattern);
		// TODO Don't start the thread if FastpathEngine.mayYieldResults is false
		new Thread() {
			public void run() {
				try {
					fastpathEngine.execute();
					output.close();
				} catch (Exception ex) {
					// Hand the exception up to the next stage;
					// it will be re-thrown there and eventually
					// be thrown into the caller code. If we
					// didn't do this, the stage thread would just
					// die, and the caller thread would wait
					// indefinitely.
					output.close(ex);
				}
			}
		}.start();
		return output;
	}

	/**
	 * Cancel operation of this stage. May be called asynchronously
	 * while the engine is running.  
	 */
	public void close() {
		this.fastpathEngine.cancel();
		super.close();
	}
}
