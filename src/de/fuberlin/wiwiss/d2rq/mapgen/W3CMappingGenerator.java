package de.fuberlin.wiwiss.d2rq.mapgen;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.List;

import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;

import de.fuberlin.wiwiss.d2rq.algebra.AliasMap;
import de.fuberlin.wiwiss.d2rq.algebra.Attribute;
import de.fuberlin.wiwiss.d2rq.algebra.Join;
import de.fuberlin.wiwiss.d2rq.algebra.RelationName;
import de.fuberlin.wiwiss.d2rq.dbschema.ColumnType;
import de.fuberlin.wiwiss.d2rq.dbschema.DatabaseSchemaInspector;
import de.fuberlin.wiwiss.d2rq.values.Pattern;

/**
 * Generates a D2RQ mapping compatible with W3C's Direct Mapping by introspecting a database schema.
 * Result is available as a high-quality Turtle serialization, or
 * as a parsed model.
 *  
 * @author Lu&iacute;s Eufrasio (luis.eufrasio@gmail.com)
 */
public class W3CMappingGenerator extends MappingGenerator {

	public W3CMappingGenerator(String jdbcURL) {
		super(jdbcURL);
	}

	@Override
	protected String uriPattern(RelationName tableName) {
		Pattern.EncodeFunction function = new Pattern.EncodeFunction();
		String result = function.encode(this.instanceNamespaceURI + tableName.qualifiedName());
		Iterator it = this.schema.primaryKeyColumns(tableName).iterator();
		int i = 0;
		while (it.hasNext()) {
			result += i == 0 ? "/" : ".";
			i++;
			
			Attribute column = (Attribute) it.next();
			String attributeName = function.encode(column.attributeName());
			String attributeQName = function.encode(column.qualifiedName());
			result += attributeName + "-@@" + attributeQName;
			if (DatabaseSchemaInspector.isStringType(this.schema.columnType(column))) {
				result += "|encode";
			}
			result += "@@";
		}
		return result;
	}
}