package de.fuberlin.wiwiss.d2rq.sql;

import de.fuberlin.wiwiss.d2rq.map.Database;

/**
 * This syntax class implements SQL syntax for MS SQL Server
 * and MS Access.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: MSSQLSyntax.java,v 1.2 2010/11/03 18:48:17 cyganiak Exp $
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
