package org.d2rq.mapgen;

import java.util.List;

import org.d2rq.db.schema.ColumnDef;
import org.d2rq.db.schema.ForeignKey;
import org.d2rq.db.schema.Identifier;
import org.d2rq.db.schema.Key;
import org.d2rq.db.schema.TableDef;
import org.d2rq.db.schema.TableName;
import org.d2rq.values.TemplateValueMaker;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * A style of default mapping. Allows customizing the mapping generator to
 * generate different default mappings. Encapsulates the differences between
 * W3C's Direct Mapping and the old D2RQ-style default mapping.
 */
public interface MappingStyle {

	/**
	 * @return A mapping generator that creates a mapping according to this style
	 */
	MappingGenerator getMappingGenerator();
	
	/**
	 * Returns an IRI template to be used to uniquely identify the records
	 * in a table. Can be invoked with a null argument to indicate that
	 * all records should receive the same IRI.
	 * @param table The table definition
	 * @param columns The primary or unique key to be used, or null
	 * @return An IRI template
	 */
	TemplateValueMaker getEntityIRIPattern(TableDef table, Key columns);

	/**
	 * Returns a list of columns to be used to form blank node identifiers
	 * for the records in a table that has no suitable unique/primary key
	 * @param table The table definition
	 * @return A column list
	 */
	List<Identifier> getEntityPseudoKeyColumns(List<ColumnDef> columns);

	/**
	 * Creates a resource that represents the ontology that defines all
	 * generated classes and properties.
	 * @return An ontology resource representing the generated terms
	 */
	Resource getGeneratedOntologyResource();

	/**
	 * Creates a class for a table.
	 * @param tableName The table
	 * @return A class resource representing the table
	 */
	Resource getTableClass(TableName tableName);
	
	/**
	 * Creates a property for a table column.
	 * @param tableName The table
	 * @param column A column on the table
	 * @return A property representing the column
	 */
	Property getColumnProperty(TableName tableName, Identifier column);

	/**
	 * Creates a property for a foreign key relationship between two tables.
	 * @param tableName The source table
	 * @param foreginKey A foreign key defines on the source table
	 * @return A property representing the relationship
	 */
	Property getForeignKeyProperty(TableName tableName, ForeignKey foreignKey);

	/**
	 * Creates a property for an N:M link table.
	 * @param linkTable A relationship table
	 * @return A property representing the table
	 */
	Property getLinkProperty(TableName linkTable);
	
	/**
	 * Creates a best-effort template for human-readable labels for the records
	 * in a table, based on a given list of columns. 
	 * @param tableName The table
	 * @param columns A list of columns in the table 
	 * @return A template for generating labels for the records
	 */
	TemplateValueMaker getEntityLabelPattern(TableName tableName, Key columns);
}
