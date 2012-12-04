package org.d2rq.r2rml;

public abstract class MappingTerm {

	public abstract void accept(MappingVisitor visitor);
	
	public boolean isValid() {
		MappingValidator validator = new MappingValidator(null);
		accept(validator);
		return !validator.getReport().hasError();
	}
}
