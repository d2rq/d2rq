package de.fuberlin.wiwiss.d2rq.expr;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;
import de.fuberlin.wiwiss.d2rq.algebra.AliasMap;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.ColumnRenamerMap;
import de.fuberlin.wiwiss.d2rq.algebra.RelationName;
import de.fuberlin.wiwiss.d2rq.algebra.AliasMap.Alias;

public class SQLExpressionTest extends TestCase {

	public void testCreate() {
		Expression e = SQLExpression.create("papers.publish = 1");
		assertEquals("SQL(papers.publish = 1)", e.toString());
		assertFalse(e.isTrue());
		assertFalse(e.isFalse());
	}
	
	public void testFindsColumns() {
		Expression e = SQLExpression.create("papers.publish = 1 AND papers.url1 != 'http://www.example.com\\'http://www.example.com' AND papers.url2 != 'http://www.example.com\\\\\\\\http://www.example.com' AND papers.rating > 4");
		Set<Attribute> expectedColumns = new HashSet<Attribute>(Arrays.asList(
				new Attribute[]{new Attribute(null, "papers", "publish"), 
						new Attribute(null, "papers", "url1"),
						new Attribute(null, "papers", "url2"),
						new Attribute(null, "papers", "rating")}));
		assertEquals(expectedColumns, e.attributes());
	}
	
	public void testToString() {
		Expression e = SQLExpression.create("papers.publish = 1");
		assertEquals("SQL(papers.publish = 1)", e.toString());
	}
	
	public void testTwoExpressionsAreEqual() {
		assertEquals(SQLExpression.create("1=1"), SQLExpression.create("1=1"));
		assertEquals(SQLExpression.create("1=1").hashCode(), SQLExpression.create("1=1").hashCode());
	}
	
	public void testTwoExpressionsAreNotEqual() {
		assertFalse(SQLExpression.create("1=1").equals(SQLExpression.create("2=2")));
		assertFalse(SQLExpression.create("1=1").hashCode() == SQLExpression.create("2=2").hashCode());
	}
	
	public void testRenameColumnsWithAliasMap() {
		Alias a = new Alias(new RelationName(null, "foo"), new RelationName(null, "bar"));
		assertEquals(SQLExpression.create("bar.col1 = baz.col1"),
				SQLExpression.create("foo.col1 = baz.col1").renameAttributes(
						new AliasMap(Collections.singleton(a))));
	}
	
	public void testRenameColumnsWithColumnReplacer() {
		Map<Attribute,Attribute> map = new HashMap<Attribute,Attribute>();
		map.put(new Attribute(null, "foo", "col1"), new Attribute(null, "foo", "col2"));
		assertEquals(SQLExpression.create("foo.col2=foo.col3"), 
				SQLExpression.create("foo.col1=foo.col3").renameAttributes(new ColumnRenamerMap(map)));
	}
}
