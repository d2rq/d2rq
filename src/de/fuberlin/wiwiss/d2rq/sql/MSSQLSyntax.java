package de.fuberlin.wiwiss.d2rq.sql;

import de.fuberlin.wiwiss.d2rq.map.Database;

/**
 * This syntax class implements SQL syntax for MS SQL Server
 * and MS Access.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: MSSQLSyntax.java,v 1.1 2009/09/29 19:56:53 cyganiak Exp $
 */
public class MSSQLSyntax extends SQL92Syntax {

	public String getRowNumLimitAsSelectModifier(int limit) {
		if (limit == Database.NO_LIMIT) return "";
		return "TOP " + limit;
	}

	public String getRowNumLimitAsQueryAppendage(int limit) {
		return "";
	}
}
