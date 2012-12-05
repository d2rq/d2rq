package org.d2rq.values;

import java.net.URI;
import java.util.List;
import java.util.Set;

import org.d2rq.db.ResultRow;
import org.d2rq.db.expr.Concatenation;
import org.d2rq.db.expr.Constant;
import org.d2rq.db.expr.Expression;
import org.d2rq.db.op.DatabaseOp;
import org.d2rq.db.op.OrderOp.OrderSpec;
import org.d2rq.db.op.ProjectionSpec;
import org.d2rq.db.renamer.Renamer;
import org.d2rq.db.types.DataType.GenericType;
import org.d2rq.db.vendor.Vendor;
import org.d2rq.nodes.NodeSetFilter;

/**
 * Resolves IRIs produced by a wrapped {@link ValueMaker} against a base IRI.
 * Any absolute IRIs produced by the wrapped value maker are left unchanged.
 *  
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class BaseIRIValueMaker implements ValueMaker {
	private final String baseIRI;
	private final ValueMaker wrapped;
	
	public BaseIRIValueMaker(String baseIRI, ValueMaker wrapped) {
		this.baseIRI = baseIRI;
		this.wrapped = wrapped;
	}

	public boolean matches(String value) {
		if (value.startsWith(baseIRI)) {
			return wrapped.matches(value.substring(baseIRI.length()));
		}
		return wrapped.matches(value);
	}

	public Expression valueExpression(String value, DatabaseOp tabular,
			Vendor vendor) {
		return URI.create(value).isAbsolute() 
				? wrapped.valueExpression(value, tabular, vendor) 
				: Concatenation.create(
						Constant.create(baseIRI, GenericType.CHARACTER), 
						wrapped.valueExpression(value, tabular, vendor));
	}

	public Set<ProjectionSpec> projectionSpecs() {
		return wrapped.projectionSpecs();
	}

	public String makeValue(ResultRow row) {
		String s = wrapped.makeValue(row);
		return URI.create(s).isAbsolute() ? s : baseIRI + s;
	}

	public void describeSelf(NodeSetFilter c) {
		//FIXME: This should take into account the fact that a base IRI might be prepended to the underlying value maker's value
		wrapped.describeSelf(c);
	}

	public ValueMaker rename(Renamer renamer) {
		return new BaseIRIValueMaker(baseIRI, wrapped.rename(renamer));
	}

	public List<OrderSpec> orderSpecs(boolean ascending) {
		return wrapped.orderSpecs(ascending);
	}
}
