/*
 * Created on Mar 2, 2005
 *
 */
package de.fuberlin.wiwiss.d2rq.rdql;

import java.util.List;

import de.fuberlin.wiwiss.d2rq.FindTestFramework;
import de.fuberlin.wiwiss.d2rq.map.Column;
import de.fuberlin.wiwiss.d2rq.map.Join;
import de.fuberlin.wiwiss.d2rq.map.PropertyBridge;

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
		String result=prefixer.prefixTable(tableName, "");
		assertEquals(result,prefix+"_"+tableName);		
		result=nullPrefixer.prefixTable(tableName, "");
		assertTrue(result==tableName);
	}

	public void testPrefixColumn() {
		Column c=(Column)prefixer.prefix(column);
		assertPrefixed(prefixer, c.getQualifiedName(),column.getQualifiedName());
		c=(Column)nullPrefixer.prefix(column);
		assertTrue(c==column);
	}

	public void testPrefixJoin() {
		assertEquals(join.getSQLExpression(),condition);
		Join j=(Join)prefixer.prefix(join);
		assertPrefixed(prefixer, j.getFirstTable(),join.getFirstTable());
		assertPrefixed(prefixer, j.getSecondTable(),join.getSecondTable());
		j=(Join)nullPrefixer.prefix(join);
		assertTrue(j==join);		
	}
	
	public void testPropertyBridge() {
		PropertyBridge p=(PropertyBridge)prefixer.prefix(propertyBridge);
		// TODO
	}
	
	protected void assertPrefixed(TablePrefixer prefixer, String prefixed, String unprefixed) {
		assertEquals(prefixed,prefixer.prefixString(unprefixed));
		assertEquals(prefixer.unprefixString(prefixed),unprefixed);	
	}

}
