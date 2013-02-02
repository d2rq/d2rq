package org.d2rq.lang;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.d2rq.D2RQException;
import org.d2rq.HSQLDatabase;
import org.d2rq.algebra.DownloadRelation;
import org.d2rq.algebra.TripleRelation;
import org.d2rq.db.ResultRow;
import org.d2rq.db.op.DistinctOp;
import org.d2rq.db.op.OpVisitor;
import org.d2rq.db.op.TableOp;
import org.d2rq.db.schema.ColumnList;
import org.d2rq.db.schema.ColumnName;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.RDF;


public class D2RQCompilerTest {
	private HSQLDatabase sql;
	private Mapping mapping;
	private Database database;
	private Resource class1;
	private String pattern1;
	private ColumnName t_id, t_col1;
	private Join t_ref_a_id;
	
	@Before
	public void setUp() {
		sql = new HSQLDatabase("test");
		database = new Database(ResourceFactory.createResource());
		database.setJdbcURL(sql.getJdbcURL());
		database.setUsername(sql.getUser());
		mapping = new Mapping();
		mapping.addDatabase(database);
		class1 = ResourceFactory.createResource("http://test/Class1");
		pattern1 = "http://test/@@T.ID@@";
		t_id = Microsyntax.parseColumn("T.ID");
		t_col1 = Microsyntax.parseColumn("T.COL1");
		t_ref_a_id = Microsyntax.parseJoin("T.REF = A.ID");
	}
	
	@After
	public void tearDown() {
		sql.close(true);
	}
	
	@Test
	public void testColumnNamesInRefersToClassMapAreRenamed() {
		sql.executeSQL("CREATE TABLE T (ID INT PRIMARY KEY, REF INT)");
		ClassMap cm1 = ClassMap.create(null, pattern1, mapping);
		PropertyBridge pb1 = PropertyBridge.create(null, DC.relation, cm1);
		pb1.setRefersToClassMap(cm1);
		pb1.addJoin(t_ref_a_id);
		pb1.addAlias(Microsyntax.parseAlias("T AS A"));
		assertEquals("URI(http://test/{\"A\".\"ID\"})", 
				firstTripleRelation().getBindingMaker().get(TripleRelation.OBJECT).toString());
	}
	
	@Test
	public void testConditionsInRefersToClassMapAreRenamed() {
		sql.executeSQL("CREATE TABLE T (ID INT PRIMARY KEY, REF INT)");
		ClassMap cm1 = ClassMap.create(null, pattern1, mapping);
		cm1.addCondition("T.ID < 100");
		PropertyBridge pb1 = PropertyBridge.create(null, DC.relation, cm1);
		pb1.setRefersToClassMap(cm1);
		pb1.addJoin(t_ref_a_id);
		pb1.addAlias(Microsyntax.parseAlias("T AS A"));
		assertTrue(firstTripleRelation().getBaseTabular().toString().contains("\"T\".\"ID\" < 100"));
		assertTrue(firstTripleRelation().getBaseTabular().toString().contains("\"A\".\"ID\" < 100"));
	}
	
	@Test
	public void testJoinsInRefersToClassMapAreRenamed() {
		sql.executeSQL("CREATE TABLE T (ID INT PRIMARY KEY, REF INT)");
		sql.executeSQL("CREATE TABLE OTHER (ID INT PRIMARY KEY)");
		ClassMap cm1 = ClassMap.create(null, pattern1, mapping);
		cm1.addJoin(Microsyntax.parseJoin("T.ID = OTHER.ID"));
		PropertyBridge pb1 = PropertyBridge.create(null, DC.relation, cm1);
		pb1.setRefersToClassMap(cm1);
		pb1.addJoin(t_ref_a_id);
		pb1.addAlias(Microsyntax.parseAlias("T AS A"));
		assertTrue(firstTripleRelation().getBaseTabular().toString().contains("(\"OTHER\".\"ID\"=\"T\".\"ID\")"));
		assertTrue(firstTripleRelation().getBaseTabular().toString().contains("(\"A\".\"ID\"=\"OTHER\".\"ID\")"));
	}
	
