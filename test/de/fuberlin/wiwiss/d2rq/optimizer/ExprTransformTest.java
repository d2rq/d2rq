/**
 */
package de.fuberlin.wiwiss.d2rq.optimizer;

import com.hp.hpl.jena.sparql.expr.Expr;
import com.hp.hpl.jena.sparql.util.ExprUtils;

import de.fuberlin.wiwiss.d2rq.optimizer.utility.DeMorganLawApplyer;
import de.fuberlin.wiwiss.d2rq.optimizer.utility.DistributiveLawApplyer;
import junit.framework.TestCase;

/**
 * @author dorgon
 *
 */
public class ExprTransformTest extends TestCase {

	public void testExprDeMorganDoubleNot() {
		Expr expr = ExprUtils.parse("!(!(?a))");
		DeMorganLawApplyer apply = new DeMorganLawApplyer();
		expr.visit(apply);
		assertEquals("?a", apply.result().toString());
	}
	
	public void testExprDeMorganAnd() {
		Expr expr = ExprUtils.parse("!(?a && ?b)");
		DeMorganLawApplyer apply = new DeMorganLawApplyer();
		expr.visit(apply);
		assertEquals("( ( ! ?a ) || ( ! ?b ) )", apply.result().toString());
	}

	public void testExprDeMorganOr() {
		Expr expr = ExprUtils.parse("!(?a || ?b)");
		DeMorganLawApplyer apply = new DeMorganLawApplyer();
		expr.visit(apply);
		assertEquals("( ( ! ?a ) && ( ! ?b ) )", apply.result().toString());
	}

	public void testExprDistributiveOr() {
		Expr expr = ExprUtils.parse("(( ?a && ?b ) || ?c )");
		DistributiveLawApplyer apply = new DistributiveLawApplyer();
		expr.visit(apply);
		assertEquals("( ( ?a || ?c ) && ( ?b || ?c ) )", apply.result().toString());
	}

	public void testExprDistributiveAnd() {
		Expr expr = ExprUtils.parse("?c || ( ?a && ?b )");
		DistributiveLawApplyer apply = new DistributiveLawApplyer();
		expr.visit(apply);
		assertEquals("( ( ?c || ?a ) && ( ?c || ?b ) )", apply.result().toString());
	}

	public void testExprDistributiveX() {
		Expr expr = ExprUtils.parse("(?c || ( ?a && ?b )) || (?d && ?e)");
		DistributiveLawApplyer apply = new DistributiveLawApplyer();
		expr.visit(apply);
		assertEquals("( ( ( ( ?c || ?a ) || ?d ) && ( ( ?c || ?b ) || ?d ) ) && ( ( ( ?c || ?a ) || ?e ) && ( ( ?c || ?b ) || ?e ) ) )", apply.result().toString()); // correct
	}

}