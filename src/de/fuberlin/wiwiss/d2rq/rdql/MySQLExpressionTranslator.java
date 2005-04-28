/*
 * Created on 28.04.2005 by Joerg Garbers, FU-Berlin
 *
 */
package de.fuberlin.wiwiss.d2rq.rdql;

import de.fuberlin.wiwiss.d2rq.find.SQLStatementMaker;
import de.fuberlin.wiwiss.d2rq.rdql.ExpressionTranslator.Result;

/**
 * @author jgarbers
 *
 */
public class MySQLExpressionTranslator extends ExpressionTranslator {

    public MySQLExpressionTranslator(ConstraintHandler handler,
            SQLStatementMaker sql) {
        super(handler, sql);
        // TODO Auto-generated constructor stub
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
