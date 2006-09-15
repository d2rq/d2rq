package de.fuberlin.wiwiss.d2rq.map;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import junit.framework.TestCase;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import de.fuberlin.wiwiss.d2rq.algebra.AliasMap;
import de.fuberlin.wiwiss.d2rq.algebra.RDFRelation;

public class CompileTest extends TestCase {
	private Model model;
	private Mapping mapping;
	private Database database;
	private ClassMap employees;
	private PropertyBridge managerBridge;
	
	public void setUp() {
		this.model = ModelFactory.createDefaultModel();
		this.mapping = new Mapping();
		this.database = new Database(this.model.createResource());
		this.mapping.addDatabase(this.database);
		this.employees = new ClassMap(this.model.createResource());
		this.employees.setDatabase(this.database);
		this.employees.setURIPattern("http://test/employee@@e.ID@@");
		this.employees.addAlias("employees AS e");
		this.employees.addJoin("e.ID = foo.bar");
		this.employees.addCondition("e.status = 'active'");
		this.mapping.addClassMap(this.employees);
		this.managerBridge = new PropertyBridge(this.model.createResource());
		this.managerBridge.setBelongsToClassMap(this.employees);
		this.managerBridge.addProperty(this.model.createProperty("http://terms.example.org/manager"));
		this.managerBridge.addAlias("e AS m");
		this.managerBridge.setRefersToClassMap(this.employees);
		this.managerBridge.addJoin("e.manager = m.ID");
		this.employees.addPropertyBridge(this.managerBridge);
	}
	
	public void testAttributesInRefersToClassMapAreRenamed() {
		RDFRelation relation = (RDFRelation) this.managerBridge.toRDFRelations().iterator().next();
		assertEquals("URI(Pattern(http://test/employee@@e.ID@@))", relation.nodeMaker(0).toString());
		assertEquals("URI(Pattern(http://test/employee@@m.ID@@))", relation.nodeMaker(2).toString());
	}
	
	public void testJoinConditionsInRefersToClassMapAreRenamed() {
		RDFRelation relation = (RDFRelation) this.managerBridge.toRDFRelations().iterator().next();
		Set joinsToString = new HashSet();
		Iterator it = relation.baseRelation().joinConditions().iterator();
		while (it.hasNext()) {
			joinsToString.add(it.next().toString());
		}
		assertEquals(new HashSet(Arrays.asList(new String[]{
				"Join(e.manager <=> m.ID)", 
				"Join(foo.bar <=> m.ID)",
				"Join(e.ID <=> foo.bar)"})),
				joinsToString);
	}
	
	public void testConditionInRefersToClassMapIsRenamed() {
		RDFRelation relation = (RDFRelation) this.managerBridge.toRDFRelations().iterator().next();
		assertEquals("Expression(e.status = 'active' AND m.status = 'active')",
				relation.baseRelation().condition().toString());
	}

	public void testAliasesInRefersToClassMapAreRenamed() {
		RDFRelation relation = (RDFRelation) this.managerBridge.toRDFRelations().iterator().next();
		assertEquals(
				AliasMap.buildFromSQL(Arrays.asList(new String[]{"employees AS e", "employees AS m"})),
				relation.baseRelation().aliases());
	}
}
