package de.fuberlin.wiwiss.d2rq.helpers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.hp.hpl.jena.graph.Triple;

import de.fuberlin.wiwiss.d2rq.find.QueryContext;
import de.fuberlin.wiwiss.d2rq.map.Database;
import de.fuberlin.wiwiss.d2rq.map.PropertyBridge;

public class D2RQUtil {

	/**
	 * Finds all property bridges from a collection that match a triple.
	 * @param t the triple
	 * @param pbIt the collection iterator (elements must be of type PropertyBridge)
	 * @return list of items from pbIt
	 */
	public static ArrayList propertyBridgesForTriple(Triple t, Collection propertyListCandidates) { // PropertyBridge[]
		QueryContext context = new QueryContext();
		ArrayList list=new ArrayList(2);
		Iterator pbIt=propertyListCandidates.iterator();
		while (pbIt.hasNext()) {
			PropertyBridge bridge = (PropertyBridge) pbIt.next();
			if (!bridge.couldFit(t, context)) {
				continue;
			}
			list.add(bridge);
		}
		return list;
	}

	public static Map makeDatabaseMapFromPropertyBridges(List propertyBridges) {
		Map ret=new HashMap();
		Iterator it=propertyBridges.iterator();
		while (it.hasNext()) {
			PropertyBridge pb=(PropertyBridge)it.next();
			Database db=pb.getDatabase();
			List list=(List) ret.get(db);
			if (list==null) {
				list=new ArrayList();
				ret.put(db,list);
			}
			list.add(pb);
		}
		return ret;
	}

}
