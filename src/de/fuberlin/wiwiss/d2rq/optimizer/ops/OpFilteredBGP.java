package de.fuberlin.wiwiss.d2rq.optimizer.ops;

import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.algebra.op.OpBGP;
import com.hp.hpl.jena.sparql.core.BasicPattern;

/**
 * A OpFilteredBGP is like a an OpBGP, but with the difference, that it has
 * a link to its parent. This is needed in the TransformD2RQAndRemoveUnneededOps-Transformer
 * to get access to the partent which is an opfilter and to integrate the filterconditions
 * into the OpD2RQ. 
 * 
 * @author Herwig Leimer
 *
 */
public class OpFilteredBGP extends OpBGP 
{
	private Op parent;	// link to the parent-operator
	
	/**
	 * Constructor
	 * @param basicPattern
	 */
	public OpFilteredBGP(BasicPattern basicPattern)
	{
		super(basicPattern);
	}
	
	/**
	 * Parent-Op
	 * @return Op - Parent-Op
	 */
	public Op getParent() 
	{
		return parent;
	}

	/**
	 * Links to a parent-op
	 * @param parent - parent-op
	 */
	public void setParent(Op parent) 
	{
		this.parent = parent;
	}
	
	/**
	 * Name
	 */
	public String getName() 
	{
		return "filteredBGP";
	}
}
