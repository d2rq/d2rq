package org.d2rq.lang;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;

import org.d2rq.HSQLDatabase;
import org.d2rq.algebra.DownloadRelation;
import org.d2rq.algebra.TripleRelation;
import org.d2rq.db.ResultRow;
import org.d2rq.db.op.DistinctOp;
import org.d2rq.db.op.ProjectionSpec;
import org.d2rq.db.op.TableOp;
import org.d2rq.db.op.OpVisitor;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.lang.ClassMap;
import org.d2rq.lang.CompiledD2RQMapping;
import org.d2rq.lang.D2RQCompiler;
import org.d2rq.lang.Database;
import org.d2rq.lang.DownloadMap;
import org.d2rq.lang.Mapping;
import org.d2rq.lang.Microsyntax;
import org.d2rq.lang.PropertyBridge;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.RDF;


public class D2RQCompilerTest {
	private HSQLDatabase sql;
	private Mapping mapping;
	private Database database;
	private Resource class1;
	private String pattern1;
	private ColumnName t_id;
	
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
		pb1.addJoin("T.REF = A.ID");
		pb1.addAlias("T AS A");
		assertEquals("URI(Pattern(http://test/@@A.ID@@))", 
				firstTripleRelation().getBindingMaker().get(TripleRelation.OBJECT).toString());
	}
	
	@Test
	public void testConditionsInRefersToClassMapAreRenamed() {
		sql.executeSQL("CREATE TABLE T (ID INT PRIMARY KEY, REF INT)");
		ClassMap cm1 = ClassMap.create(null, pattern1, mapping);
		cm1.addCondition("T.ID < 100");
		PropertyBridge pb1 = PropertyBridge.create(null, DC.relation, cm1);
		pb1.setRefersToClassMap(cm1);
		pb1.addJoin("T.REF = A.ID");
		pb1.addAlias("T AS A");
		assertTrue(firstTripleRelation().getBaseTabular().toString().contains("\"T\".\"ID\" < 100"));
		assertTrue(firstTripleRelation().getBaseTabular().toString().contains("\"A\".\"ID\" < 100"));
	}
	
	@Test
	public void testJoinsInRefersToClassMapAreRenamed() {
		sql.executeSQL("CREATE TABLE T (ID INT PRIMARY KEY, REF INT)");
		sql.executeSQL("CREATE TABLE OTHER (ID INT PRIMARY KEY)");
		ClassMap cm1 = ClassMap.create(null, pattern1, mapping);
		cm1.addJoin("T.ID = OTHER.ID");
		PropertyBridge pb1 = PropertyBridge.create(null, DC.relation, cm1);
		pb1.setRefersToClassMap(cm1);
		pb1.addJoin("T.REF = A.ID");
		pb1.addAlias("T AS A");
		assertTrue(firstTripleRelation().getBaseTabular().toString().contains("(\"OTHER\".\"ID\"=\"T\".\"ID\")"));
		assertTrue(firstTripleRelation().getBaseTabular().toString().contains("(\"A\".\"ID\"=\"OTHER\".\"ID\")"));
	}
	
	public void testAliasesInRefersToClassMapAreRenamed() {
		sql.executeSQL("CREATE TABLE X (ID INT PRIMARY KEY, REF INT)");
		ClassMap cm1 = ClassMap.create(null, pattern1, mapping);
		cm1.addAlias("X as T");
		PropertyBridge pb1 = PropertyBridge.create(null, DC.relation, cm1);
		pb1.setRefersToClassMap(cm1);
		pb1.addJoin("T.REF = A.ID");
		pb1.addAlias("T AS A");
		assertTrue(firstTripleRelation().getBaseTabular().toString().contains("\"T\".\"ID\" <=> \"OTHER\".\"ID\""));
		assertTrue(firstTripleRelation().getBaseTabular().toString().contains("\"A\".\"ID\" <=> \"OTHER\".\"ID\""));
	}

	@Test
	public void testColumnsInClassMapTabular() {
		sql.executeSQL("CREATE TABLE T (ID INT)");
		ClassMap cm1 = ClassMap.create(null, pattern1, mapping);
		cm1.setContainsDuplicates(true);
		PropertyBridge.create(null, RDF.type, cm1).setConstantValue(class1);
		assertEquals(
				Collections.singletonList(t_id), 
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
						new ResultRow(null) {public String get(ProjectionSpec column) {return null;}}));
		assertEquals(Microsyntax.parseColumn("People.pic"), d.getContentDownloadColumn());
		assertEquals("URI(Pattern(http://example.org/downloads/@@People.ID@@))", 
				d.nodeMaker(DownloadRelation.RESOURCE).toString());
		assertFalse(d.getBaseTabular().getUniqueKeys().isEmpty());
		assertEquals(Arrays.asList(new ColumnName[]{
					Microsyntax.parseColumn("People.ID"),
					Microsyntax.parseColumn("People.pic")}), 
				d.getBaseTabular().getColumns());
	}
	
	private TripleRelation firstTripleRelation() {
		return new D2RQCompiler(mapping).getResult().getTripleRelations().iterator().next();
	}
}
