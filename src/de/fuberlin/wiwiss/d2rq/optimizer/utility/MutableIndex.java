package de.fuberlin.wiwiss.d2rq.optimizer.utility;

/**
 * Represents a mutable-Integer.
 * It is used to generate a running number over all generated
 * SQL-Statements. 
 * It is used for the aliases of the tablenames. This means that
 * all table-aliases in the generated sql-statements for one 
 * sparql-query are unique. 
 * 
 * 
 * @author Herwig Leimer
 *
 */
public class MutableIndex
{
	private int value;
	private boolean useIndex = true;
	
	/**
	 * Constructor
	 */
	public MutableIndex()
	{
		this.value = 1;
	}

	/**
	 * Constructor
	 */
	public MutableIndex(boolean useIndex)
	{
		this.value = 1;
		this.useIndex = useIndex;
	}
	
	/**
	 * Returns the current value of the index
	 * @return int - current index-value
	 */	 
	public int getValue()
	{
		return this.value;
	}
	
	/**
	 * Increases the value at 1
	 */
	public void incValue()
	{
		this.value++;
	}
	
	public boolean isUseIndex() 
	{
		return useIndex;
	}

	public void setUseIndex(boolean useIndex) 
	{
		this.useIndex = useIndex;
	}
	
}
