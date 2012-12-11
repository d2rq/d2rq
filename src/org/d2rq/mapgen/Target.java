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


/**
 * A target that receives information from the {@link MappingGenerator} and
 * represents that information in some other form. This allows us to
 * use the same mapping generator code to output in different formats.
 */
public interface Target {

	/**
	 * Called before generation starts.
	 * @param baseIRI A base IRI for absolutizing relative IRI templates
	 * @param generatedOntology A URI that identifies the generated vocabulary
	 * @param serveVocabulary <code>true</code> iff the generated mapping is supposed to include vocabulary definitions
	 * @param generateDefinitionLabels <code>true</code> iff the generated mapping is supposed to include labels for generated classes and properties
	 */
	void init(String baseIRI, Resource generatedOntology, 
			boolean serveVocabulary, boolean generateDefinitionLabels);

	/**
	 * Adds a prefix mapping.
	 */
	void addPrefix(String prefix, String uri);

	/**
	 * Generates a database connection.
	 * @param connection Connection information to the database
	 * @param startupSQLScript A SQL file that was used to populate/initialize the database connection; may be <code>null</code>
	 */
	void generateDatabase(SQLConnection connection, String startupSQLScript);

	/**
	 * Generates a mapping from one table to a set of entities. Either the
	 * IRI template or the list of blank node columns will be given, the other
	 * will be <code>null</code>.
	 * @param class_ RDFS/OWL class of the entities; may be <code>null</code> 
	 * @param table Name of the table
	 * @param iriTemplate IRI template for generating entity IRIs, possibly relative
	 * @param blankNodeColumns List of columns for generating unique blank nodes
	 */
	void generateEntities(Resource class_, TableName table,
			TemplateValueMaker iriTemplate, List<Identifier> blankNodeColumns);

	/**
	 * Generates a label template for the entities of one table, as part of that
	 * table's mapping.
	 * @param labelTemplate Template for the labels
	 * @param table Name of the table
	 */
	void generateEntityLabels(TemplateValueMaker labelTemplate, TableName table);

	/**
	 * Generates a mapping from one column to a property. 
	 * @param property The target property
	 * @param table The source table
	 * @param column The source column
	 * @param datatype The column's datatype; guaranteed to be non-<code>null</code>
	 */
	void generateColumnProperty(Property property,
			TableName table, Identifier column, DataType datatype);

	/**
	 * Generates a mapping from a foreign key to a property.
	 * @param property The target property
	 * @param table The table on which the foreign key constraint is defined
	 * @param foreignKey The foreign key
	 */
	void generateRefProperty(Property property, 
			TableName table, ForeignKey foreignKey);

	/**
	 * Generates a mapping from one N:M table to a property.
	 * @param property The target property
	 * @param table Name of the N:M table
	 * @param fk1 The foreign key constraint on the N:M table that points to the table containing subjects
	 * @param fk2 The foreign key constraint on the N:M table that points to the table containing objects
	 */
	void generateLinkProperty(Property property, 
			TableName table, ForeignKey fk1, ForeignKey fk2);

	/**
	 * Informs the {@link Target} instance that a certain column has been
	 * skipped. May be useful for logging etc.
	 */
	void skipColumn(TableName table, Identifier column, String reason);

	/**
	 * Will be called when generation is complete.
	 */
	void close();
}
