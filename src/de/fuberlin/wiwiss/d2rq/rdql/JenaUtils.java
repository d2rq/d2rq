package de.fuberlin.wiwiss.d2rq.rdql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.query.Expression;
import com.hp.hpl.jena.graph.query.ExpressionSet;

/**
 * @author jgarbers
 * @version $Id: JenaUtils.java,v 1.2 2006/09/02 20:59:00 cyganiak Exp $
 */
public class JenaUtils {

    static protected void addVariableNames(ExpressionSet es, Set set, List listOfSets) {
      	  Iterator it=es.iterator();
      	  while (it.hasNext()) {
          	  Expression e=(Expression)it.next();
          	  Set s;
          	  if (listOfSets!=null) {
          		  s=new HashSet();
          	  } else {
          		  s=set;
          	  }
          	  Expression.Util.addVariablesOf( s, e );
          	  if (set!=null && s!=set) {
          		  set.addAll(s);
          	  }
      	  }
      }
      static protected void addVariableNodes(ExpressionSet s, Collection nodes, List listOfNodes) {
      	  Set names=null;
      	  if (nodes!=null)
      		  names=new HashSet();
      	  List namesList=null;
      	  if (listOfNodes!=null) {
      		namesList=new ArrayList();
      	  }
      	  addVariableNames(s,names,namesList);
      	  if (nodes!=null)
      		  namesToNodes(names,nodes);
      	  if (listOfNodes!=null) {
      		  Iterator it=namesList.iterator();
      		  while (it.hasNext()) {
      			  Collection in=(Collection)it.next();
      			  Collection out=new HashSet();
      			 namesToNodes(in,out);
      			 listOfNodes.add(out);
      		  }
      	  }
      }
      static void namesToNodes(Collection names,Collection nodes) {
      	  Iterator it=names.iterator();
      	  while (it.hasNext()) {
      		  String name=(String)it.next();
      		  Node node=Node.createVariable( name );
      		nodes.add(node);
      	  }    	  
      }
}
