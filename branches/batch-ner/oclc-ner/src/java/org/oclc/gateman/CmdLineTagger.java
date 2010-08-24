package org.oclc.gateman;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.Map;
import java.util.Properties;

import java.util.logging.Logger;
import java.util.logging.LogManager;

import org.oclc.gateman.ner.Tagger;
import org.oclc.gateman.logging.LogFormatter;

/**
 * Command line tool for tagging named entities in text.
 */
public class CmdLineTagger {

	private static Logger logger;
	private static Tagger tagger;
	private static File taggerPropFile = new File("tagger.prop");

	private static String usage = "Usage: org.oclc.gateman.CmdLineTagger input-dir output-dir [ format ]";
	public static void main(String[] args) throws IOException, FileNotFoundException {
		LogManager lm = LogManager.getLogManager();
		lm.readConfiguration(ClassLoader.getSystemClassLoader().getSystemResourceAsStream("properties/cmdline.logging.properties"));
		logger = Logger.getLogger("CmdLineTagger");

		if ( args.length < 2 || args.length > 3) {
			usage(true); // error. must have at least two args, but no more than 3
		}

		File inputDir = new File(args[0]);
		File outputDir = new File(args[1]);
		String format = null != args[2] ? args[2] : "text/x-ner-markup";

		if ( ! (inputDir.isDirectory() && outputDir.isDirectory() ) ) {
			// error. both must be directories
			logger.severe("Supplied input and output are not directories: input: \"" + inputDir.toString() + "\"; output: \"" + outputDir.toString() + "\"");
			usage(true);
		}

		File configProps = new File(inputDir, taggerPropFile.toString());
		if ( ! configProps.isFile() ) {
			logger.severe("Tagging properties file not found. Expected at: \"" + configProps.toString() + "\"");
			System.exit(1);
		}

		Map<String,String> configMap = null;
		if ( configProps.canRead() ) {
			configMap = loadConfiguration(configProps);
		}
		else {
			logger.severe("Can't read tagging properties file. Located at: \"" + configProps.toString() + "\"");
			System.exit(1);
		}

		logger.info("Initializing tagger (this may take a few minutes) ...");
		tagger = Tagger.getInstance(configMap); // this will take a few _minutes_
		logger.info("Tagger initialized");

		File[] inputs = inputDir.listFiles();

		for (File f : inputs) {
			if ( f.getName().equals(taggerPropFile.getName()) ) { continue; } // skip the prop file
			if ( f.isHidden() ) {
				logger.info("Skipping " + f.getName() + " - hidden file");
				continue;
			}
			logger.info("Tagging " + f.getName() + " ...");
			try {
				tag(f, new File(outputDir, f.getName()), format);
			}
			catch (Exception e) {
				logger.info("Skipping " + f.getName() + " - exception: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	private static Map loadConfiguration(File file) throws FileNotFoundException, IOException {
		FileReader pr = new FileReader(file);
		Properties p = new Properties();
		p.load(pr);
		return p;
	}

	/**
	 * Tag a file
	 *
	 */
	private static void tag(File in, File out, String format) throws IOException, FileNotFoundException {
		tagger.setSource(new FileReader(in));
		tagger.prepareText();
		tagger.tagText();

		String taggedText = null;
		if (format.equals("text/x-ner-entities") ) {
			taggedText = tagger.formatResultsENT();
		}
		else if (format.equals("text/html") ) {
			taggedText = tagger.formatResultsHTML("");
		}
		else if (format.equals("application/xml") || format.equals("text/xml") ) {
			taggedText = tagger.formatResultsXML();
		}
		else {
			taggedText = tagger.formatResults();
		}

		FileWriter fw = new FileWriter(out);
		fw.write(taggedText);
		fw.close();
	}

	private static void usage(boolean exit) {
		System.err.println(usage);
		if ( exit ) { System.exit(1); }
	}

}
// vim:ts=4:noet:indentexpr=""
