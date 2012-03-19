package de.fuberlin.wiwiss.d2rq.sql;

import de.fuberlin.wiwiss.d2rq.map.Database;

/**
 * This syntax class implements SQL syntax for MS SQL Server
 * and MS Access.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class MSSQLSyntax extends SQL92Syntax {

	public MSSQLSyntax() {
		super(true);
	}
	
	public String getRowNumLimitAsSelectModifier(int limit) {
		if (limit == Database.NO_LIMIT) return "";
		return "TOP " + limit;
	}

	public String getRowNumLimitAsQueryAppendage(int limit) {
		return "";
	}
}
