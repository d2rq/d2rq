/*
  (c) Copyright 2004 by Chris Bizer (chris@bizer.de)
*/

package de.fuberlin.wiwiss.d2rq;

import java.util.HashMap;
import java.util.HashSet;
import com.hp.hpl.jena.graph.*;

/** Some utility methods used by various classes in the package.
 * 
 * <BR>History: 06-06-2004   : Initial version of this class.
 * @author Chris Bizer chris@bizer.de
 * @version V0.1
 */
public class D2RQUtil {
    protected static String getNamespacePrefix(String qName) {
        int len = qName.length();
        for (int i = 0; i < len; i++) {
            if (qName.charAt(i) == ':') {
                return qName.substring(0, i);
            }
        }
        return null;
    }

    protected static String getLocalName(String qName) {
        int len = qName.length();
        for (int i = 0; i < len; i++) {
            if (qName.charAt(i) == ':') {
                return qName.substring(i + 1);
            }
        }
        return null;
    }

    protected static String getQuotedColumnValue(String value, Node columnType) {
        String quote;
        if (columnType.equals(D2RQ.textColumn)) {
            quote = "'";
        } else if (columnType.equals(D2RQ.dateColumn)) {
            quote = "#";
        } else {
            quote = "";
        }
        return quote + value + quote;
    }

    /**
     * Extracts the database column name from a tablename.columnname combination.
     * @param fName tablename.columnname combination seperated by a '.'
     * @return database column name.
     */
    protected static String getColumnName(String fName) {
        int len = fName.length();
        for (int i = 0; i < len; i++) {
            if (fName.charAt(i) == '.') {
                return fName.substring(i + 1);
            }
        }
         throw new D2RQException("There is no . in the tablename.columnname combination " + fName);
    }

    /**
     * Extracts the database table name from a tablename.columnname combination.
     * @param fName tablename.columnname combination seperated by a '.'
     * @return database table name.
     */
    protected static String getTableName(String fName) {
        int len = fName.length();
        for (int i = 0; i < len; i++) {
            if (fName.charAt(i) == '.') {
                return fName.substring(0, i);
            }
        }
        throw new D2RQException("There is no . in the tablename.columnname combination " + fName);
    }

 /**
     * Parses an D2R pattern. Translates the placeholders in an D2R pattern with values from the database.
     * @param  pattern Pattern to be translated.
     * @return String with placeholders replaced.
     */
    protected static HashSet getColumnsfromPattern(String pattern) {
        HashSet result = new HashSet(5);
        int startPosition = 0;
        int endPosition = 0;
            if (pattern.indexOf(D2RQ.deliminator) == -1) return result;
            while (startPosition < pattern.length() && pattern.indexOf(D2RQ.deliminator, startPosition) != -1) {
                endPosition = startPosition;
                startPosition = pattern.indexOf(D2RQ.deliminator, startPosition);
                startPosition = startPosition + D2RQ.deliminator.length();
                endPosition = pattern.indexOf(D2RQ.deliminator, startPosition);
                // get field
                String columnname = pattern.substring(startPosition, endPosition).trim();
                result.add(columnname);
                startPosition = endPosition + D2RQ.deliminator.length();
            }
            return result;
    }

    /**
     * Checks if a given value could fit a D2RQ pattern.
     * @param  pattern Pattern against which the value is checked.
     * @param  value value to be checked.
     */
     public static boolean valueCanFitPattern(String value, String pattern) {
		 HashMap map = ReverseValueWithPattern(value, pattern);
         return !map.isEmpty();
     }

    /**
     * Reverses a given value with D2RQ pattern and returns a coumn name/column value map.
     * @param  pattern Pattern against which the value is checked.
     * @param  value value to be checked.
     */
     public static HashMap ReverseValueWithPattern(String value, String pattern) {
		    HashMap resultMap = new HashMap();
			int patternStartPosition = 0;
            int patternEndPosition;
            int patternVarStartPosition = 0;
            int patternVarEndPosition = 0;
            int patternNextStartPosition = 0;
            int seperatingTextStartPosition = 0;
            int valueEndPosition = 0;
            int valueVarStartPosition = 0;
            int valueVarEndPosition = 0;
            String varName;
            String varValue;
            String varSeperatingText;

			if (pattern.indexOf(D2RQ.deliminator) == -1) return resultMap;
			while (patternStartPosition < pattern.length()) {
				patternEndPosition = patternStartPosition;
                // Deliminator in pattern
				patternStartPosition = pattern.indexOf(D2RQ.deliminator, patternStartPosition);
                if (patternStartPosition != -1) {
                    // Pattern start deliminator found
                    if (patternStartPosition <= value.length()) {
						if (!pattern.substring(patternEndPosition, patternStartPosition).equals(value.substring(valueEndPosition, valueEndPosition + patternStartPosition - patternEndPosition))) {
							// Text in pattern doesn't fit text in value
							resultMap.clear();
							return resultMap;
						}
						valueVarStartPosition = valueEndPosition + patternStartPosition - patternEndPosition;
                    } else {
                       // Position in pattern is bigger than value lenth
							resultMap.clear();
							return resultMap;
                    }
                    patternVarStartPosition = patternStartPosition + D2RQ.deliminator.length();
                    // Find pattern end deliminator
                    patternVarEndPosition = pattern.indexOf(D2RQ.deliminator, patternVarStartPosition);
                    // Get Var Name from pattern
                    varName = pattern.substring(patternVarStartPosition, patternVarEndPosition);
                    // Find next pattern start position or end of pattern
                    patternNextStartPosition = pattern.indexOf(D2RQ.deliminator, patternVarEndPosition + D2RQ.deliminator.length());
                    // Get text that seperates vars or the rest of the pattern
                    seperatingTextStartPosition = patternVarEndPosition + D2RQ.deliminator.length();
                    if (seperatingTextStartPosition != pattern.length()) {
                        // There is more sperating text behind the variable
						if (patternNextStartPosition != -1) {
							varSeperatingText = pattern.substring(seperatingTextStartPosition, patternNextStartPosition);
						} else {
							varSeperatingText = pattern.substring(seperatingTextStartPosition);
						}
						// Find seperating text in value
					   valueVarEndPosition = value.indexOf(varSeperatingText, valueVarStartPosition);
					   if (valueVarEndPosition != -1) {
						  // get var value
						  varValue = value.substring(valueVarStartPosition, valueVarEndPosition);
					   } else {
						 // Seperating text not found in value =>  Text in pattern doesn't fit the value
						resultMap.clear();
						return resultMap;
					   }
                   } else {
                       // There is no more sperating text behind the variable
                       varValue = value.substring(valueVarStartPosition);
                       varSeperatingText = "";
                   }
                   // Add name/value pair to result
                   resultMap.put(varName, varValue);
                   patternStartPosition = seperatingTextStartPosition + varSeperatingText.length();
                   valueEndPosition = valueVarEndPosition + varSeperatingText.length();
                }
			}
            return resultMap;
     }
}