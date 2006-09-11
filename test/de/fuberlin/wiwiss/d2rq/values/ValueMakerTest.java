package de.fuberlin.wiwiss.d2rq.values;

import junit.framework.TestCase;

public class ValueMakerTest extends TestCase {

	public void testBlankNodeIDToString() {
		BlankNodeID b = new BlankNodeID("table.col1,table.col2", "classmap1");
		assertEquals("BlankNodeID(Column(table.col1),Column(table.col2))", b.toString());
	}
}
