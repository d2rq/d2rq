package de.fuberlin.wiwiss.d2rq.sql;

/**
 * A class for capturing performance information.
 * We grant read/write access to instance variables.
 * 
 * TODO: Remove?
 * 
 * @author jgarbers
 */
public class BeanCounter implements Cloneable {
    
    // Static global state information to be copied into an instance with update(). 
    public static int totalNumberOfExecutedSQLQueries = 0; 
    public static int totalNumberOfReturnedRows = 0; 
    public static int totalNumberOfReturnedFields = 0; 
       
    // instance fields corresponding to static fields
    public int numberOfExecutedSQLQueries = 0; 
    public int numberOfReturnedRows = 0; 
    public int numberOfReturnedFields = 0; 
    // time field corresponding to System.currentTimeMillis()
    public long timeMillis;
   
    // Operations on instances
    
    /**
     * Updates the instance fields with current static values.
     */
    public void update() {
    	   numberOfExecutedSQLQueries=totalNumberOfExecutedSQLQueries;
    	   numberOfReturnedRows=totalNumberOfReturnedRows;
    	   numberOfReturnedFields=totalNumberOfReturnedFields;
    	   timeMillis=System.currentTimeMillis();
    }
    /**
     * <code>This = This - minus </code>.
     * The difference between this and a <code>start</code> value.
     */
    public void subtract(BeanCounter minus) {
    		numberOfExecutedSQLQueries-=minus.numberOfExecutedSQLQueries;
    		numberOfReturnedRows-=minus.numberOfReturnedRows;
    		numberOfReturnedFields-=minus.numberOfReturnedFields;
    		timeMillis-=minus.timeMillis;
    }
    /**
     * <code>This = This / n </code>. Good for averaging over <code>n</code> runs.
     */   
    public void div(int n) {
		numberOfExecutedSQLQueries/=n;
		numberOfReturnedRows/=n;
		numberOfReturnedFields/=n;
		timeMillis/=n; 
    }
    
    // Creating Instances (convenience methods)
    
    public BeanCounter() {
    }
    public Object clone() {
    	  try {	
    	  	return super.clone();
    	  } catch (CloneNotSupportedException e) {
		// there should be no problems in cloneing just bare fields...
		throw new RuntimeException(e);
    	  }
    }
    /** 
     * Get a copy of the static fields.
     * @return an instance
     */
    public static BeanCounter instance() {
  	   BeanCounter inst=new BeanCounter();
  	   inst.update();
  	   return inst;
    }
    /**
     * Get a difference instance.
     * @return a new (static - minus) instance
     */ 
    public static BeanCounter instanceMinus(BeanCounter minus) {
   	   BeanCounter inst=instance();
  	   inst.subtract(minus);
  	   return inst;    	
    }
    /**
     * Get a difference instance.
     * @return a new (this - minus) instance
     */ 
    public BeanCounter minus(BeanCounter minus) {
  	   BeanCounter clone=(BeanCounter) clone();
  	   clone.subtract(minus);
  	   return clone;
     }
    
    // String methods
    /** 
     * Convenience method for presenting information.
     */
    public String sqlInfoString() {
    		return "" + numberOfExecutedSQLQueries + "/" + 
			numberOfReturnedRows + "/" + numberOfReturnedFields;
    }
    
    /** 
     * Convenience method for presenting information.
     */
    public String sqlPerformanceString() {
    	   return "" + timeMillis + "ms(" + sqlInfoString() + ")";
    }
 }