	public void testAliasesInRefersToClassMapAreRenamed() {
		sql.executeSQL("CREATE TABLE X (ID INT PRIMARY KEY, REF INT)");
		ClassMap cm1 = ClassMap.create(null, pattern1, mapping);
		cm1.addAlias(Microsyntax.parseAlias("X as T"));
		PropertyBridge pb1 = PropertyBridge.create(null, DC.relation, cm1);
		pb1.setRefersToClassMap(cm1);
		pb1.addJoin(t_ref_a_id);
		pb1.addAlias(Microsyntax.parseAlias("T AS A"));
		assertTrue(firstTripleRelation().getBaseTabular().toString().contains("\"T\".\"ID\" <=> \"OTHER\".\"ID\""));
		assertTrue(firstTripleRelation().getBaseTabular().toString().contains("\"A\".\"ID\" <=> \"OTHER\".\"ID\""));
	}

	@Test
	public void testColumnsInClassMapTabular() {
		sql.executeSQL("CREATE TABLE T (ID INT)");
		ClassMap cm1 = ClassMap.create(null, pattern1, mapping);
		cm1.setContainsDuplicates(true);
		PropertyBridge.create(null, RDF.type, cm1).setConstantValue(class1);
		assertEquals(ColumnList.create(t_id), 
				firstTripleRelation().getBaseTabular().getColumns());
	}
	
	@Test
	public void testClassMapWithDuplicatesHasDistinct() {
		sql.executeSQL("CREATE TABLE T (ID INT)");
		ClassMap cm1 = ClassMap.create(null, pattern1, mapping);
		cm1.setContainsDuplicates(true);
		PropertyBridge.create(null, RDF.type, cm1).setConstantValue(class1);
		firstTripleRelation().getBaseTabular().accept(new OpVisitor.Default(true) {
			@Override
			public boolean visitEnter(DistinctOp table) {
				return false;
			}
			@Override
			public void visit(TableOp table) {
				fail("Expected Distinct(...) missing: " + table);
			}
		});
		// DISTINCT creates a unique key
		assertFalse(firstTripleRelation().getBaseTabular().getUniqueKeys().isEmpty());
	}
	
	@Test
	public void testClassMapWithoutDuplicates() {
		sql.executeSQL("CREATE TABLE T (ID INT)");
		ClassMap cm1 = ClassMap.create(null, pattern1, mapping);
		// This asserts we have no duplicates. It's the default anyway for class maps. So we need no DISTINCT.
		cm1.setContainsDuplicates(false);
		PropertyBridge.create(null, RDF.type, cm1).setConstantValue(class1);
		// Check that we have no DISTINCT
		firstTripleRelation().getBaseTabular().accept(new OpVisitor.Default(true) {
			@Override
			public boolean visitEnter(DistinctOp table) {
				fail("There should be no Distinct(...) in " + table);
				return false;
			}
		});
		// Check that the asserted unique key is present
		assertFalse(firstTripleRelation().getBaseTabular().getUniqueKeys().isEmpty());
	}
	
	@Test
	public void testClassMapWithPrimaryKeyNeedsNoDistinct() {
		sql.executeSQL("CREATE TABLE T (ID INT PRIMARY KEY)");
		ClassMap cm1 = ClassMap.create(null, pattern1, mapping);
		PropertyBridge.create(null, RDF.type, cm1).setConstantValue(class1);
		firstTripleRelation().getBaseTabular().accept(new OpVisitor.Default(true) {
			@Override
			public boolean visitEnter(DistinctOp table) {
				fail("There should be no Distinct(...) in " + table);
				return false;
			}
		});
		// Primary key
		assertFalse(firstTripleRelation().getBaseTabular().getUniqueKeys().isEmpty());
	}
	
