package de.fuberlin.wiwiss.d2rq.rdql;

/**
 * Implementation for ExpressionTranslator Result.
 *  
 * @author jgarbers
 * @version $Id: SQLExpr.java,v 1.4 2006/09/03 00:08:11 cyganiak Exp $
 */
    public class SQLExpr implements ExpressionTranslator.Result {
        int type;
        String str;
        StringBuffer strBuf;
        public SQLExpr(String expr, int type) {
            this.type=type;
            str=expr;
        }
        public SQLExpr(StringBuffer expr, int type) {
            this.type=type;
            strBuf=expr;
        }
        public int getType() {
            return type;
        }
        public void setType(int type) {
            this.type=type;
        }
        public String getString() {
            if (str==null) {
                str=strBuf.toString();
            }
            return str;
        }
        public StringBuffer getStringBuffer() {
            if (strBuf==null) {
                strBuf=new StringBuffer(str);
            }
            return strBuf;
        }
        public void appendTo(StringBuffer sb) {
            if (str!=null)
                sb.append(str);
            else
                sb.append(strBuf);
        }
    }
    /*
    interface Result {
        public int getType();
        public String getString();
        public StringBuffer getStringBuffer();
        public void appendTo(StringBuffer sb);
    }

    class ResultBase implements Result {
        public int type;        
        public int getType() {
            return type;
        }
        public String getString() {
            return getStringBuffer().toString();
        }
        public StringBuffer getStringBuffer() {
            return new StringBuffer(getString());
        }
    }
    public class StringResult extends ResultBase {
        String expr;
        public StringResult(String expr, int type) {
            this.type=type;
            this.expr=expr;
        }
        public String getString() {
            return expr;
        }
    }
    public class StringBufferResult extends ResultBase {
        StringBuffer expr;
        public StringBufferResult(StringBuffer expr, int type) {
            this.type=type;
            this.expr=expr;
        }
        public StringBuffer getStringBuffer() {
            return expr;
        }
    }
    */
