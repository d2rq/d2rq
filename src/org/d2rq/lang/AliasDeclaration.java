package org.d2rq.lang;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.d2rq.db.renamer.Renamer;
import org.d2rq.db.renamer.TableRenamer;
import org.d2rq.db.schema.TableName;

/**
 * A "foo AS bar" declaration that establishes an alias for a named table,
 * as used with d2rq:alias.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class AliasDeclaration {

	public static Renamer getRenamer(Collection<AliasDeclaration> aliases) {
		Map<TableName,TableName> old2new = new HashMap<TableName,TableName>();
		for (AliasDeclaration alias: aliases) {
			old2new.put(alias.getOriginal(), alias.getAlias());
		}
		return TableRenamer.create(old2new);
	}
	
	private final TableName original;
	private final TableName alias;
	
	public AliasDeclaration(TableName original, TableName alias) {
		this.original = original;
		this.alias = alias;
	}

	public TableName getOriginal() {
		return original;
	}
	
	public TableName getAlias() {
		return alias;
	}
	
	@Override
	public String toString() {
		return Microsyntax.toString(original) + " AS " + alias.getTable().getName();
	}
	
	public int hashCode() {
		return original.hashCode() ^ alias.hashCode() ^ 974;
	}
	
	public boolean equals(Object o) {
		if (!(o instanceof AliasDeclaration)) return false;
		AliasDeclaration other = (AliasDeclaration) o;
		return other.original.equals(original) && other.alias.equals(alias);
	}
}
