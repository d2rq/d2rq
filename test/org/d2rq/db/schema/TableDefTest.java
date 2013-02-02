package org.d2rq.db.schema;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.d2rq.db.types.DataType;
import org.d2rq.db.vendor.Vendor;
import org.junit.Before;
import org.junit.Test;

public class TableDefTest {
	private TableName t1, t2;
	private Identifier col1, col2, col3;
	private List<ColumnDef> cols12, cols13;
	private IdentifierList key1;
	private ForeignKey fk2;
	
	@Before
	public void setUp() throws Exception {
		t1 = TableName.parse("t1");
		t2 = TableName.parse("t2");
		col1 = Identifier.createUndelimited("col1");
		col2 = Identifier.createUndelimited("col2");
		col3 = Identifier.createUndelimited("col3");
		cols12 = Arrays.asList(new ColumnDef[]{
				new ColumnDef(col1, DataType.GenericType.NUMERIC.dataTypeFor(Vendor.SQL92), false),
				new ColumnDef(col2, DataType.GenericType.NUMERIC.dataTypeFor(Vendor.SQL92), false),
		});
		cols13 = Arrays.asList(new ColumnDef[]{
				new ColumnDef(col1, DataType.GenericType.NUMERIC.dataTypeFor(Vendor.SQL92), false),
				new ColumnDef(col3, DataType.GenericType.NUMERIC.dataTypeFor(Vendor.SQL92), false),
		});
		key1 = IdentifierList.create(col1);
		fk2 = new ForeignKey(IdentifierList.create(col2), IdentifierList.create(col1), t2);
	}

	@Test
	public void testSameDefEquals() {
		TableDef def1a = new TableDef(t1, cols12, key1, Collections.singleton(key1), Collections.singleton(fk2));
		TableDef def1b = new TableDef(t1, cols12, key1, Collections.singleton(key1), Collections.singleton(fk2));
		assertEquals(def1a, def1b);
		assertEquals(def1a.hashCode(), def1b.hashCode());
	}
	
	@Test
	public void testDifferentTableNamesNotEqual() {
		TableDef def1 = new TableDef(t1, cols12, null, Collections.<IdentifierList>emptySet(), Collections.<ForeignKey>emptySet());
		TableDef def2 = new TableDef(t2, cols12, null, Collections.<IdentifierList>emptySet(), Collections.<ForeignKey>emptySet());
		assertFalse(def1.equals(def2));
		assertFalse(def1.hashCode() == def2.hashCode());
	}
	
	@Test
	public void testDifferentColumnListsNotEqual() {
		TableDef def1 = new TableDef(t1, cols12, null, Collections.<IdentifierList>emptySet(), Collections.<ForeignKey>emptySet());
		TableDef def2 = new TableDef(t1, cols13, null, Collections.<IdentifierList>emptySet(), Collections.<ForeignKey>emptySet());
		assertFalse(def1.equals(def2));
		assertFalse(def1.hashCode() == def2.hashCode());
	}
}
