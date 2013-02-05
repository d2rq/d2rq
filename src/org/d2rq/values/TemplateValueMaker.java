package org.d2rq.values;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

import org.d2rq.db.ResultRow;
import org.d2rq.db.expr.ColumnExpr;
import org.d2rq.db.expr.Concatenation;
import org.d2rq.db.expr.Conjunction;
import org.d2rq.db.expr.Constant;
import org.d2rq.db.expr.Equality;
import org.d2rq.db.expr.Expression;
import org.d2rq.db.op.DatabaseOp;
import org.d2rq.db.op.OrderOp.OrderSpec;
import org.d2rq.db.renamer.Renamer;
import org.d2rq.db.schema.ColumnName;
import org.d2rq.db.types.DataType.GenericType;
import org.d2rq.db.vendor.Vendor;
import org.d2rq.mapgen.IRIEncoder;
import org.d2rq.nodes.NodeSetFilter;


/**
 * A template that combines one or more database columns into a String. Often
 * used as an IRI pattern for generating URIs from a column's primary key.
 *
 * Templates consist of alternating literal parts and column references.
 * R2RML and the D2RQ mapping language use different syntaxes for writing
 * patterns down as strings:
 * 
 * D2RQ: http://example.com/person/@@ppl.first|urlify@@_@@ppl.last|urlify@@
 * R2RML: http://example.com/person/{first}_{last}
 * 
 * Each column reference can also include an encoding function, an instance
 * of {@link ColumnFunction}, like {@link ColumnFunction#URLIFY}. In R2RML it is always
 * {@link ColumnFunction#ENCODE} for IRI templates and {@link ColumnFunction#IDENTITY} otherwise.
 * In D2RQ, the default encoding function is {@link ColumnFunction#IDENTITY}.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class TemplateValueMaker implements ValueMaker {

	public static class Builder {
		private ArrayList<String> literalParts = new ArrayList<String>();
		private List<ColumnName> columns = new ArrayList<ColumnName>();
		private List<ColumnFunction> functions = new ArrayList<ColumnFunction>();
		private boolean complete = false;
		private Builder() {} // instantiate through #builder()
		public Builder add(ColumnName column) {
			return add(column, IDENTITY);
		}
		public Builder add(ColumnName column, ColumnFunction function) {
			if (!complete) {
				add("");
			}
			columns.add(column);
			functions.add(function);
			complete = false;
			return this;
		}
		public Builder add(String literalPart) {
			if (complete) {
				literalPart = literalParts.remove(literalParts.size() - 1) + literalPart;
			}
			literalParts.add(literalPart);
			complete = true;
			return this;
		}
		public TemplateValueMaker build() {
			if (!complete) {
				add("");
			}
			return new TemplateValueMaker(
					literalParts.toArray(new String[literalParts.size()]),
					columns.toArray(new ColumnName[columns.size()]),
					functions.toArray(new ColumnFunction[functions.size()]));
		}
	}

	public static Builder builder() {
		return new Builder();
	}
	
	private final String[] literalParts;	// size n + 1
	private final ColumnName[] columns;		// size n
	private final ColumnFunction[] functions;	// size n
	private final Set<ColumnName> columnsAsSet;
	private final java.util.regex.Pattern regex;
	
	public TemplateValueMaker(String[] literalParts, ColumnName[] columns, ColumnFunction[] functions) {
		if (literalParts.length == 0 || literalParts.length != columns.length + 1 || 
				columns.length != functions.length) {
			throw new IllegalArgumentException(
					"Broken pattern: " + literalParts + columns + functions);
		}
		this.literalParts = literalParts;
		this.columns = columns;
		this.functions = functions;
		columnsAsSet = new HashSet<ColumnName>(Arrays.asList(columns));
		regex = toRegex();
	}
	
	private java.util.regex.Pattern toRegex() {
		StringBuilder s = new StringBuilder();
		if (!"".equals(literalParts[0])) {
			s.append("\\Q");
			s.append(literalParts[0]);
			s.append("\\E");
		}
		for (int i = 0; i < columns.length; i++) {
			s.append("(.*?)");
			if (!"".equals(literalParts[i + 1])) {
				s.append("\\Q");
				s.append(literalParts[i + 1]);
				s.append("\\E");
			}
		}
		return java.util.regex.Pattern.compile(s.toString(), 
				java.util.regex.Pattern.DOTALL);
	}

	public String[] literalParts() {
		return literalParts;
	}
	
	public ColumnName[] columns() { 
		return columns;
	}
	
	public ColumnFunction[] functions() {
		return functions;
	}

	public String firstLiteralPart() {
		return literalParts[0];
	}
	
	public String lastLiteralPart() {
		return literalParts[literalParts.length - 1];
	}
	
	public void describeSelf(NodeSetFilter c) {
		c.limitValuesToPattern(this);
	}

	public boolean matches(String value) {
		if (value == null) return false;
		Matcher match = regex.matcher(value);
		if (!match.matches()) return false;
		for (int i = 0; i < columns.length; i++) {
			if (functions[i].decode(match.group(i + 1)) == null) {
				return false;
			}
		}
		return true;
	}
	
	public Expression valueExpression(String value, DatabaseOp table, Vendor vendor) {
		if (value == null) {
			return Expression.FALSE;
		}
		Matcher match = this.regex.matcher(value);
		if (!match.matches()) {
			return Expression.FALSE;
		}
		Collection<Expression> expressions = new ArrayList<Expression>(columns.length);
		for (int i = 0; i < columns.length; i++) {
			String attributeValue = functions[i].decode(match.group(i + 1));
			if (attributeValue == null) {
				return Expression.FALSE;
			}
			expressions.add(Equality.createColumnValue(columns[i], 
					attributeValue, table.getColumnType(columns[i])));
		}
		return Conjunction.create(expressions);
	}

	public Set<ColumnName> getRequiredColumns() { 
		return columnsAsSet;
	}
	
	/**
	 * Constructs a String from the pattern using the given database row.
	 * @param row a database row
	 * @return the pattern's value for the given row
	 */
	public String makeValue(ResultRow row) {
		int index = 0;
		StringBuilder result = new StringBuilder(literalParts[0]);
		while (index < columns.length) {
			String value = row.get(columns[index]);
			if (value == null) {
				return null;
			}
			value = functions[index].encode(value);
			if (value == null) {
				return null;
			}
			result.append(value);
			index++;
			result.append(literalParts[index]);
		}
		return result.toString();
	}
	
	public List<OrderSpec> orderSpecs(boolean ascending) {
		List<OrderSpec> result = new ArrayList<OrderSpec>(columns.length);
		for (ColumnName column: columns) {
			result.add(new OrderSpec(new ColumnExpr(column), ascending));
		}
		return result;
	}
	
	public String toString() {
		StringBuilder s = new StringBuilder(literalParts[0]);
		for (int i = 0; i < columns.length; i++) {
			s.append("{");
			s.append(columns[i]);
			if (functions[i].name() != null) {
				s.append("|");
				s.append(functions[i].name());
			}
			s.append("}");
			s.append(literalParts[i + 1]);
		}
		return s.toString();
	}

	public boolean equals(Object otherObject) {
		if (!(otherObject instanceof TemplateValueMaker)) {
			return false;
		}
		TemplateValueMaker other = (TemplateValueMaker) otherObject;
		return Arrays.equals(literalParts, other.literalParts) &&
				Arrays.equals(columns, other.columns) && 
				Arrays.equals(functions, other.functions);
	}
	
	public int hashCode() {
		return Arrays.hashCode(literalParts) ^ Arrays.hashCode(columns) ^ 
				Arrays.hashCode(functions) ^ 17;
	}
	
	/**
	 * @return <code>true</code> if the pattern is identical or differs only in 
	 * the column names
	 */
	public boolean isEquivalentTo(TemplateValueMaker p) {
		return Arrays.equals(literalParts, p.literalParts)
				&& Arrays.equals(functions, p.functions);
	}
	
	public ValueMaker rename(Renamer renames) {
		ColumnName[] newColumns = new ColumnName[columns.length];
		for (int i = 0; i < columns.length; i++) {
			newColumns[i] = renames.applyTo(columns[i]);
		}
		return new TemplateValueMaker(literalParts, newColumns, functions);
	}
	
	// FIXME: This doesn't take column functions other than IDENTITY into account
	// The usesColumnFunctions() method is here to allow detection of this case. 
	public Expression toExpression() {
		List<Expression> parts = new ArrayList<Expression>(literalParts.length + columns.length);
		parts.add(Constant.create(literalParts[0], GenericType.CHARACTER));
		for (int i = 0; i < columns.length; i++) {
			parts.add(new ColumnExpr(columns[i]));
			parts.add(Constant.create(literalParts[i + 1], GenericType.CHARACTER));
		}
		return Concatenation.create(parts);
	}
	
	/**
	 * @return TRUE if this pattern uses any column function (encode, urlify, etc.)
	 */
	public boolean usesColumnFunctions() {
		for (ColumnFunction f: functions) {
			if (f != IDENTITY) return true;
		}
		return false;
	}
	
	public final static ColumnFunction IDENTITY = new IdentityFunction();
	public final static ColumnFunction URLENCODE = new URLEncodeFunction();
	public final static ColumnFunction URLIFY = new URLifyFunction();
	public final static ColumnFunction ENCODE = new EncodeFunction();
	
	public interface ColumnFunction {
		String encode(String s);
		String decode(String s);
		String name();
	}
	
	static class IdentityFunction implements ColumnFunction {
		public String encode(String s) { return s; }
		public String decode(String s) { return s; }
		public String name() { return null; }
	}
	
	static class URLEncodeFunction implements ColumnFunction {
		public String encode(String s) {
			try {
				return URLEncoder.encode(s, "utf-8");
			} catch (UnsupportedEncodingException ex) {
				// Can't happen, UTF-8 is always supported
				throw new RuntimeException(ex);
			}
		}
		public String decode(String s) {
			try {
				return URLDecoder.decode(s, "utf-8");
			} catch (UnsupportedEncodingException ex) {
				// Can't happen, UTF-8 is always supported
				throw new RuntimeException(ex);
			} catch (IllegalArgumentException ex) {
				// Broken encoding
				return null;
			}
		}
		public String name() { return "urlencode"; }
	}
	static class URLifyFunction implements ColumnFunction {
		public String encode(String s) {
			try {
				// Note, replacing _ with %5F causes trouble in some browsers
				// (Chrome) because they decode %5F back to _ before requesting
				// the URL.
				return URLEncoder.encode(s, "utf-8")
						.replaceAll("_", "%5F").replace('+', '_');
			} catch (UnsupportedEncodingException ex) {
				// Can't happen, UTF-8 is always supported
				throw new RuntimeException(ex);
			}
		}
		public String decode(String s) {
			try {
				return URLDecoder.decode(s.replace('_', '+'), "utf-8");
			} catch (UnsupportedEncodingException ex) {
				// Can't happen, UTF-8 is always supported
				throw new RuntimeException(ex);
			} catch (IllegalArgumentException ex) {
				// Broken encoding
				return null;
			}
		}
		public String name() { return "urlify"; }
	}
	public static class EncodeFunction implements ColumnFunction {
		public String encode(String s) {
			return IRIEncoder.encode(s);
		}
		public String decode(String s) {
			try {
				return URLDecoder.decode(s.replaceAll("%20", "+"), "utf-8");
			} catch (UnsupportedEncodingException ex) {
				// Can't happen, UTF-8 is always supported
				throw new RuntimeException(ex);
			} catch (IllegalArgumentException ex) {
				// Broken encoding
				return null;
			}
		}
		public String name() { return "encode"; }
	}
}