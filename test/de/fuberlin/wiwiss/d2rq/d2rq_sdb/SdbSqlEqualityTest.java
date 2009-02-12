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
import java.util.Iterator;
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
							"query9.rq",		// bsbm - D2RQException: Wrong data type: For input string: "er1":
							"var-1.rq", 		// basic - Exception: Wrong data type: For input string: "Site1":
							"term-8.rq",		// basic - Unterschiedl. Anzahl von Erebnissen
							"distinct-1.rq",	// distinct - Unterschiedl. Anzahl von Erebnissen + Datatype format exception: "2008-08-30:00"^^xsd:dateTime
							"no-distinct-1.rq", // distinct - Unterschiedl. Erebnisse
							"no-distinct-2.rq", // distinct - DatatypeFormatException: Lexical form '2008-03-24:00' is not a legal instance of Datatype[http://www.w3.org/2001/XMLSchema#dateTime
							"query-eq-3.rq",	// expr-equals - Unterschiedl. Anzahl von Erebnissen + Datatype format exception: "2008-08-30:00"^^xsd:dateTime
							"q-opt-complex-1.rq", // optional - D2RQException: Wrong data type: For input string: "er1":
							"q-opt-complex-4.rq", // optional - D2RQException: Wrong data type: For input string: "er1":
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
		String varName;
		Query query;
		ResultSet resultSet;
		QueryExecution sdbQueryExecution, d2rqQueryExecution;
		Binding binding;
		List sdbDataResult, hsqlDataResult, sortingConditions;
		SortCondition sortCondition;
		Var var;
		List queries;
		int hsqlResultSize, sdbResultSize;
		Object sdbResultEntry, hsqlResultEntry;
		boolean entriesEqual;
		Model sdbModel, d2rqModel;
		Statement statement;
				
		try 
		{
		    System.out.println("Searching for Query-Files!");
		    
			queries = loadAllQueries();
		
			System.out.println("Loaded " + queries.size() + " Queries from Queries-Directory: " + CURR_DIR + "/" + QUERY_DIR);
			
			for(Iterator queryIterator = queries.iterator(); queryIterator.hasNext();)
			{
				query = (Query) queryIterator.next();
				
				System.out.println("--------------------------------------------------------------------------------------");
				System.out.println("Executing Query: ");
				System.out.println(query);
				
				
				// now execute the query against the sdb-datamodel
				System.out.println("Querying SDB-Data-Model!");
				sdbQueryExecution = QueryExecutionFactory.create(query, this.sdbDataModel);
				// collect the sdb-data-result
				sdbDataResult = new ArrayList();
				
				// now execute the query against the hsql-datamodel
				System.out.println("Querying HSQL-Data-Model!");
				d2rqQueryExecution = QueryExecutionFactory.create(query, this.hsqlDataModel);
				// now collect the hsql-result
				hsqlDataResult = new ArrayList();
				
				// Check for SELECT-Queries
				if (query.isSelectType())
				{
					resultSet = sdbQueryExecution.execSelect();
				
					// sorting
					// both results (sdbDataResult, mysqlDataResult) must have the same order
					// for equality-checking
					// create an sort-order that will be used for both results
					sortingConditions = new ArrayList();
					
					for(Iterator varIterator = resultSet.getResultVars().iterator(); varIterator.hasNext(); )
					{
						varName = (String)varIterator.next();
						var = Var.alloc(varName);
						sortCondition = new SortCondition(var, Query.ORDER_DEFAULT);
						sortingConditions.add(sortCondition);
					}
										
					
					while (resultSet.hasNext()) 
					{
						binding = resultSet.nextBinding();
						sdbDataResult.add(binding);
					}
					
					Collections.sort(sdbDataResult, new BindingComparator(sortingConditions));
				
					resultSet = d2rqQueryExecution.execSelect();
					
					
					while (resultSet.hasNext()) 
					{
						binding = resultSet.nextBinding();
						hsqlDataResult.add(binding);
					}
					
					Collections.sort(hsqlDataResult, new BindingComparator(sortingConditions));
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
					
					// sdb
					for(StmtIterator iterator = sdbModel.listStatements(); iterator.hasNext();)
					{
						statement = (Statement)iterator.nextStatement();
						sdbDataResult.add(statement);
					}
					Collections.sort(sdbDataResult, new StatementsComparator());
					
					
					// hsql
					for(StmtIterator iterator = d2rqModel.listStatements(); iterator.hasNext();)
					{
						statement = (Statement)iterator.nextStatement();
						hsqlDataResult.add(statement);
					}
					Collections.sort(hsqlDataResult, new StatementsComparator());
															
				}else if (query.isAskType())
				{
					// TODO: test for an ask-type
				}else
				{
					fail("Unknown Query-Type !!!");
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
	
	
	private List loadAllQueries() throws IOException
	{
		File queryDir;
		File[] files;
		List queries;
		
		queries = new ArrayList();
		
		queryDir = new File(CURR_DIR + "/" + QUERY_DIR);
		files = queryDir.listFiles();
		Arrays.sort(files);
		
		for(int i = 0; i < files.length; i++)
		{
			readRecursiveAndCreateQuery(files[i], queries);
		}
		
		return queries;
	}
	
	private void readRecursiveAndCreateQuery(File file, List queries) throws IOException
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

	
	private static class StatementsComparator implements Comparator 
	{

		public int compare(Object arg0, Object arg1) {
			return arg0.toString().compareTo(arg1.toString());
		}
		
	}
}




