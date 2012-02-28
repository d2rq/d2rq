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

	public void testExprDeMorganDoubleNotA() {
		Expr expr = ExprUtils.parse("!(!(?a))");
		DeMorganLawApplyer apply = new DeMorganLawApplyer();
		expr.visit(apply);
		assertNotNull(apply.result());
		assertEquals("?a", apply.result().toString());
	}
	
	public void testExprDeMorganDoubleNotAB() {
		Expr expr = ExprUtils.parse("!(!(?a && ?b))");
		DeMorganLawApplyer apply = new DeMorganLawApplyer();
		expr.visit(apply);
		assertNotNull(apply.result());
		assertEquals("( ?a && ?b )", apply.result().toString());
	}
	
	public void testExprDeMorganOr() {
		Expr expr = ExprUtils.parse("!(?a || ?b)");
		DeMorganLawApplyer apply = new DeMorganLawApplyer();
		expr.visit(apply);
		assertNotNull(apply.result());
		assertEquals("( ( ! ?a ) && ( ! ?b ) )", apply.result().toString());
	}

	public void testExprDeMorganAndDontChange() {
		Expr expr = ExprUtils.parse("!(?a && ?b)");
		DeMorganLawApplyer apply = new DeMorganLawApplyer();
		expr.visit(apply);
		assertNotNull(apply.result());
		assertEquals("( ! ( ?a && ?b ) )", apply.result().toString());
	}
	
	public void testExprDistributiveABOrC() {
		Expr expr = ExprUtils.parse("(( ?a && ?b ) || ?c )");
		DistributiveLawApplyer apply = new DistributiveLawApplyer();
		expr.visit(apply);
		assertNotNull(apply.result());
		assertEquals("( ( ?a || ?c ) && ( ?b || ?c ) )", apply.result().toString());
	}

	public void testExprDistributiveCOrAB() {
		Expr expr = ExprUtils.parse("?c || ( ?a && ?b )");
		DistributiveLawApplyer apply = new DistributiveLawApplyer();
		expr.visit(apply);
		assertNotNull(apply.result());
		assertEquals("( ( ?c || ?a ) && ( ?c || ?b ) )", apply.result().toString());
	}

	public void testExprDistributiveAndDontChange() {
		Expr expr = ExprUtils.parse("!(?a || ?b) && ?c");
		DistributiveLawApplyer apply = new DistributiveLawApplyer();
		expr.visit(apply);
		assertNotNull(apply.result());
		assertEquals("( ( ! ( ?a || ?b ) ) && ?c )", apply.result().toString());
	}
	
	public void testExprDistributiveOrComplex() {
		Expr expr = ExprUtils.parse("(?c || ( ?a && ?b )) || (?d && ?e)");
		DistributiveLawApplyer apply = new DistributiveLawApplyer();
		expr.visit(apply);
		assertNotNull(apply.result());
		assertEquals("( ( ( ( ?c || ?a ) || ?d ) && ( ( ?c || ?b ) || ?d ) ) && ( ( ( ?c || ?a ) || ?e ) && ( ( ?c || ?b ) || ?e ) ) )", apply.result().toString()); // correct
	}

	public void testDeMorganNotEqual() {
		Expr expr = ExprUtils.parse("?x != ?z");
		DeMorganLawApplyer apply = new DeMorganLawApplyer();
		expr.visit(apply);
		assertNotNull(apply.result());
		assertEquals("( ?x != ?z )", apply.result().toString());
	}
}
