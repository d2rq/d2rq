package org.d2rq.r2rml;

import java.util.HashMap;
import java.util.Map;

import org.d2rq.CompiledMapping;
import org.d2rq.db.SQLConnection;
import org.d2rq.r2rml.TermMap.Position;
import org.d2rq.vocab.RR;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.shared.impl.PrefixMappingImpl;


/**
 * A representation of an R2RML mapping.
 * 
 * It provides getters and
 * setters for everything, allowing programmatic construction and
 * manipulation of mappings.
 * 
 * It can represent certain kinds of invalid R2RML mappings,
 * for example those with required information missing.
 * A call to {@link #isValid()} can be used to check that it's
 * ok.
 * 
 * It represent some extra bits of information that
 * strictly speaking are not part of an R2RML mapping:
 * 
 * <ul>
 * <li>There can be unused mapping components (e.g., a term map
 * instance that is not used by anything). These can be listed
 * via {@link #getUnreferencedMappingComponents()}.</li>
 * <li>There is an associated prefix mapping: {@link #getPrefixMapping()}.</li>
 * <li>Mapping components are stored along with the Jena {@link Resource}
 * that represents it in the mapping graph.</li>
 * </ul> 
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class Mapping extends MappingComponent {
	private final String baseIRI;
	private PrefixMapping prefixes = new PrefixMappingImpl();
	private final ComponentCollection<TriplesMap> triplesMaps = new ComponentCollection<TriplesMap>();
	private final ComponentCollection<LogicalTable> logicalTables = new ComponentCollection<LogicalTable>();
	private final ComponentCollection<TermMap> termMaps = new ComponentCollection<TermMap>();
	private final ComponentCollection<PredicateObjectMap> predicateObjectMaps = new ComponentCollection<PredicateObjectMap>();
	private final ComponentCollection<ReferencingObjectMap> referencingObjectMaps = new ComponentCollection<ReferencingObjectMap>();
	private final ComponentCollection<Join> joins = new ComponentCollection<Join>();

	/**
	 * @param baseIRI Used for resolving relative IRI templates
	 */
	public Mapping(String baseIRI) {
		this.baseIRI = baseIRI;
		prefixes.setNsPrefix("rr", RR.getURI());
	}

	public CompiledMapping compile(SQLConnection sqlConnection) {
		MappingValidator validator = new MappingValidator(this, sqlConnection);
		validator.setThrowExceptionOnError(true);
		validator.run();
		R2RMLCompiler compiler = new R2RMLCompiler(this, sqlConnection);
		compiler.connect();
		return compiler;
	}
	
	public ComponentType getType() {
		return ComponentType.MAPPING;
	}
	
	public String getBaseIRI() {
		return baseIRI;
	}
	
	public void setPrefixMapping(PrefixMapping prefixes) {
		this.prefixes = prefixes; 
	}
	
	public PrefixMapping getPrefixMapping() {
		return prefixes;
	}

	public ComponentCollection<TriplesMap> triplesMaps() {
		return triplesMaps;
	}
	
	public ComponentCollection<LogicalTable> logicalTables() {
		return logicalTables;
	}
	
	public ComponentCollection<TermMap> termMaps() {
		return termMaps;
	}
	
	public ComponentCollection<PredicateObjectMap> predicateObjectMaps() {
		return predicateObjectMaps;
	}
	
	public ComponentCollection<ReferencingObjectMap> referencingObjectMaps() {
		return referencingObjectMaps;
	}
	
	public ComponentCollection<Join> joins() {
		return joins;
	}
	
	public MappingComponent getMappingComponent(Resource r, ComponentType type) {
		switch (type) {
		case TRIPLES_MAP:
			return triplesMaps.get(r);
		case LOGICAL_TABLE:
		case BASE_TABLE_OR_VIEW:
		case R2RML_VIEW:
			return logicalTables.get(r);
		case TERM_MAP:
		case CONSTANT_VALUED_TERM_MAP:
		case COLUMN_VALUED_TERM_MAP:
		case TEMPLATE_VALUED_TERM_MAP:
		case SUBJECT_MAP:
		case PREDICATE_MAP:
		case OBJECT_MAP:
		case GRAPH_MAP:
			return termMaps.get(r);
		case PREDICATE_OBJECT_MAP:
			return predicateObjectMaps.get(r);
		case REF_OBJECT_MAP:
			return referencingObjectMaps.get(r);
		case JOIN:
			return joins.get(r);
		default:
			return null;
		}
	}
	
	@Override
	public void accept(MappingVisitor visitor) {
		visitor.visitComponent(this);
		for (Resource triplesMap: triplesMaps().resources()) {
			visitor.visitComponentProperty(null, triplesMap, ComponentType.TRIPLES_MAP);
		}
	}
	
	@Override
	public boolean isValid() {
		MappingValidator validator = new MappingValidator(this);
		accept(validator);
		return !validator.getReport().hasError();
	}
	
	/**
	 * Returns all mapping components that are not referenced from a
	 * triples map.
	 */
	public Map<Resource,MappingComponent> getUnreferencedMappingComponents() {
		final Map<MappingComponent,Resource> allComponents = new HashMap<MappingComponent,Resource>();
		for (Resource r: logicalTables.resources()) {
			allComponents.put(logicalTables.get(r), r);
		}
		for (Resource r: termMaps.resources()) {
			allComponents.put(termMaps.get(r), r);
		}
		for (Resource r: predicateObjectMaps.resources()) {
			allComponents.put(predicateObjectMaps.get(r), r);
		}
		for (Resource r: referencingObjectMaps.resources()) {
			allComponents.put(referencingObjectMaps.get(r), r);
		}
		for (Resource r: joins.resources()) {
			allComponents.put(joins.get(r), r);
		}
		accept(new MappingVisitor.TreeWalkerImplementation(this) {
			@Override
			public void visitComponent(LogicalTable logicalTable) {
				allComponents.remove(logicalTable);
			}
			@Override
			public void visitComponent(TermMap termMap, Position position) {
				allComponents.remove(termMap);
			}
			@Override
			public void visitComponent(PredicateObjectMap predicateObjectMap) {
				allComponents.remove(predicateObjectMap);
			}
			@Override
			public void visitComponent(ReferencingObjectMap referencingObjectMap) {
				allComponents.remove(referencingObjectMap);
			}
			@Override
			public void visitComponent(Join join) {
				allComponents.remove(join);
			}
		});
		Map<Resource,MappingComponent> result = new HashMap<Resource,MappingComponent>();
		for (MappingComponent key: allComponents.keySet()) {
			result.put(allComponents.get(key), key);
		}
		return result;
	}
}
