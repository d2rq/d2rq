package de.fuberlin.wiwiss.d2rq.optimizer.expr;

import org.apache.xerces.impl.dv.XSSimpleType;
import org.apache.xerces.xs.XSConstants;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.Node;

public class XSD {

	private static final XSSimpleType integerType = (XSSimpleType) XSDDatatype.XSDinteger.extendedTypeDefinition();
	private static final XSSimpleType decimalType = (XSSimpleType) XSDDatatype.XSDdecimal.extendedTypeDefinition();
	private static final XSSimpleType floatType   = (XSSimpleType) XSDDatatype.XSDfloat.extendedTypeDefinition();
	private static final XSSimpleType doubleType  = (XSSimpleType) XSDDatatype.XSDdouble.extendedTypeDefinition();
	
	public static RDFDatatype getNumericType(RDFDatatype lhs, RDFDatatype rhs)
	{
		int lhsType = getNumericType(lhs);
		int rhsType = getNumericType(rhs);
		
		if (lhsType == XSConstants.INTEGER_DT) {
			if (rhsType == XSConstants.INTEGER_DT)
				return XSDDatatype.XSDinteger;
			if (rhsType == XSConstants.DECIMAL_DT)
				return XSDDatatype.XSDdecimal;
			if (rhsType == XSConstants.FLOAT_DT)
				return XSDDatatype.XSDfloat;
			
			return XSDDatatype.XSDdouble;
		} else if (lhsType == XSConstants.DECIMAL_DT) {
			if (rhsType == XSConstants.INTEGER_DT || rhsType == XSConstants.DECIMAL_DT)
				return XSDDatatype.XSDdecimal;
			if (rhsType == XSConstants.FLOAT_DT)
				return XSDDatatype.XSDfloat;
			
			return XSDDatatype.XSDdouble;
		} else if (lhsType == XSConstants.FLOAT_DT) {
			if (rhsType == XSConstants.INTEGER_DT || rhsType == XSConstants.DECIMAL_DT || rhsType == XSConstants.FLOAT_DT)
				return XSDDatatype.XSDfloat;
			
			return XSDDatatype.XSDdouble;
		} else if (lhsType == XSConstants.DOUBLE_DT) {
			return XSDDatatype.XSDdouble;
		}
		
		throw new IllegalArgumentException();
	}
	
	private static int getNumericType(RDFDatatype numeric)
	{
		XSSimpleType type = (XSSimpleType) numeric.extendedTypeDefinition();
				
		if (type.derivedFromType(integerType, XSConstants.DERIVATION_EXTENSION))
			return integerType.getBuiltInKind();
		if (type.derivedFromType(decimalType, XSConstants.DERIVATION_EXTENSION))
			return decimalType.getBuiltInKind();
		if (type.derivedFromType(floatType, XSConstants.DERIVATION_EXTENSION))
			return floatType.getBuiltInKind();
		if (type.derivedFromType(doubleType, XSConstants.DERIVATION_EXTENSION))
			return doubleType.getBuiltInKind();
		
		throw new IllegalArgumentException();
	}
	
	public static boolean isNumeric(Node node)
	{
		if (!node.isLiteral())
			return false;
		
		RDFDatatype datatype = node.getLiteral().getDatatype();
		return datatype != null && isNumeric(datatype);
	}
	
	public static boolean isNumeric(RDFDatatype datatype)
	{
		XSSimpleType type = (XSSimpleType) datatype.extendedTypeDefinition();
		
		return type != null && type.getNumeric();
	}
	

	public static Node cast(Node numeric, RDFDatatype datatype)
	{
		return  Node.createLiteral(numeric.getLiteralLexicalForm(), null, datatype);
	}
	
	public static boolean isSupported(RDFDatatype datatype)
	{
		return datatype == null || isNumeric(datatype) || datatype.equals(XSDDatatype.XSDdateTime) || datatype.equals(XSDDatatype.XSDstring); 
	}
	
	public static boolean isString(Node node)
	{
		if (!node.isLiteral())
			return false;
		
		return XSDDatatype.XSDstring.equals(node.getLiteralDatatype());
	}
	
}
