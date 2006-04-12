/*
(c) Copyright 2005 by Joerg Garbers (jgarbers@zedat.fu-berlin.de)
*/
package de.fuberlin.wiwiss.d2rq.helpers;

/**
 * @author jgarbers
 * Uncomment the code that does not fit!
 */

// Jena 2.2
import com.hp.hpl.jena.rdql.ResultBinding;
import com.hp.hpl.jena.rdql.ResultBindingIterator;

public class JenaCompatibility {

	public static String resultBindingIteratorVarName(ResultBindingIterator it) {
	  return it.varName();
	}
	public static Object resultBindingIteratorValue(ResultBindingIterator it, ResultBinding b) {
	  return b.get(it.varName());
	}
}

// Jena 2.1
/*
import com.hp.hpl.jena.rdql.ResultBinding;
import com.hp.hpl.jena.rdql.ResultBinding.ResultBindingIterator;

public class JenaCompatibility {
	public static String resultBindingIteratorVarName(ResultBindingIterator it) {
	  return it.varName();
    }
	public static Object resultBindingIteratorValue(ResultBindingIterator it, ResultBinding b) {
	  return it.value());
	}
}
*/