package org.d2rq.mapgen;

import java.util.List;

import org.d2rq.db.SQLConnection;
import org.d2rq.db.schema.ForeignKey;
import org.d2rq.db.schema.Identifier;
import org.d2rq.db.schema.TableName;
import org.d2rq.db.types.DataType;
import org.d2rq.values.TemplateValueMaker;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;


public interface Target {
	void init(String baseIRI, Resource generatedOntology, 
			boolean serveVocabulary, 
			boolean generateDefinitionLabels);
	void addPrefix(String prefix, String uri);
	void generateDatabase(SQLConnection connection, String startupSQLScript);
	void generateEntities(Resource class_, TableName table,
			TemplateValueMaker iriTemplate, List<Identifier> blankNodeColumns);
	void generateEntityLabels(TemplateValueMaker labelTemplate, TableName table);
	void generateColumnProperty(Property property,
			TableName table, Identifier column, DataType datatype);
	void generateRefProperty(Property property, 
			TableName table, ForeignKey foreignKey);
	void generateLinkProperty(Property property, 
			TableName table, ForeignKey fk1, ForeignKey fk2);
	void skipColumn(TableName table, Identifier column, String reason);
	void close();
}