	@Test
	public void testDownloadMap() {
		sql.executeSQL("CREATE TABLE \"People\" (\"ID\" INT PRIMARY KEY, \"pic\" VARCHAR(50))");
		DownloadMap dl = new DownloadMap(ResourceFactory.createResource());
		dl.setDatabase(database);
		dl.setURIPattern("http://example.org/downloads/@@People.ID@@");
		dl.setMediaType("image/png");
		dl.setContentDownloadColumn("People.pic");
		mapping.addDownloadMap(dl);
		CompiledD2RQMapping compiled = mapping.compile();
		assertEquals(1, compiled.getDownloadRelations().size());
		DownloadRelation d = compiled.getDownloadRelations().iterator().next();
		assertNotNull(d);
		assertEquals(Node.createLiteral("image/png"), 
				d.nodeMaker(DownloadRelation.MEDIA_TYPE).makeNode(
						new ResultRow(null) {public String get(ColumnName column) {return null;}}));
		assertEquals(Microsyntax.parseColumn("People.pic"), d.getContentDownloadColumn());
		assertEquals("URI(http://example.org/downloads/{\"People\".\"ID\"})", 
				d.nodeMaker(DownloadRelation.RESOURCE).toString());
		assertFalse(d.getBaseTabular().getUniqueKeys().isEmpty());
		assertEquals(ColumnList.create(
					Microsyntax.parseColumn("People.ID"),
					Microsyntax.parseColumn("People.pic")), 
				d.getBaseTabular().getColumns());
	}
	
	@Test
	public void testCatchUnsupportedDatatype() {
		sql.executeSQL("CREATE TABLE T (COL1 OTHER)");
		ClassMap.create(null, "http://example.com/@@T.COL1@@", mapping).addClass(FOAF.Person);
		try {
			firstTripleRelation();
			fail("Expected error due to unsupported datatype OTHER");
		} catch (D2RQException ex) {
			assertEquals(D2RQException.DATATYPE_UNMAPPABLE, ex.errorCode());
			assertTrue(ex.getMessage().contains("OTHER"));
			assertTrue(ex.getMessage().contains("\"T\".\"COL1\""));
		}
	}
	
	@Test
	public void testOverrideDatatype() {
		sql.executeSQL("CREATE TABLE T (COL1 OTHER)");
		database.addTextColumn("T.COL1");
		ClassMap.create(null, "http://example.com/@@T.COL1@@", mapping).addClass(FOAF.Person);
		assertFalse(firstTripleRelation().getBaseTabular().getColumnType(t_col1).isUnsupported());
		assertEquals("VARCHAR",
				firstTripleRelation().getBaseTabular().getColumnType(t_col1).name());
	}
	
	@Test
	public void testNonExistingTable() {
		ClassMap.create(null, "http://example.com/@@TBL.COL1@@", mapping).addClass(FOAF.Person);
		try {
			firstTripleRelation();
			fail("Expected error due to non-existing table TBL");
		} catch (D2RQException ex) {
			assertEquals(D2RQException.SQL_TABLE_NOT_FOUND, ex.errorCode());
			assertTrue("Error message was: " + ex.getMessage(), 
					ex.getMessage().contains("TBL"));
		}
	}
	
	@Test
	public void testNonExistingTableWithAlias() {
		ClassMap cm1 = ClassMap.create(null, "http://example.com/@@ALIAS.COL1@@", mapping);
		cm1.addClass(FOAF.Person);
		cm1.addAlias(Microsyntax.parseAlias("TBL AS ALIAS"));
		try {
			firstTripleRelation();
			fail("Expected error due to non-existing table TBL");
		} catch (D2RQException ex) {
			assertEquals(D2RQException.SQL_TABLE_NOT_FOUND, ex.errorCode());
			assertTrue("Error message was: " + ex.getMessage(), 
					ex.getMessage().contains("TBL"));
		}
	}

	@Test
	public void testNonExistingTableInCondition() {
		sql.executeSQL("CREATE TABLE T (COL1 INT)");
		ClassMap cm1 = ClassMap.create(null, "http://example.com/@@T.COL1@@", mapping);
		cm1.addClass(FOAF.Person);
		cm1.addCondition("TBL.COL1>0");
		try {
			firstTripleRelation();
			fail("Expected error due to non-existing table TBL");
		} catch (D2RQException ex) {
			assertEquals(D2RQException.SQL_TABLE_NOT_FOUND, ex.errorCode());
			assertTrue("Error message was: " + ex.getMessage(), 
					ex.getMessage().contains("TBL"));
		}
	}
	
