package de.fuberlin.wiwiss.d2rq;

import java.io.IOException;
import java.sql.SQLException;

import jena.cmdline.ArgDecl;
import jena.cmdline.CommandLine;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.hp.hpl.jena.shared.JenaException;


/**
 * Base class for the D2RQ command line tools. They share much of their
 * argument list and functionality, therefore this is extracted into
 * this superclass.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 */
public abstract class CommandLineTool {
	private final static Log log = LogFactory.getLog(CommandLineTool.class);

	private final CommandLine cmd = new CommandLine();
	private final ArgDecl userArg = new ArgDecl(true, "u", "user", "username");
	private final ArgDecl passArg = new ArgDecl(true, "p", "pass", "password");
	private final ArgDecl schemaArg = new ArgDecl(true, "s", "schema");
	private final ArgDecl driverArg = new ArgDecl(true, "d", "driver");
	private final ArgDecl sqlFileArg = new ArgDecl(true, "l", "load-sql");
	private final ArgDecl verboseArg = new ArgDecl(false, "verbose");
	private final ArgDecl debugArg = new ArgDecl(false, "debug");
	private final SystemLoader loader = new SystemLoader();
	
	public abstract void usage();

	public abstract void initArgs(CommandLine cmd);
	
	public abstract void run(CommandLine cmd, SystemLoader loader)
	throws D2RQException, IOException;
	
	public void process(String[] args) {
		cmd.add(userArg);
		cmd.add(passArg);
		cmd.add(schemaArg);
		cmd.add(driverArg);
		cmd.add(sqlFileArg);
		cmd.add(verboseArg);
		cmd.add(debugArg);
		
		initArgs(cmd);
		
		try {
			cmd.process(args);
		} catch (IllegalArgumentException ex) {
			reportException(ex);
		}
		
		if (cmd.hasArg(verboseArg)) {
			// Adjust Log4j log level to show more stuff 
			Logger.getLogger("de.fuberlin.wiwiss.d2rq").setLevel(Level.INFO);
			Logger.getLogger("org.eclipse.jetty").setLevel(Level.INFO);
			Logger.getLogger("org.joseki").setLevel(Level.INFO);
		}
		if (cmd.hasArg(debugArg)) {
			// Adjust Log4j log level to show MUCH more stuff 
			Logger.getLogger("de.fuberlin.wiwiss.d2rq").setLevel(Level.ALL);
			Logger.getLogger("org.eclipse.jetty").setLevel(Level.INFO);
			Logger.getLogger("org.joseki").setLevel(Level.INFO);
		}
		
		if (cmd.numItems() == 0) {
			if (cmd.hasArg(sqlFileArg)) {
				loader.setJdbcURL(SystemLoader.DEFAULT_JDBC_URL);
			} else {
				usage();
				System.exit(1);
			}
		} else if (cmd.numItems() > 1) {
			reportException(new IllegalArgumentException("Too many arguments"));
		}
		
		if (cmd.contains(userArg)) {
			loader.setUsername(cmd.getArg(userArg).getValue());
		}
		if (cmd.contains(passArg)) {
			loader.setPassword(cmd.getArg(passArg).getValue());
		}
		if (cmd.contains(schemaArg)) {
			loader.setSchema(cmd.getArg(schemaArg).getValue());
		}
		if (cmd.contains(driverArg)) {
			loader.setJDBCDriverClass(cmd.getArg(driverArg).getValue());
		}
		if (cmd.contains(sqlFileArg)) {
			loader.setStartupSQLScript(cmd.getArg(sqlFileArg).getValue());
		}
		
		try {
			run(cmd, loader);
		} catch (IOException ex) {
			reportException(ex);
		} catch (D2RQException ex) {
			reportException(ex);
		} catch (JenaException ex) {
			reportException(ex);
		}
	}
	
	public static void reportException(D2RQException ex) {
		if (ex.getCause() != null && ex.getCause().getMessage() != null) {
			if (ex.getCause() instanceof SQLException) {
				System.err.println("SQL error " + ex.getCause().getMessage());
			} else {
				System.err.println(ex.getCause().getMessage());
			}
		} else {
			System.err.println(ex.getMessage());
		}
		log.debug("Command line tool exception", ex);
		System.exit(1);
	}
	
	public void reportException(Exception ex) {
		System.err.println(ex.getMessage());
		log.debug("Command line tool exception", ex);
		System.exit(1);
	}

	public void printStandardArguments(boolean withMappingFile) {
		System.err.println("  Arguments" + (withMappingFile ? " (choose one)" : "") + ":");
		if (withMappingFile) {
			System.err.println("    mappingFile     Filename or URL of a D2RQ mapping file");
		}
		System.err.println("    jdbcURL         JDBC URL for the DB, e.g. jdbc:mysql://localhost/dbname");
		System.err.println("                    (If omitted with -l, jdbcURL defaults to " + SystemLoader.DEFAULT_JDBC_URL + ")");
	}
	
	public void printConnectionOptions() {
		System.err.println("    -u username     Database user for connecting to the DB");
		System.err.println("    -p password     Database password for connecting to the DB");
		System.err.println("    -d driverclass  Java class name of the JDBC driver for the DB");
		System.err.println("    -s db_schema    Only map tables in a specific named DB schema");
		System.err.println("    -l script.sql   Load a SQL script before generating the mapping");
	}
}
