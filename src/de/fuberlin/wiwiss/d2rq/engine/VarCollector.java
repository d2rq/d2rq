package de.fuberlin.wiwiss.d2rq.engine;

import java.util.HashSet;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.algebra.OpVisitorBase;
import com.hp.hpl.jena.sparql.algebra.OpWalker;
import com.hp.hpl.jena.sparql.algebra.op.OpAssign;
import com.hp.hpl.jena.sparql.algebra.op.OpBGP;
import com.hp.hpl.jena.sparql.algebra.op.OpDatasetNames;
import com.hp.hpl.jena.sparql.algebra.op.OpExtend;
import com.hp.hpl.jena.sparql.algebra.op.OpGraph;
import com.hp.hpl.jena.sparql.algebra.op.OpPath;
import com.hp.hpl.jena.sparql.algebra.op.OpQuad;
import com.hp.hpl.jena.sparql.algebra.op.OpQuadPattern;
import com.hp.hpl.jena.sparql.algebra.op.OpTable;
import com.hp.hpl.jena.sparql.algebra.op.OpTriple;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.sparql.core.Var;

/**
 * Collects the variables mentioned in an {@link Op} and its
 * children.
 * 
 * @author Herwig Leimer
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class VarCollector extends OpVisitorBase {
	
	/**
	 * @return All variables mentioned in the op and its children
	 */
	public static Set<Var> mentionedVars(Op op) {
		VarCollector collector = new VarCollector();
		OpWalker.walk(op, collector);
		return collector.mentionedVariables();
	}
	
	private Set<Var> variables = new HashSet<Var>();
	
	public Set<Var> mentionedVariables() {
		return variables;
	}
	
	@Override
	public void visit(OpBGP opBGP) {
		for (Triple triple: opBGP.getPattern()) {
			visit(triple);
		}
	}
	
	@Override
	public void visit(OpTriple opTriple) {
		visit(opTriple.getTriple());
	}
	
	@Override
	public void visit(OpQuadPattern quadPattern) {
		for (Quad quad: quadPattern.getPattern()) {
			visit(quad);
		}
	}
	
	@Override
	public void visit(OpQuad opQuad) {
		visit(opQuad.getQuad());
	}

	@Override
	public void visit(OpGraph opGraph) {
		visit(opGraph.getNode());
	}

	@Override
	public void visit(OpDatasetNames dsNames) {
		visit(dsNames.getGraphNode());
	}

	@Override
	public void visit(OpAssign opAssign) {
		variables.addAll(opAssign.getVarExprList().getVars());
	}

	@Override
	public void visit(OpExtend opExtend) {
		variables.addAll(opExtend.getVarExprList().getVars());
	}

	@Override
	public void visit(OpTable opTable) {
		variables.addAll(opTable.getTable().getVars());
	}

	private void visit(Triple triple) {
		visit(triple.getSubject());
		visit(triple.getPredicate());
		visit(triple.getObject());
	}

	private void visit(Quad quad) {
		visit(quad.asTriple());
		visit(quad.getGraph());
	}
	
	private void visit(Node node) {
		if (node == null) return;
        if (node.isVariable()) {
        	variables.add((Var) node);
        }
	}

	public void visit(OpPath opPath) {
		visit(opPath.getTriplePath().asTriple());
	}
}
