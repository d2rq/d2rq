package d2rq;

import java.io.*;

import com.hp.hpl.jena.rdf.model.Model;
import jena.cmdline.ArgDecl;
import jena.cmdline.CommandLine;
import de.fuberlin.wiwiss.d2rq.map.Database;
import de.fuberlin.wiwiss.d2rq.mapgen.MappingGenerator;

/**
 * Command line interface for {@link MappingGenerator}.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id: generate_mapping.java,v 1.5 2010/01/26 13:28:18 fatorange Exp $
 */
public class generate_mapping {
	private final static String[] includedDrivers = {
			"com.mysql.jdbc.Driver"
	};
	
	private final static String DEFAULT_BASE_URI = "http://localhost:2020/";
	
	public static void main(String[] args) {
		for (int i = 0; i < includedDrivers.length; i++) {
			Database.registerJDBCDriverIfPresent(includedDrivers[i]);
		}
		CommandLine cmd = new CommandLine();
		ArgDecl userArg = new ArgDecl(true, "u", "user", "username");
		ArgDecl passArg = new ArgDecl(true, "p", "pass", "password");
		ArgDecl schemaArg = new ArgDecl(true, "s", "schema");
		ArgDecl driverArg = new ArgDecl(true, "d", "driver");
        ArgDecl vocabModelFileArg = new ArgDecl(true, "v", "vocabfile");
		ArgDecl outfileArg = new ArgDecl(true, "o", "out", "outfile");
		ArgDecl baseUriArg = new ArgDecl(true, "b", "base", "baseuri");
		cmd.add(userArg);
		cmd.add(passArg);
		cmd.add(schemaArg);
		cmd.add(driverArg);
        cmd.add(vocabModelFileArg);
		cmd.add(outfileArg);
		cmd.add(baseUriArg);
		cmd.process(args);

		if (cmd.numItems() == 0) {
			usage();
			System.exit(1);
		}
		if (cmd.numItems() > 1) {
			System.err.println("too many arguments");
			usage();
			System.exit(1);
		}
		String jdbc = cmd.getItem(0);
		MappingGenerator gen = new MappingGenerator(jdbc);
		if (cmd.contains(userArg)) {
			gen.setDatabaseUser(cmd.getArg(userArg).getValue());
		}
		if (cmd.contains(passArg)) {
			gen.setDatabasePassword(cmd.getArg(passArg).getValue());
		}
		if (cmd.contains(schemaArg)) {
			gen.setDatabaseSchema(cmd.getArg(schemaArg).getValue());
		}
		if (cmd.contains(driverArg)) {
			gen.setJDBCDriverClass(cmd.getArg(driverArg).getValue());
		}
        File vocabModelOutfile = null;
        if(cmd.contains(vocabModelFileArg)) {
            vocabModelOutfile = new File(cmd.getArg(vocabModelFileArg).getValue());
        }
		File outputFile = null;
        String mapUriEnding;
		if (cmd.contains(outfileArg)) {
			outputFile = new File(cmd.getArg(outfileArg).getValue());
            mapUriEnding = outputFile.getName();
		} else {
            mapUriEnding = "stdout";
		}
        if(vocabModelOutfile != null && outputFile != null) {
            System.err.println("either -o or -v are permitted, but not both");
            usage();
            System.exit(1);
        }
		gen.setInstanceNamespaceURI("");
		
		String baseURI = cmd.contains(baseUriArg) ? cmd.getArg(baseUriArg).getValue()
												  : DEFAULT_BASE_URI;
		
		gen.setVocabNamespaceURI(baseURI + "vocab/resource/");
        gen.setMapNamespaceURI("d2r-mappings/" + mapUriEnding + "#");
		try {
            if(vocabModelOutfile != null) {
                Model model = gen.vocabularyModel(System.err);
                OutputStream vocabStream = new FileOutputStream(vocabModelOutfile);
                model.write(vocabStream, "N3");
            } else {
			    PrintStream out = (outputFile == null)
				    	? System.out
					    : new PrintStream(new FileOutputStream(outputFile));
			    gen.writeMapping(out, System.err);
            }
		} catch (IOException ex) {
			System.err.println(ex.getMessage());
			System.exit(1);
		}
	}
	
	private static void usage() {
        String usageBegin = "generate-mapping [-u username] [-p password] [-s database schema] [-d driverclass] ";
        String usageEnd = " [-b base uri] jdbcURL";
        String outfileUsage = "usage: " + usageBegin +  "[-o outfile.n3]" + usageEnd;
        String vocabUsage = "       " + usageBegin + "[-v vocabfile.n3]" + usageEnd;
		System.err.println(outfileUsage);
        System.err.println(vocabUsage);
	}
}
