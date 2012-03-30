package de.fuberlin.wiwiss.d2rq.values;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import de.fuberlin.wiwiss.d2rq.algebra.ColumnRenamer;
import de.fuberlin.wiwiss.d2rq.algebra.OrderSpec;
import de.fuberlin.wiwiss.d2rq.algebra.ProjectionSpec;
import de.fuberlin.wiwiss.d2rq.expr.Expression;
import de.fuberlin.wiwiss.d2rq.nodes.NodeSetFilter;
import de.fuberlin.wiwiss.d2rq.sql.ResultRow;

/**
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class ValueDecorator implements ValueMaker {
	public static ValueConstraint maxLengthConstraint(final int maxLength) {
		return new ValueConstraint() {
			public boolean matches(String value) {
				return value == null || value.length() <= maxLength;
			}
			public String toString() {
				return "maxLength=" + maxLength;
			}
		};
	}
	public static ValueConstraint containsConstraint(final String containsSubstring) {
		return new ValueConstraint() {
			public boolean matches(String value) {
				return value == null || value.indexOf(containsSubstring) >= 0;
			}
			public String toString() {
				return "contains='" + containsSubstring + "'";
			}
		};
	}
	public static ValueConstraint regexConstraint(final String regex) {
		final Pattern pattern = Pattern.compile(regex);
		return new ValueConstraint() {
			public boolean matches(String value) {
				return value == null || pattern.matcher(value).matches();
			}
			public String toString() {
				return "regex='" + regex + "'";
			}
		};
	}
	
	private ValueMaker base;
	private List<ValueConstraint> constraints;
	private Translator translator;
	
	public ValueDecorator(ValueMaker base, List<ValueConstraint> constraints) {
		this(base, constraints, Translator.IDENTITY);
	}

	public ValueDecorator(ValueMaker base, List<ValueConstraint> constraints, Translator translator) {
		this.base = base;
		this.constraints = constraints;
		this.translator = translator;
	}
	
	public String makeValue(ResultRow row) {
		return this.translator.toRDFValue(this.base.makeValue(row));
	}

	public void describeSelf(NodeSetFilter c) {
		c.setUsesTranslator(translator);
		this.base.describeSelf(c);
	}

	public Expression valueExpression(String value) {
		for (ValueConstraint constraint: constraints) {
			if (!constraint.matches(value)) {
				return Expression.FALSE;
			}
		}
		String dbValue = this.translator.toDBValue(value);
		if (dbValue == null) {
			return Expression.FALSE;
		}
		return base.valueExpression(dbValue);
	}

	public Set<ProjectionSpec> projectionSpecs() {
		return this.base.projectionSpecs();
	}
	
	public ValueMaker renameAttributes(ColumnRenamer renamer) {
		return new ValueDecorator(this.base.renameAttributes(renamer), this.constraints, this.translator);
	}
	
	public List<OrderSpec> orderSpecs(boolean ascending) {
		return base.orderSpecs(ascending);
	}
	
	public interface ValueConstraint {
		boolean matches(String value);
	}
	
	public String toString() {
		StringBuffer result = new StringBuffer();
		if (!this.translator.equals(Translator.IDENTITY)) {
			result.append(this.translator);
			result.append("(");
		}
		result.append(this.base.toString());
		Iterator<ValueConstraint> it = this.constraints.iterator();
		if (it.hasNext()) {
			result.append(":");
		}
		while (it.hasNext()) {
			result.append(it.next());
			if (it.hasNext()) {
				result.append("&&");
			}
		}
		if (!this.translator.equals(Translator.IDENTITY)) {
			result.append(")");
		}
		return result.toString();
	}
}