// a class for presenting inner information to the outside world

package de.fuberlin.wiwiss.d2rq;

public class InfoD2RQ implements Cloneable {
    public static int totalNumberOfExecutedSQLQueries = 0; 
    public static int totalNumberOfReturnedRows = 0; 
    public static int totalNumberOfReturnedFields = 0; 
       
    public int numberOfExecutedSQLQueries = 0; 
    public int numberOfReturnedRows = 0; 
    public int numberOfReturnedFields = 0; 
    
    public long timeMillis;
   
    // Operations on instances
    
    public void update() {
    	   numberOfExecutedSQLQueries=totalNumberOfExecutedSQLQueries;
    	   numberOfReturnedRows=totalNumberOfReturnedRows;
    	   numberOfReturnedFields=totalNumberOfReturnedFields;
    	   timeMillis=System.currentTimeMillis();
    }
    public void subtract(InfoD2RQ minus) {
    		numberOfExecutedSQLQueries-=minus.numberOfExecutedSQLQueries;
    		numberOfReturnedRows-=minus.numberOfReturnedRows;
    		numberOfReturnedFields-=minus.numberOfReturnedFields;
    		timeMillis-=minus.timeMillis;
    }
    public void div(int n) {
		numberOfExecutedSQLQueries/=n;
		numberOfReturnedRows/=n;
		numberOfReturnedFields/=n;
		timeMillis/=n; 
    }
    
    // Creating Instances (convenience methods)
    
    public InfoD2RQ() {
    }
    public Object clone() {
    	  try {	
    	  	return super.clone();
    	  } catch (CloneNotSupportedException e) {
		// there should be no problems in cloneing just bare fields...
		throw new RuntimeException(e);
    	  }
    }
    public static InfoD2RQ instance() {
  	   InfoD2RQ inst=new InfoD2RQ();
  	   inst.update();
  	   return inst;
    }
    // return a new (static - minus) instance
    public static InfoD2RQ instanceMinus(InfoD2RQ minus) {
   	   InfoD2RQ inst=instance();
  	   inst.subtract(minus);
  	   return inst;    	
    }
    // return a new (this - minus) instance
    public InfoD2RQ minus(InfoD2RQ minus) {
  	   InfoD2RQ clone=(InfoD2RQ) clone();
  	   clone.subtract(minus);
  	   return clone;
     }
    
    // String methods
    
    public String sqlInfoString() {
    		return "" + numberOfExecutedSQLQueries + "/" + 
			numberOfReturnedRows + "/" + numberOfReturnedFields;
    }
    
    public String sqlPerformanceString() {
    	   return "" + timeMillis + "ms(" + sqlInfoString() + ")";
    }
 }