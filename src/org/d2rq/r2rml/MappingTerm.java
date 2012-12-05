package org.d2rq.r2rml;

import org.d2rq.db.SQLConnection;

public abstract class MappingTerm {

	public abstract void accept(MappingVisitor visitor);
	
	/**
	 * Determines validity. For components that require a SQL connection,
	 * this is a best-effort attempt that may not catch certain kinds of errors.
	 */
	public boolean isValid() {
		return isValid(null);
	}
	
	/**
	 * Determines validity.
	 */
	public boolean isValid(SQLConnection connection) {
		MappingValidator validator = new MappingValidator(null, connection);
		accept(validator);
		return !validator.getReport().hasError();
	}
}
