package de.fuberlin.wiwiss.d2rq.optimizer.utility;

import java.util.Hashtable;

/**
 * @deprecated
 */
public class SPARQLToSQLOperatorsTable
{
    /** Hashtable with the keywords, used for efficient search. */
    private static Hashtable keywords;
    
    static
    {
        initKeywords();
    }
    
    /**
     * Delivers to a specific sparql-operator the equivalent sql-operator
     * if it does exist.
     * @param sparqlOperator - sparql-operator
     * @return String - either the equivalent sql-operator or an empty string if it does
     *                  not exist
     */
    public static String getSQLKeyword(String sparqlOperator)
    {
        return (String)keywords.get(sparqlOperator);
    }
    
    
    /**
     * Inits the Hashtable with all keywords of microjava
     *
     */
    private static void initKeywords()
    {
        keywords = new Hashtable();
        // TODO: different sql-operators for different databases !!!
        
        // add all Sparql-keywords and the corresponding sql-keyword
        // value null means no equivalent sql-keyword does exist
        // xquery unary operators
        keywords.put("!", "is not");
        keywords.put("+", "+");
        keywords.put("-", "-");
        
        // sparql tests
        keywords.put("bound", "");
        keywords.put("isIRI", "");
        keywords.put("isURI", "");
        keywords.put("isBlank", "");
        keywords.put("isLiteral", "");
        keywords.put("sameTerm", "");
        keywords.put("langMatches", "");
        keywords.put("regex", "");
        
        // sparql accessors
        keywords.put("str", "");
        keywords.put("lang", "");
        keywords.put("datatype", "");
        
        // logical connectives
        keywords.put("||", "OR");
        keywords.put("&&", "AND");

        // xpath tests
        keywords.put("=", "=");
        keywords.put("!=", "!=");
        keywords.put("<", "<");
        keywords.put(">", ">");
        keywords.put("<=", "<=");
        keywords.put(">=", ">=");
        
        // xpath arithmetic
        keywords.put("*", "*");
        keywords.put("/", "/");
        
        // not defined in sparql spec, but does exist in ARQ
        keywords.put("exists", "");
        
    }
}
