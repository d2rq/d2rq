package org.d2rq.mapgen;

import java.util.List;

import org.d2rq.D2RQException;
import org.d2rq.db.SQLConnection;
import org.d2rq.db.schema.ForeignKey;
import org.d2rq.db.schema.Identifier;
import org.d2rq.db.schema.TableName;
import org.d2rq.db.types.DataType;
import org.d2rq.r2rml.Mapping;
import org.d2rq.values.TemplateValueMaker;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

public class R2RMLTarget implements Target {
	private Mapping mapping = null;
	
	public R2RMLTarget() {
		throw new D2RQException("Can't generate R2RML yet", D2RQException.NOT_YET_IMPLEMENTED);
	}
	
	public Mapping getMapping() {
		return mapping;
	}
	
	public void init(String baseIRI, Resource generatedOntology, 
			boolean serveVocabulary, boolean generateDefinitionLabels) {
		mapping = new Mapping(baseIRI);
		// TODO Auto-generated method stub

	}

	public void addPrefix(String prefix, String uri) {
		// TODO Auto-generated method stub

	}

	public void generateDatabase(SQLConnection connection,
			String startupSQLScript) {
		// TODO Auto-generated method stub

	}

	public void generateEntities(Resource class_, TableName table,
			TemplateValueMaker iriTemplate, List<Identifier> blankNodeColumns) {
		// TODO Auto-generated method stub

	}

	public void generateEntityLabels(TemplateValueMaker labelTemplate,
			TableName table) {
		// TODO Auto-generated method stub

	}

	public void generateColumnProperty(Property property, TableName table,
			Identifier column, DataType datatype) {
		// TODO Auto-generated method stub

	}

	public void generateRefProperty(Property property, TableName table,
			ForeignKey foreignKey) {
		// TODO Auto-generated method stub

	}

	public void generateLinkProperty(Property property, TableName table,
			ForeignKey fk1, ForeignKey fk2) {
		// TODO Auto-generated method stub

	}

	public void skipColumn(TableName table, Identifier column, String reason) {
		// TODO Auto-generated method stub

	}

	public void close() {
		// TODO Auto-generated method stub

	}

}
