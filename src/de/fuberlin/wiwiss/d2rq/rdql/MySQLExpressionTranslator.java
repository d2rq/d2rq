package de.fuberlin.wiwiss.d2rq.rdql;

import de.fuberlin.wiwiss.d2rq.sql.SelectStatementBuilder;

/**
 * @author jgarbers
 * @version $Id: MySQLExpressionTranslator.java,v 1.3 2006/09/03 00:08:11 cyganiak Exp $
 */
public class MySQLExpressionTranslator extends ExpressionTranslator {

    public MySQLExpressionTranslator(ConstraintHandler handler,
            SelectStatementBuilder sql) {
        super(handler, sql);
        putOp(concatenateOp,"CONCAT",StringType).functional=true; // mySQL
    }
    
    public Result castToString(Result r) {
        if (r.getType()==StringType)
            return r;
        StringBuffer sb=new StringBuffer("CAST(");
        r.appendTo(sb);
        // sb.append(" AS SQL_TEXT)"); // SQL 92
        sb.append(" AS char)"); // mysql
        return newResult(sb,StringType);
    }
}
