package de.fuberlin.wiwiss.d2rq.helpers;

import de.fuberlin.wiwiss.d2rq.D2RQException;
import de.fuberlin.wiwiss.d2rq.D2RQTestSuite;
import de.fuberlin.wiwiss.d2rq.map.Database;
import de.fuberlin.wiwiss.d2rq.map.Mapping;
import de.fuberlin.wiwiss.d2rq.parser.MapParser;
import de.fuberlin.wiwiss.d2rq.sql.DummyDB;

/**
 * Test helper for creating {@link Mapping}s.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public class MappingHelper {

	/**
	 * Parses a D2RQ mapping from a file located relative to
	 * the {@link D2RQTestSuite} directory.
	 * 
	 * @param testFileName Filename, relative to {@link D2RQTestSuite}'s location
	 * @return A mapping
	 * @throws D2RQException On error during parse
	 */
	public static Mapping readFromTestFile(String testFileName) {
		return new MapParser(
				D2RQTestSuite.loadTurtle(testFileName), 
				"http://example.org/").parse();
	}
	
	public static void connectToDummyDBs(Mapping m) {
		for (Database db: m.databases()) {
			connectToDummyDB(db);
		}
	}
	
	public static void connectToDummyDB(Database db) {
		db.useConnectedDB(new DummyDB());
	}
}