	@Test
	public void testNonExistingTableInJoin() {
		sql.executeSQL("CREATE TABLE T (COL1 INT)");
		ClassMap cm1 = ClassMap.create(null, "http://example.com/@@T.COL1@@", mapping);
		cm1.addClass(FOAF.Person);
		cm1.addJoin(Microsyntax.parseJoin("T.COL1=TBL.COL2"));
		try {
			firstTripleRelation();
			fail("Expected error due to non-existing table TBL");
		} catch (D2RQException ex) {
			assertEquals(D2RQException.SQL_TABLE_NOT_FOUND, ex.errorCode());
			assertTrue("Error message was: " + ex.getMessage(), 
					ex.getMessage().contains("TBL"));
		}
	}
	
	@Test
	public void testNonExistingColumn() {
		sql.executeSQL("CREATE TABLE TBL (COL1 INT)");
		ClassMap.create(null, "http://example.com/@@TBL.COL2@@", mapping).addClass(FOAF.Person);
		try {
			firstTripleRelation();
			fail("Expected error due to non-existing table TBL.COL2");
		} catch (D2RQException ex) {
			assertEquals(D2RQException.SQL_COLUMN_NOT_FOUND, ex.errorCode());
			assertTrue("Error message was: " + ex.getMessage(), 
					ex.getMessage().contains("\"TBL\".\"COL2\""));
		}
	}
	
	@Test
	public void testNonExistingColumnWithAlias() {
		sql.executeSQL("CREATE TABLE TBL (COL1 INT)");
		ClassMap cm1 = ClassMap.create(null, "http://example.com/@@ALIAS.COL2@@", mapping);
		cm1.addClass(FOAF.Person);
		cm1.addAlias(Microsyntax.parseAlias("TBL AS ALIAS"));
		try {
			firstTripleRelation();
			fail("Expected error due to non-existing table TBL.COL2");
		} catch (D2RQException ex) {
			assertEquals(D2RQException.SQL_COLUMN_NOT_FOUND, ex.errorCode());
			assertTrue("Error message was: " + ex.getMessage(), 
					ex.getMessage().contains("\"ALIAS\".\"COL2\""));
		}
	}

	@Test
	public void testNonExistingColumnInCondition() {
		sql.executeSQL("CREATE TABLE TBL (COL1 INT)");
		ClassMap cm1 = ClassMap.create(null, "http://example.com/@@TBL.COL1@@", mapping);
		cm1.addClass(FOAF.Person);
		cm1.addCondition("TBL.COL2>0");
		try {
			firstTripleRelation();
			fail("Expected error due to non-existing table TBL.COL2");
		} catch (D2RQException ex) {
			assertEquals(D2RQException.SQL_COLUMN_NOT_FOUND, ex.errorCode());
			assertTrue("Error message was: " + ex.getMessage(), 
					ex.getMessage().contains("\"TBL\".\"COL2\""));
		}
	}
	
	@Test
	public void testNonExistingColumnInJoin() {
		sql.executeSQL("CREATE TABLE T (COL1 INT)");
		sql.executeSQL("CREATE TABLE TBL (COL1 INT)");
		ClassMap cm1 = ClassMap.create(null, "http://example.com/@@T.COL1@@", mapping);
		cm1.addClass(FOAF.Person);
		cm1.addJoin(Microsyntax.parseJoin("T.COL1=TBL.COL2"));
		try {
			firstTripleRelation();
			fail("Expected error due to non-existing table TBL.COL2");
		} catch (D2RQException ex) {
			assertEquals(D2RQException.SQL_COLUMN_NOT_FOUND, ex.errorCode());
			assertTrue("Error message was: " + ex.getMessage(), 
					ex.getMessage().contains("\"TBL\".\"COL2\""));
		}
	}
	
	private TripleRelation firstTripleRelation() {
		return new D2RQCompiler(mapping).getResult().getTripleRelations().iterator().next();
	}
}
