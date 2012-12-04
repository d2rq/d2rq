package org.d2rq.r2rml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.hp.hpl.jena.rdf.model.Resource;

/**
 * A collection of {@link MappingComponent}s. Mapping components
 * are stored with a {@link Resource} that serves as an identifier.
 * 
 * @author RichardCyganiak
 *
 * @param <T> The type of mapping component
 */
public class ComponentCollection<T extends MappingComponent> {
	private final Map<Resource,T> components = new HashMap<Resource,T>();
	
	public void put(Resource resource, T component) {
		components.put(resource, component);
	}
	
	public boolean has(Resource resource) {
		return components.containsKey(resource);
	}
	
	public T get(Resource resource) {
		return components.get(resource);
	}
	
	public Collection<T> getAll(Set<Resource> resources) {
		Collection<T> result = new ArrayList<T>();
		for (Resource r: resources) {
			if (get(r) != null) result.add(get(r));
		}
		return result;
	}
	
	public T remove(Resource resource) {
		return components.remove(resource);
	}
	
	public Set<Resource> resources() {
		return components.keySet();
	}
	
	public Collection<T> components() {
		return components.values();
	}
	
	public Resource resourceFor(T component) {
		for (Entry<Resource,T> entry: components.entrySet()) {
			if (entry.getValue().equals(component)) return entry.getKey();
		}
		return null;
	}
	
	public boolean isEmpty() {
		return components.isEmpty();
	}
	
	public int size() {
		return components.size();
	}
}
