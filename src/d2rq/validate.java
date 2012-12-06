package d2rq;

import java.io.IOException;

import jena.cmdline.ArgDecl;
import jena.cmdline.CommandLine;

import org.d2rq.CommandLineTool;
import org.d2rq.SystemLoader;
import org.d2rq.validation.Message;
import org.d2rq.validation.Message.Renderer;
import org.d2rq.validation.PlainTextMessageRenderer;
import org.d2rq.validation.Report;

public class validate extends CommandLineTool {

	public static void main(String[] args) {
		new validate().process(args);
	}
	
	private ArgDecl baseArg = new ArgDecl(true, "b", "base");

	@Override
	public void usage() {
		System.err.println("usage: validate [options] [jdbcURL] mappingFile");
		System.err.println();
		printStandardArguments(true, true);
		System.err.println();
		System.err.println("  Options:");
		printConnectionOptions(false);
		System.err.println("    -b baseURI      Base URI (default: " + SystemLoader.DEFAULT_BASE_URI + ")");
		System.err.println("    --verbose       Print debug information");
		System.err.println();
		System.exit(1);
	}

	@Override
	public void initArgs(CommandLine cmd) {
		cmd.add(baseArg);
		setMinMaxArguments(1, 2);
		setSupportImplicitJdbcURL(true);
	}

	@Override
	public void run(CommandLine cmd, SystemLoader loader) throws IOException {
		loader.setUseServerConfig(false);
		String mappingFile;
		if (cmd.numItems() == 1) {
			mappingFile = cmd.getItem(0);
		} else {
			loader.setJdbcURL(cmd.getItem(0));
			mappingFile = cmd.getItem(1);
		}
		loader.setMappingFile(mappingFile);
		if (cmd.hasArg(baseArg)) {
			loader.setSystemBaseURI(cmd.getArg(baseArg).getValue());
		}

		int exit = 0;
		try {
			Report report = loader.getReport();
			loader.validate();
			System.out.println();
			System.out.println("=== Validation report for " + mappingFile + " ===");
			System.out.println();
			Renderer renderer = new PlainTextMessageRenderer(System.out);
			for (Message message: report.getMessages()) {
				renderer.render(message);
			}
			if (report.countWarnings() == 0 && report.countErrors() == 0) {
				System.out.println(
						"SUCCESS: The document is a valid " + 
						loader.getMappingLanguage() + " mapping.");
				System.out.println();
			}
			if (report.hasError()) {
				exit = 2;
			} else if (report.countWarnings() > 0) {
				exit = 1;
			}
		} finally {
			loader.close();
		}
		System.exit(exit);
	}
}
