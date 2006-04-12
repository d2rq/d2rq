/*
 * $Id: QueryCombiner.java,v 1.1 2006/04/12 09:53:04 garbers Exp $
 */
package de.fuberlin.wiwiss.d2rq.find;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;


/**
 * Container for {@link TripleQuery} instances. TripleQueries can
 * be added using the {@link #add} method. The QueryCombiner will
 * combine multiple queries into a single SQL statement, if
 * possible. After adding all queries, a {@link D2RQResultIterator}
 * can be obtained from the {@link #getResultIterator} method.
 *
 * <p>History:<br>
 * 08-03-2004: Initial version of this class.<br>
 * 
 * @author Richard Cyganiak <richard@cyganiak.de>
 * @version V0.2
 */
public class QueryCombiner {
	
    private List relationships;

    /**
     * @param relationships The relationships between triples in the query
     * that was issued.  This is generated in {@link CombinedPatternStage}.
     */
	public QueryCombiner( List relationships ){
        this.relationships = relationships;
	}
	
	private Collection compatibleQueries = new ArrayList(10);

	public void add(TripleQuery newQuery) {
		Iterator it = this.compatibleQueries.iterator();
		while (it.hasNext()) {
			List queries = (List) it.next();
			if (((TripleQuery) queries.get(0)).isCombinable(newQuery)) {
				queries.add(newQuery);
				return;
			}
		}
		List newList = new ArrayList(5);
		newList.add(newQuery);
		this.compatibleQueries.add(newList);
	}

	public D2RQResultIterator getResultIterator() {
		D2RQResultIterator result = new D2RQResultIterator();
		Iterator it = this.compatibleQueries.iterator();
		while (it.hasNext()) {
			List queryList = (List) it.next();
			result.addTripleResultSet(getTripleResultSet(queryList));
		}
		return result;
	}

	private TripleResultSet getTripleResultSet(List queries) {
		SQLStatementMaker sql = getSQL(queries);
		Iterator it = queries.iterator();
		TripleQuery first = (TripleQuery) it.next();
        List statements = sql.getSQLStatements();
        TripleResultSet result = null;
        for( int i =0;i < statements.size();i++ ){
    		result = new TripleResultSet((String)statements.get(i),
    				sql.getColumnNameNumberMap(i),
    				first.getDatabase());
    		result.addTripleMaker(first);
        }
		while (it.hasNext()) {
			TripleQuery query = (TripleQuery) it.next();
			result.addTripleMaker(query);
		}
		return result;
	}

	private SQLStatementMaker getSQL(List queries) {
		Iterator it = queries.iterator();
		TripleQuery first = (TripleQuery) it.next();
        TripleQuery[] qa = new TripleQuery[1];
        qa[0] = first;
		SQLStatementMaker result = new SQLStatementMaker(qa, relationships);
		result.addJoins(first.getJoins());
		result.addColumnValues(first.getColumnValues());
		result.addConditions(first.getConditions());
		result.addSelectColumns(first.getSelectColumns());
		result.addColumnRenames(first.getReplacedColumns());
		result.setEliminateDuplicates(first.mightContainDuplicates());
		while (it.hasNext()) {
			TripleQuery query = (TripleQuery) it.next();
			result.addSelectColumns(query.getSelectColumns());
			result.addColumnRenames(query.getReplacedColumns());
		}
		return result;
	}
}
