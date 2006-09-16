package de.fuberlin.wiwiss.d2rq.values;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import de.fuberlin.wiwiss.d2rq.algebra.ColumnRenamer;
import de.fuberlin.wiwiss.d2rq.nodes.NodeSetFilter;
import de.fuberlin.wiwiss.d2rq.sql.ResultRow;

/**
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: ValueDecorator.java,v 1.3 2006/09/16 14:19:20 cyganiak Exp $
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
	private List constraints;
	private Translator translator;
	
	public ValueDecorator(ValueMaker base, List constraints) {
		this(base, constraints, Translator.identity);
	}

	public ValueDecorator(ValueMaker base, List constraints, Translator translator) {
		this.base = base;
		this.constraints = constraints;
		this.translator = translator;
	}
	
	public Map attributeConditions(String value) {
		return this.base.attributeConditions(this.translator.toDBValue(value));
	}

	public String makeValue(ResultRow row) {
		return this.translator.toRDFValue(this.base.makeValue(row));
	}

	public void describeSelf(NodeSetFilter c) {
		this.base.describeSelf(c);
	}

	public boolean matches(String value) {
		Iterator it = this.constraints.iterator();
		while (it.hasNext()) {
			ValueConstraint constraint = (ValueConstraint) it.next();
			if (!constraint.matches(value)) {
				return false;
			}
		}
		return this.base.matches(this.translator.toDBValue(value));
	}

	public Set projectionAttributes() {
		return this.base.projectionAttributes();
	}
	
	public ValueMaker replaceColumns(ColumnRenamer renamer) {
		return new ValueDecorator(this.base.replaceColumns(renamer), this.constraints, this.translator);
	}
	
	public interface ValueConstraint {
		boolean matches(String value);
	}
	
	public String toString() {
		StringBuffer result = new StringBuffer();
		if (!this.translator.equals(Translator.identity)) {
			result.append(this.translator);
			result.append("(");
		}
		result.append(this.base.toString());
		Iterator it = this.constraints.iterator();
		if (it.hasNext()) {
			result.append(":");
		}
		while (it.hasNext()) {
			result.append(it.next());
			if (it.hasNext()) {
				result.append("&&");
			}
		}
		if (!this.translator.equals(Translator.identity)) {
			result.append(")");
		}
		return result.toString();
	}
}