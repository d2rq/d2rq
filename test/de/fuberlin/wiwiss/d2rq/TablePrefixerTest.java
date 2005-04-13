/*
 * Created on Mar 2, 2005
 *
 */
package de.fuberlin.wiwiss.d2rq;

import java.util.ArrayList;
import java.util.List;

import de.fuberlin.wiwiss.d2rq.map.Column;
import de.fuberlin.wiwiss.d2rq.map.Join;
import de.fuberlin.wiwiss.d2rq.map.PropertyBridge;

import junit.framework.TestCase;

/**
 * @author jg
 *
 */
public class TablePrefixerTest extends FindTestFramework {
	
	/**
	 * @param arg0
	 */
	public TablePrefixerTest(String arg0) {
		super(arg0);
	}

	String prefix;
	TablePrefixer prefixer;
	TablePrefixer nullPrefixer;
	List propertyBridges;
	PropertyBridge propertyBridge;
	Column column;
	Join join;
	String tableName;
	String condition;
	
	
	/*
	 * @see TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		TablePrefixer.mayPrefixPrefixedString=false;
		prefix="Pre";
		prefixer=new TablePrefixer(prefix);
		nullPrefixer=new TablePrefixer();
		tableName="Table";
		condition="Table1.Col1=Table2.Col2";
		join=Join.buildJoin(condition);
		column=new Column("Table","Col");
	    propertyBridges = graph.getPropertyBridges();
		propertyBridge=(PropertyBridge)propertyBridges.get(0);
	}
	
	/*
	 * @see TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testPrefixString() {
		String prefixed, unprefixed;
		prefixed=prefixer.prefixString(tableName);
		unprefixed=prefixer.unprefixString(prefixed);
		assertEquals(tableName,unprefixed);
		assertTrue(nullPrefixer.prefixString(tableName)==tableName);
	}

	public void testPrefixCollection() {
	}

	public void testPrefixTable() {
		String result=prefixer.prefixTable(tableName);
		assertEquals(result,prefix+"_"+tableName);		
		result=nullPrefixer.prefixTable(tableName);
		assertTrue(result==tableName);
	}

	public void testPrefixColumn() {
		Column c=prefixer.prefixColumn(column);
		assertPrefixed(prefixer, c.getQualifiedName(),column.getQualifiedName());
		c=nullPrefixer.prefixColumn(column);
		assertTrue(c==column);
	}

	public void testPrefixJoin() {
		assertEquals(join.sqlExpression(),condition);
		Join j=prefixer.prefixJoin(join);
		assertPrefixed(prefixer, j.getFirstTable(),join.getFirstTable());
		assertPrefixed(prefixer, j.getSecondTable(),join.getSecondTable());
		j=nullPrefixer.prefixJoin(join);
		assertTrue(j==join);		
	}
	
	public void testPropertyBridge() {
		PropertyBridge p=prefixer.prefixPropertyBridge(propertyBridge);
		// TODO
	}
	
	protected void assertPrefixed(TablePrefixer prefixer, String prefixed, String unprefixed) {
		assertEquals(prefixed,prefixer.prefixString(unprefixed));
		assertEquals(prefixer.unprefixString(prefixed),unprefixed);	
	}

}
