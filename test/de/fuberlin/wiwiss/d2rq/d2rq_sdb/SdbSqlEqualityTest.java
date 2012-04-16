package de.fuberlin.wiwiss.d2rq.d2rq_sdb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.SortCondition;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.engine.binding.BindingComparator;

public class SdbSqlEqualityTest extends LoadDataTest 
{
	// query-directory
	private static final String QUERY_DIR = "queries";
	private static final String QUERY_FILE_SUFFIX = ".rq";
	private String[] excludedQueriesFileNames = {  
            // fehlgeschlagene Tests
							"var-1.rq", 		// basic - Exception: Wrong data type: For input string: "Site1":
							"term-8.rq",		// basic - Unterschiedl. Anzahl von Erebnissen
							"distinct-1.rq",	// distinct - Unterschiedl. Anzahl von Erebnissen + Datatype format exception: "2008-08-30:00"^^xsd:dateTime
							"no-distinct-1.rq", // distinct - Unterschiedl. Erebnisse
							"query-eq-3.rq",	// expr-equals - Unterschiedl. Anzahl von Erebnissen + Datatype format exception: "2008-08-30:00"^^xsd:dateTime
							"open-cmp-01.rq",	// open-world - noch anzupassen
							"open-cmp.02.rq",	// open-world - noch anzupassen
							"open-eq-06.rq", 	// open-world - Unterschiedl. Anzahl von Erebnissen
							
//                            "expr-builtin",
//                            "expr-ops",
//                            "open-world",
//                            "optional",
//                            "optional-filter"
	                                            };
	
	public SdbSqlEqualityTest()
	{
		super();
	}
		
	public void testSdbSqlEquality()
	{
		ResultSet resultSet;
		QueryExecution sdbQueryExecution, d2rqQueryExecution;
		List<? extends Object> sdbDataResult;		// Statement or Binding
		List<? extends Object> hsqlDataResult;	// Statement or Binding
		List<SortCondition> sortingConditions;
		SortCondition sortCondition;
		Var var;
		List<Query> queries;
		int hsqlResultSize, sdbResultSize;
		Object sdbResultEntry, hsqlResultEntry;
		boolean entriesEqual;
		Model sdbModel, d2rqModel;
				
		try 
		{
		    System.out.println("Searching for Query-Files!");
		    
			queries = loadAllQueries();
		
			System.out.println("Loaded " + queries.size() + " Queries from Queries-Directory: " + CURR_DIR + "/" + QUERY_DIR);
			
			for (Query query: queries) {
				
				System.out.println("--------------------------------------------------------------------------------------");
				System.out.println("Executing Query: ");
				System.out.println(query);
				
				
				// now execute the query against the sdb-datamodel
				System.out.println("Querying SDB-Data-Model!");
				sdbQueryExecution = QueryExecutionFactory.create(query, this.sdbDataModel);
				
				// now execute the query against the hsql-datamodel
				System.out.println("Querying HSQL-Data-Model!");
				d2rqQueryExecution = QueryExecutionFactory.create(query, this.hsqlDataModel);
				
				// Check for SELECT-Queries
				if (query.isSelectType())
				{
					resultSet = sdbQueryExecution.execSelect();
				
					// sorting
					// both results (sdbDataResult, mysqlDataResult) must have the same order
					// for equality-checking
					// create an sort-order that will be used for both results
					sortingConditions = new ArrayList<SortCondition>();
					
					for (String varName: resultSet.getResultVars()) {
						var = Var.alloc(varName);
						sortCondition = new SortCondition(var, Query.ORDER_DEFAULT);
						sortingConditions.add(sortCondition);
					}
										
					List<Binding> sdbSelectResult = new ArrayList<Binding>();
					List<Binding> hsqlSelectResult = new ArrayList<Binding>();
					
					while (resultSet.hasNext()) 
					{
						sdbSelectResult.add(resultSet.nextBinding());
					}
					
					Collections.sort(sdbSelectResult, new BindingComparator(sortingConditions));
				
					resultSet = d2rqQueryExecution.execSelect();
					
					
					while (resultSet.hasNext()) 
					{
						hsqlSelectResult.add(resultSet.nextBinding());
					}
					
					Collections.sort(hsqlSelectResult, new BindingComparator(sortingConditions));

					sdbDataResult = sdbSelectResult;
					hsqlDataResult = hsqlSelectResult;
				}else if (query.isConstructType() || query.isDescribeType())
				{
					
					if (query.isConstructType())
					{
						// sdb
						sdbModel = sdbQueryExecution.execConstruct();
						// hsql
						d2rqModel = d2rqQueryExecution.execConstruct();
					}else
					{
						// sdb
						sdbModel = sdbQueryExecution.execDescribe();
						// hsql
						d2rqModel = d2rqQueryExecution.execDescribe();
					}
					
					List<Statement> sdbGraphResult = new ArrayList<Statement>();
					List<Statement> hsqlGraphResult = new ArrayList<Statement>();

					// sdb
					for(StmtIterator iterator = sdbModel.listStatements(); iterator.hasNext();)
					{
						sdbGraphResult.add(iterator.nextStatement());
					}
					Collections.sort(sdbGraphResult, new StatementsComparator());
					
					
					// hsql
					for(StmtIterator iterator = d2rqModel.listStatements(); iterator.hasNext();)
					{
						hsqlGraphResult.add(iterator.nextStatement());
					}
					Collections.sort(hsqlGraphResult, new StatementsComparator());

					sdbDataResult = sdbGraphResult;
					hsqlDataResult = hsqlGraphResult;

				}else if (query.isAskType())
				{
					// TODO: test for an ask-type
					continue;
				}else
				{
					fail("Unknown Query-Type !!!");
					continue;
				}
					
					
				System.out.println("Now checking for Query-Result-Equality!");
				
				sdbResultSize = sdbDataResult.size();
				hsqlResultSize = hsqlDataResult.size();
				
				System.out.println("Query-SDB-Result-Size: " + sdbResultSize);
				System.out.println("Query-HSQL-Result-Size: " + hsqlResultSize);
				
				if (sdbResultSize == hsqlResultSize)
				{
				    System.out.println("SDB-Result-Size and HSQL-Result-Size are equal!");
				}else
				{
				    fail();
				}
				
				System.out.println("Now checking each Result-Entry for Equality!");
					
				for(int i = 0; i <sdbDataResult.size(); i++)
				{
				    sdbResultEntry = sdbDataResult.get(i);
				    hsqlResultEntry = hsqlDataResult.get(i);
				    entriesEqual = sdbResultEntry.equals(hsqlResultEntry); 
				    System.out.println("SDB-Result-Entry: " + sdbResultEntry);
					System.out.println("HSQL-Result-Entry: " + hsqlResultEntry);
					if (entriesEqual)
					{
					    System.out.println("Result-Entries are Equal: " + entriesEqual);
					}else
					{
					    fail();
					}
				}
					
				System.out.println("SDB and SQL-Results are equal!");
				
			}
			
			System.out.println(queries.size() + " Queries checked !");
		} catch (IOException e) 
		{
			e.printStackTrace();
			fail();
		}
		
		
	}
	
	
	private List<Query> loadAllQueries() throws IOException
	{
		File queryDir;
		File[] files;
		List<Query> queries;
		
		queries = new ArrayList<Query>();
		
		queryDir = new File(CURR_DIR + "/" + QUERY_DIR);
		files = queryDir.listFiles();
		Arrays.sort(files);
		
		for(int i = 0; i < files.length; i++)
		{
			readRecursiveAndCreateQuery(files[i], queries);
		}
		
		return queries;
	}
	
