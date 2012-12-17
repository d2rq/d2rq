package org.d2rq.lang;

import java.util.ArrayList;
import java.util.List;

import org.d2rq.db.op.ProjectionSpec;
import org.d2rq.db.types.DataType.GenericType;
import org.d2rq.nodes.FixedNodeMaker;
import org.d2rq.nodes.NodeMaker;
import org.d2rq.nodes.TypedNodeMaker;
import org.d2rq.nodes.TypedNodeMaker.NodeType;
import org.d2rq.pp.PrettyPrinter;
import org.d2rq.values.BlankNodeIDValueMaker;
import org.d2rq.values.ColumnValueMaker;
import org.d2rq.values.DecoratingValueMaker;
import org.d2rq.values.DecoratingValueMaker.ValueConstraint;
import org.d2rq.values.ValueMaker;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;


/**
 * Creates {@link NodeMaker}s corresponding to {@link ResourceMap}s
 * in a D2RQ mapping.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class NodeMakerFactory {
	private final String baseURI;
	
	public NodeMakerFactory(String baseURI) {
		this.baseURI = baseURI;
	}

	public NodeMaker createFrom(final ResourceMap map) {
		if (map.getConstantValue() != null) {
			return new FixedNodeMaker(
					map.getConstantValue().asNode());
		}
		// Use a visitor to work out what kind of resource map this is
		ResourceMapRule rule = new D2RQMappingVisitor.Default() {
			private ResourceMapRule result;
			ResourceMapRule getResult() {
				map.accept(this);
				return result;
			}
			@Override
			public boolean visitEnter(ClassMap map) {
				result = new ResourceMapRule(map);
				return false;	// Don't recurse into property bridges
			}
			@Override
			public void visit(PropertyBridge map) {
				if (map.getRefersToClassMap() == null) {
					result = new PropertyBridgeRule(map);
				} else {
					result = new ReferencingPropertyBridgeRule(map);
				}
			}
			@Override
			public void visit(DownloadMap map) {
				result = new ResourceMapRule(map);
			}
		}.getResult();
		return new TypedNodeMaker(rule.getNodeType(), rule.createValueMaker());
	}

	private class ResourceMapRule {
		private ResourceMap map;
		ResourceMapRule(ResourceMap map) {
			this.map = map;
		}
		public NodeType getNodeType() {
			if (!map.getBNodeIdColumnsParsed().isEmpty()) {
				return TypedNodeMaker.BLANK;
			}
			if (map.getURIColumn() != null || map.getURIPattern() != null) {
				return TypedNodeMaker.URI;
			}
			if (map.getUriSQLExpression() != null) {
				return TypedNodeMaker.URI;
			}
			return null;
		}
		public ValueMaker createRawValueMaker() {
			if (!map.getBNodeIdColumnsParsed().isEmpty()) {
				return new BlankNodeIDValueMaker(PrettyPrinter.toString(map.resource()),
						map.getBNodeIdColumnsParsed());
			}
			if (map.getURIColumn() != null) {
				return new ColumnValueMaker(map.getURIColumn());
			}
			if (map.getURIPattern() != null) {
				return Microsyntax.parsePattern(ensureIsAbsolute(map.getURIPattern()));
			}
			if (map.getUriSQLExpression() != null) {
				return new ColumnValueMaker(
						ProjectionSpec.createColumnNameFor(
							Microsyntax.parseSQLExpression(
									map.getUriSQLExpression(), 
									GenericType.CHARACTER)));
			}
			return null;
		}
		public ValueMaker createValueMaker() {
			return decorate(createRawValueMaker());
		}
		private ValueMaker decorate(ValueMaker values) {
			List<ValueConstraint> constraints = new ArrayList<ValueConstraint>();
			if (map.getValueMaxLength() != Integer.MAX_VALUE) {
				constraints.add(DecoratingValueMaker.maxLengthConstraint(map.getValueMaxLength()));
			}
			for (String contains: map.getValueContainses()) {
				constraints.add(DecoratingValueMaker.containsConstraint(contains));
			}
			for (String regex: map.getValueRegexes()) {
				constraints.add(DecoratingValueMaker.regexConstraint(regex));
			}
			if (map.getTranslateWith() == null) {
				if (constraints.isEmpty()) {
					return values;
				}
				return new DecoratingValueMaker(values, constraints);
			}
			return new DecoratingValueMaker(values, constraints, map.getTranslateWith().translator());
		}
	}
	
	private class PropertyBridgeRule extends ResourceMapRule {
		private PropertyBridge map;
		PropertyBridgeRule(PropertyBridge bridge) {
			super(bridge);
			this.map = bridge;
		}
		public NodeType getNodeType() {
			if (map.getDatatype() != null) {
				RDFDatatype datatype = TypeMapper.getInstance().getSafeTypeByName(map.getDatatype());
				return TypedNodeMaker.typedLiteral(datatype);
			}
			if (map.getLang() != null) {
				return TypedNodeMaker.languageLiteral(map.getLang());
			}
			if (super.getNodeType() != null) {
				return super.getNodeType();
			}
			return TypedNodeMaker.PLAIN_LITERAL;
		}
		public ValueMaker createRawValueMaker() {
			if (map.getColumn() != null) {
				return new ColumnValueMaker(map.getColumn());
			} else if (map.getPattern() != null) {
				return Microsyntax.parsePattern(map.getPattern());
			} else if (map.getSQLExpression() != null) {
				return new ColumnValueMaker(
						ProjectionSpec.createColumnNameFor(
							Microsyntax.parseSQLExpression(
									map.getSQLExpression(), 
									GenericType.CHARACTER)));
			} else {
				return super.createRawValueMaker();
			}
		}
	}

	private class ReferencingPropertyBridgeRule extends PropertyBridgeRule {
		private final PropertyBridge map;
		private final ResourceMapRule forReferencedClassMap;
		ReferencingPropertyBridgeRule(PropertyBridge bridge) {
			super(bridge);
			map = bridge;
			forReferencedClassMap = new ResourceMapRule(map.getRefersToClassMap());
		}
		public NodeType getNodeType() {
			return forReferencedClassMap.getNodeType();
		}
		public ValueMaker createRawValueMaker() {
			ValueMaker values = forReferencedClassMap.createValueMaker();
			return values.rename(AliasDeclaration.getRenamer(map.getAliases()));
		}
	}
	
	private String ensureIsAbsolute(String uriPattern) {
		if (uriPattern.indexOf(":") == -1) {
			return this.baseURI + uriPattern;
		}
		return uriPattern;
	}
}
