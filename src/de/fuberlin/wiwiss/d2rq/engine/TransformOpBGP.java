package de.fuberlin.wiwiss.d2rq.engine;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.algebra.TransformCopy;
import com.hp.hpl.jena.sparql.algebra.op.OpBGP;
import com.hp.hpl.jena.sparql.algebra.op.OpFilter;
import com.hp.hpl.jena.sparql.expr.Expr;
import com.hp.hpl.jena.sparql.expr.ExprList;

import de.fuberlin.wiwiss.d2rq.algebra.NodeRelation;
import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.map.Mapping;
import de.fuberlin.wiwiss.d2rq.optimizer.expr.TransformExprToSQLApplyer;

/**
 * Translates an OpBGP to an OpUnionTableSQL over a GraphD2RQ.
 * Filter expressions may be specified; attempt to absorb the
 * expressions into the SQL. Leave an OpFilter if not all
 * expressions could be absorbed. 
 * 
 * @author Herwig Leimer
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class TransformOpBGP extends TransformCopy {
	private final static Log log = LogFactory.getLog(TransformOpBGP.class);

	private final Mapping mapping;
	private final boolean useAllOptimizations;
	private final boolean transformFilters;
	
	public TransformOpBGP(Mapping mapping, boolean transformFilters) {
		this.mapping = mapping;
		this.transformFilters = transformFilters;
		this.useAllOptimizations = mapping.configuration().getUseAllOptimizations();
	}
	
	@Override
	public Op transform(OpBGP opBGP) {
		if (transformFilters) {
			return opBGP;
		}
		return createOpD2RQ(opBGP, new ExprList());
	}

	@Override
	public Op transform(OpFilter opFilter, Op subOp) {
		if (!transformFilters || !(opFilter.getSubOp() instanceof OpBGP)) {
			return super.transform(opFilter, subOp);
		}
		return createOpD2RQ((OpBGP) subOp, opFilter.getExprs());
	}

	public Op createOpD2RQ(OpBGP opBGP, ExprList filters) {
        List<NodeRelation> tables = new GraphPatternTranslator(
        		opBGP.getPattern().getList(), mapping.compiledPropertyBridges(), 
        		useAllOptimizations).translate();
        
        if (useAllOptimizations) {
        	log.debug("NodeRelations before applying filters: " + tables.size());
        	ExprList copy = new ExprList(filters);
        	for (Expr filter: copy) {
        		tables = applyFilter(tables, filter, filters);
        	}
        	if (log.isDebugEnabled()) {
        		log.debug("NodeRelations after applying filters: " + tables.size());
        	}
        }
        
        Op op = OpUnionTableSQL.create(tables);
        if (!filters.isEmpty()) {
            op = OpFilter.filter(filters, op);
        }
        return op;
    }
    
    private List<NodeRelation> applyFilter(
    		List<NodeRelation> nodeRelations, Expr filter, ExprList allFilters) {
        List<NodeRelation> result = new ArrayList<NodeRelation>();
        boolean convertable = true;
        for (NodeRelation nodeRelation: nodeRelations) {
        	// TODO: The transformation from Expr to Expression should happen in NodeRelation.select()
            Expression expression = TransformExprToSQLApplyer.convert(filter, nodeRelation);
            if (expression == null) {
            	// the expression cannot be transformed to SQL
                convertable = false;
            } else if (expression.isTrue()) {
            	// keep as is
            } else if (expression.isFalse()) {
                continue;	// skip
            } else {
            	nodeRelation = nodeRelation.select(expression);
            	if (nodeRelation.baseRelation().condition().isFalse()) continue;
            }
            result.add(nodeRelation);
        }
        if (convertable) {
        	log.debug("Removing converted filter: " + filter);
            allFilters.getList().remove(filter);
        } else {
        	log.debug("Filter could not be fully converted and is kept: " + filter);
        }
        return result;
    }
}