	private void readRecursiveAndCreateQuery(File file, List<Query> queries) throws IOException
	{
		File[] files;
		Query query;
		BufferedReader queryReader = null;
		String fileName;
		
		fileName = file.getName();
		
		if (file.isDirectory())
		{
			
			files = file.listFiles();
			System.out.println("Reading Directory: " + fileName + " - contains " + files.length + " Files!");
			
			Arrays.sort(files);
			
			for(int i = 0; i < files.length; i++)
			{
				if (!excludeFile(files[i]))
				{
					// step down
					readRecursiveAndCreateQuery(files[i], queries);
				}
			}			
		}else
		{
			System.out.println("Reading File: " + fileName);
			
			// no directory
			
			try
			{
				
				if (!excludeFile(file))
				{
					queryReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
					query = createQuery(queryReader);
					queries.add(query);
				}
			}finally
			{
				if (queryReader != null)
				{
					queryReader.close();
				}
			}
			
		}
		
		
	}
	
	
	private boolean excludeFile(File file)
	{
		String fileName;
		boolean exclude = false;
		
		fileName = file.getName();
		// no directory
		for(int j = 0; j < excludedQueriesFileNames.length; j++)
		{
			
		    if (fileName.equals(excludedQueriesFileNames[j]) || (file.isFile() && !fileName.toLowerCase().endsWith(QUERY_FILE_SUFFIX)))
		    {
		        exclude = true;
		        break;
		    }
		}
		
		return exclude;
	}
	
	private Query createQuery(BufferedReader queryReader) throws IOException
	{
		StringBuffer stringBuffer;
		String line;
		
		stringBuffer = new StringBuffer();
		
		while((line = queryReader.readLine()) != null)
		{
			stringBuffer.append(line);
			stringBuffer.append("\n");
		}
		
		return QueryFactory.create(stringBuffer.toString());
	}

	
	private static class StatementsComparator implements Comparator<Statement> 
	{

		public int compare(Statement arg0, Statement arg1) {
			return arg0.toString().compareTo(arg1.toString());
		}
		
	}
}




